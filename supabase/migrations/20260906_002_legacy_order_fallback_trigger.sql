
-- 1. Refine fn_legacy_order_to_suborder
CREATE OR REPLACE FUNCTION fn_legacy_order_to_suborder()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    IF current_setting('vallego.atomic_checkout', true) = 'true' THEN
        RETURN NEW;
    END IF;

    IF NEW.seller_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM sub_orders WHERE order_id = NEW.id) THEN
            INSERT INTO sub_orders (
                id,
                order_id,
                seller_id,
                subtotal_amount,
                status,
                payment_method,
                created_at,
                updated_at
            ) VALUES (
                gen_random_uuid(),
                NEW.id,
                NEW.seller_id,
                COALESCE(NEW.total_price, 0),
                COALESCE(NEW.status, 'pending'),
                COALESCE(NEW.payment_method, 'efectivo'),
                COALESCE(NEW.created_at, now()),
                COALESCE(NEW.updated_at, now())
            );
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_legacy_order_to_suborder ON orders;
CREATE CONSTRAINT TRIGGER trg_legacy_order_to_suborder
    AFTER INSERT ON orders
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION fn_legacy_order_to_suborder();

-- 2. Refine fn_link_order_item_to_suborder
CREATE OR REPLACE FUNCTION fn_link_order_item_to_suborder()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_sub_id UUID;
    v_seller_id UUID;
BEGIN
    IF current_setting('vallego.atomic_checkout', true) = 'true' THEN
        RETURN NEW;
    END IF;

    IF NEW.sub_order_id IS NULL THEN
        SELECT seller_id INTO v_seller_id FROM products WHERE id = NEW.product_id;
        IF v_seller_id IS NULL THEN
            SELECT seller_id INTO v_seller_id FROM orders WHERE id = NEW.order_id;
        END IF;

        IF v_seller_id IS NOT NULL THEN
            SELECT id INTO v_sub_id FROM sub_orders WHERE order_id = NEW.order_id AND seller_id = v_seller_id LIMIT 1;
            IF v_sub_id IS NULL THEN
                v_sub_id := gen_random_uuid();
                INSERT INTO sub_orders (
                    id, order_id, seller_id, subtotal_amount, status, payment_method, created_at, updated_at
                ) 
                SELECT 
                    v_sub_id, NEW.order_id, v_seller_id, (NEW.price_at_sale * NEW.quantity), 
                    COALESCE(status, 'pending'), COALESCE(payment_method, 'efectivo'), now(), now()
                FROM orders WHERE id = NEW.order_id;
            END IF;

            NEW.sub_order_id := v_sub_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

-- 3. Update checkout_order_atomic to set local transaction flag
CREATE OR REPLACE FUNCTION public.checkout_order_atomic(
    p_order_id UUID,
    p_meeting_point_id VARCHAR,
    p_meeting_point_name VARCHAR,
    p_scheduled_time VARCHAR,
    p_payment_method VARCHAR,
    p_notes TEXT,
    p_suborders JSONB
)
RETURNS JSONB AS $$
DECLARE
    v_buyer_id UUID;
    v_order_code TEXT;
    v_grand_total NUMERIC(10, 2) := 0;
    v_sub JSONB;
    v_item JSONB;
    v_sub_id UUID;
    v_seller_id UUID;
    v_subtotal NUMERIC(10, 2);
    v_prod_stock INT;
    v_prod_price NUMERIC(10, 2);
    v_prod_active BOOLEAN;
    v_prod_name VARCHAR;
    v_prod_seller UUID;
    v_seller_accepting BOOLEAN;
    v_first_seller_id UUID := NULL;
    v_existing_order_id UUID;
BEGIN
    -- Set flag so legacy triggers know this is an atomic checkout
    PERFORM set_config('vallego.atomic_checkout', 'true', true);

    v_buyer_id := auth.uid();
    IF v_buyer_id IS NULL THEN
        RAISE EXCEPTION 'UNAUTHENTICATED: Usuario no autenticado.';
    END IF;

    SELECT id INTO v_existing_order_id FROM public.orders WHERE id = p_order_id;
    IF v_existing_order_id IS NOT NULL THEN
        RETURN jsonb_build_object(
            'success', true,
            'order_id', p_order_id,
            'is_duplicate', true,
            'message', 'Pedido ya registrado previamente.'
        );
    END IF;

    IF p_meeting_point_name IS NULL OR length(trim(p_meeting_point_name)) = 0 THEN
        RAISE EXCEPTION 'INVALID_DATA: Debe seleccionar un punto de encuentro válido.';
    END IF;
    IF p_scheduled_time IS NULL OR length(trim(p_scheduled_time)) = 0 THEN
        RAISE EXCEPTION 'INVALID_DATA: Debe seleccionar un horario de entrega válido.';
    END IF;

    IF p_suborders IS NULL OR jsonb_array_length(p_suborders) = 0 THEN
        RAISE EXCEPTION 'INVALID_DATA: El pedido debe contener al menos un producto.';
    END IF;

    FOR v_sub IN SELECT * FROM jsonb_array_elements(p_suborders)
    LOOP
        v_seller_id := (v_sub->>'seller_id')::UUID;
        IF v_first_seller_id IS NULL THEN
            v_first_seller_id := v_seller_id;
        END IF;

        IF v_seller_id = v_buyer_id THEN
            RAISE EXCEPTION 'SELF_PURCHASE: No puedes realizar un pedido a tu propio emprendimiento.';
        END IF;

        SELECT accepting_orders INTO v_seller_accepting FROM public.profiles WHERE id = v_seller_id;
        IF v_seller_accepting = false THEN
            RAISE EXCEPTION 'SELLER_CLOSED: Un emprendedor del carrito actualmente no está aceptando pedidos.';
        END IF;

        v_subtotal := 0;
        FOR v_item IN SELECT * FROM jsonb_array_elements(v_sub->'items')
        LOOP
            SELECT stock, price, is_active, name, seller_id
            INTO v_prod_stock, v_prod_price, v_prod_active, v_prod_name, v_prod_seller
            FROM public.products
            WHERE id = (v_item->>'product_id')::UUID
            FOR UPDATE;

            IF NOT FOUND THEN
                RAISE EXCEPTION 'PRODUCT_NOT_FOUND: El producto seleccionado no existe.';
            END IF;

            IF v_prod_seller != v_seller_id THEN
                RAISE EXCEPTION 'INVALID_SELLER: El producto % no pertenece al emprendedor indicado.', v_prod_name;
            END IF;

            IF NOT v_prod_active OR v_prod_stock <= 0 THEN
                RAISE EXCEPTION 'PRODUCT_UNAVAILABLE: El producto "%" ya no se encuentra disponible.', v_prod_name;
            END IF;

            IF v_prod_stock < (v_item->>'quantity')::INT THEN
                RAISE EXCEPTION 'INSUFFICIENT_STOCK: Stock insuficiente para "%". Disponibles: %, solicitados: %',
                    v_prod_name, v_prod_stock, (v_item->>'quantity')::INT;
            END IF;

            UPDATE public.products
            SET stock = stock - (v_item->>'quantity')::INT,
                is_active = (stock - (v_item->>'quantity')::INT > 0),
                updated_at = now()
            WHERE id = (v_item->>'product_id')::UUID;

            INSERT INTO public.stock_logs (product_id, quantity_changed, type, notes)
            VALUES (
                (v_item->>'product_id')::UUID,
                -(v_item->>'quantity')::INT,
                'sale',
                'Reserva de stock por pedido #' || p_order_id
            );

            v_subtotal := v_subtotal + (v_prod_price * (v_item->>'quantity')::INT);
        END LOOP;

        v_grand_total := v_grand_total + v_subtotal;
    END LOOP;

    INSERT INTO public.orders (
        id,
        buyer_id,
        seller_id,
        total_price,
        delivery_place,
        meeting_point_id,
        meeting_point_name,
        scheduled_time,
        notes,
        payment_method,
        status
    ) VALUES (
        p_order_id,
        v_buyer_id,
        v_first_seller_id,
        v_grand_total,
        p_meeting_point_name || ' (' || p_scheduled_time || ')',
        p_meeting_point_id,
        p_meeting_point_name,
        p_scheduled_time,
        p_notes,
        p_payment_method,
        'pending'
    ) RETURNING order_code INTO v_order_code;

    FOR v_sub IN SELECT * FROM jsonb_array_elements(p_suborders)
    LOOP
        v_sub_id := COALESCE((v_sub->>'id')::UUID, gen_random_uuid());
        v_seller_id := (v_sub->>'seller_id')::UUID;

        v_subtotal := 0;
        FOR v_item IN SELECT * FROM jsonb_array_elements(v_sub->'items')
        LOOP
            SELECT price INTO v_prod_price FROM public.products WHERE id = (v_item->>'product_id')::UUID;
            v_subtotal := v_subtotal + (v_prod_price * (v_item->>'quantity')::INT);
        END LOOP;

        INSERT INTO public.sub_orders (
            id,
            order_id,
            seller_id,
            subtotal_amount,
            status,
            payment_method,
            is_payment_confirmed,
            is_delivery_confirmed,
            stock_reserved
        ) VALUES (
            v_sub_id,
            p_order_id,
            v_seller_id,
            v_subtotal,
            'pending',
            p_payment_method,
            false,
            false,
            true
        );

        FOR v_item IN SELECT * FROM jsonb_array_elements(v_sub->'items')
        LOOP
            SELECT price INTO v_prod_price FROM public.products WHERE id = (v_item->>'product_id')::UUID;
            INSERT INTO public.order_items (
                id,
                order_id,
                sub_order_id,
                product_id,
                quantity,
                price_at_sale
            ) VALUES (
                COALESCE((v_item->>'id')::UUID, gen_random_uuid()),
                p_order_id,
                v_sub_id,
                (v_item->>'product_id')::UUID,
                (v_item->>'quantity')::INT,
                v_prod_price
            );
        END LOOP;
    END LOOP;

    RETURN jsonb_build_object(
        'success', true,
        'order_id', p_order_id,
        'order_code', v_order_code,
        'total_amount', v_grand_total,
        'status', 'pending',
        'is_duplicate', false
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
