-- Migration: 20260906_001_multi_vendor_orders.sql
-- Description: Multi-vendor suborders, atomic checkout, inventory reservation, and lifecycle management

-- 1. Extend orders table to support master multi-vendor orders and metadata
ALTER TABLE public.orders ALTER COLUMN seller_id DROP NOT NULL;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS meeting_point_id VARCHAR(100);
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS meeting_point_name VARCHAR(255);
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS scheduled_time VARCHAR(100);
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) DEFAULT 'EFECTIVO';

-- 2. Create sub_orders table
CREATE TABLE IF NOT EXISTS public.sub_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES public.profiles(id),
    subtotal_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    rejection_reason TEXT,
    payment_method VARCHAR(50) DEFAULT 'EFECTIVO',
    is_payment_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    is_delivery_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    stock_reserved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Extend order_items table to link to sub_orders
ALTER TABLE public.order_items ADD COLUMN IF NOT EXISTS sub_order_id UUID REFERENCES public.sub_orders(id) ON DELETE CASCADE;

-- 4. Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_orders_buyer_id ON public.orders(buyer_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON public.orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sub_orders_order_id ON public.sub_orders(order_id);
CREATE INDEX IF NOT EXISTS idx_sub_orders_seller_id ON public.sub_orders(seller_id);
CREATE INDEX IF NOT EXISTS idx_sub_orders_status ON public.sub_orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_sub_order_id ON public.order_items(sub_order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON public.order_items(order_id);

-- 5. Enable RLS on sub_orders
ALTER TABLE public.sub_orders ENABLE ROW LEVEL SECURITY;

-- 6. RLS Policies for sub_orders
DROP POLICY IF EXISTS "Comprador puede ver sus subpedidos" ON public.sub_orders;
CREATE POLICY "Comprador puede ver sus subpedidos" ON public.sub_orders
    FOR SELECT TO authenticated
    USING (EXISTS (
        SELECT 1 FROM public.orders o
        WHERE o.id = sub_orders.order_id AND o.buyer_id = auth.uid()
    ));

DROP POLICY IF EXISTS "Vendedor puede ver sus propios subpedidos" ON public.sub_orders;
CREATE POLICY "Vendedor puede ver sus propios subpedidos" ON public.sub_orders
    FOR SELECT TO authenticated
    USING (seller_id = auth.uid());

DROP POLICY IF EXISTS "Vendedor puede actualizar sus propios subpedidos" ON public.sub_orders;
CREATE POLICY "Vendedor puede actualizar sus propios subpedidos" ON public.sub_orders
    FOR UPDATE TO authenticated
    USING (seller_id = auth.uid())
    WITH CHECK (seller_id = auth.uid());

DROP POLICY IF EXISTS "Comprador puede insertar subpedidos de su orden" ON public.sub_orders;
CREATE POLICY "Comprador puede insertar subpedidos de su orden" ON public.sub_orders
    FOR INSERT TO authenticated
    WITH CHECK (EXISTS (
        SELECT 1 FROM public.orders o
        WHERE o.id = sub_orders.order_id AND o.buyer_id = auth.uid()
    ));

-- Update order_items RLS to allow access via sub_orders as well
DROP POLICY IF EXISTS "Involucrados pueden ver items del pedido" ON public.order_items;
CREATE POLICY "Involucrados pueden ver items del pedido" ON public.order_items
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_items.order_id AND (o.buyer_id = auth.uid() OR o.seller_id = auth.uid())
        )
        OR EXISTS (
            SELECT 1 FROM public.sub_orders s
            WHERE s.id = order_items.sub_order_id AND s.seller_id = auth.uid()
        )
    );

-- 7. Trigger to keep sub_orders.updated_at current
CREATE OR REPLACE FUNCTION public.set_sub_order_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sub_order_updated_at ON public.sub_orders;
CREATE TRIGGER trg_sub_order_updated_at
    BEFORE UPDATE ON public.sub_orders
    FOR EACH ROW EXECUTE FUNCTION public.set_sub_order_updated_at();

-- 8. Function: recalculate_master_order_status
CREATE OR REPLACE FUNCTION public.recalculate_master_order(p_order_id UUID)
RETURNS VOID AS $$
DECLARE
    v_total NUMERIC(10, 2) := 0;
    v_all_count INT := 0;
    v_rejected_count INT := 0;
    v_completed_count INT := 0;
    v_in_progress_count INT := 0;
    v_pending_count INT := 0;
    v_new_status VARCHAR(50);
BEGIN
    SELECT 
        COUNT(*),
        COUNT(*) FILTER (WHERE status IN ('rejected', 'cancelled')),
        COUNT(*) FILTER (WHERE status = 'completed'),
        COUNT(*) FILTER (WHERE status IN ('accepted', 'preparing', 'ready', 'waiting_delivery', 'payment_confirmed')),
        COUNT(*) FILTER (WHERE status = 'pending'),
        COALESCE(SUM(CASE WHEN status NOT IN ('rejected', 'cancelled') THEN subtotal_amount ELSE 0 END), 0)
    INTO
        v_all_count,
        v_rejected_count,
        v_completed_count,
        v_in_progress_count,
        v_pending_count,
        v_total
    FROM public.sub_orders
    WHERE order_id = p_order_id;

    IF v_all_count = 0 THEN
        RETURN;
    END IF;

    IF v_all_count = v_rejected_count THEN
        v_new_status := 'cancelled';
    ELSIF v_completed_count > 0 AND (v_completed_count + v_rejected_count = v_all_count) THEN
        v_new_status := 'completed';
    ELSIF v_in_progress_count > 0 OR v_completed_count > 0 THEN
        v_new_status := 'preparing';
    ELSE
        v_new_status := 'pending';
    END IF;

    UPDATE public.orders
    SET total_price = v_total,
        status = v_new_status::order_status,
        updated_at = now()
    WHERE id = p_order_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 9. Trigger on sub_orders to automatically recalculate master order
CREATE OR REPLACE FUNCTION public.trg_sub_order_change_fn()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM public.recalculate_master_order(OLD.order_id);
        RETURN OLD;
    ELSE
        PERFORM public.recalculate_master_order(NEW.order_id);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sub_order_change ON public.sub_orders;
CREATE TRIGGER trg_sub_order_change
    AFTER INSERT OR UPDATE OR DELETE ON public.sub_orders
    FOR EACH ROW EXECUTE FUNCTION public.trg_sub_order_change_fn();

-- 10. Atomic Checkout Function: checkout_order_atomic
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
            FALSE,
            FALSE,
            TRUE
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
        'status', 'pending'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 11. Function: update_suborder_status_atomic
CREATE OR REPLACE FUNCTION public.update_suborder_status_atomic(
    p_sub_order_id UUID,
    p_new_status VARCHAR,
    p_rejection_reason TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_caller_id UUID;
    v_sub RECORD;
    v_item RECORD;
    v_valid_transition BOOLEAN := FALSE;
    v_stock_released BOOLEAN := FALSE;
BEGIN
    v_caller_id := auth.uid();
    IF v_caller_id IS NULL THEN
        RAISE EXCEPTION 'UNAUTHENTICATED: Usuario no autenticado.';
    END IF;

    SELECT s.*, o.buyer_id
    INTO v_sub
    FROM public.sub_orders s
    JOIN public.orders o ON o.id = s.order_id
    WHERE s.id = p_sub_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'NOT_FOUND: Subpedido no encontrado.';
    END IF;

    IF p_new_status IN ('accepted', 'preparing', 'ready', 'completed') THEN
        IF v_sub.seller_id != v_caller_id THEN
            RAISE EXCEPTION 'FORBIDDEN: Solo el vendedor asignado puede gestionar este subpedido.';
        END IF;
    END IF;

    IF p_new_status = 'rejected' THEN
        IF v_sub.seller_id != v_caller_id THEN
            RAISE EXCEPTION 'FORBIDDEN: Solo el vendedor asignado puede rechazar este subpedido.';
        END IF;
    END IF;

    IF p_new_status = 'cancelled' THEN
        IF v_sub.seller_id != v_caller_id AND v_sub.buyer_id != v_caller_id THEN
            RAISE EXCEPTION 'FORBIDDEN: No tienes permiso para cancelar este subpedido.';
        END IF;
        IF v_sub.buyer_id = v_caller_id AND v_sub.status NOT IN ('pending') THEN
            RAISE EXCEPTION 'INVALID_TRANSITION: El comprador solo puede cancelar un pedido en estado pendiente.';
        END IF;
    END IF;

    CASE v_sub.status
        WHEN 'pending' THEN
            v_valid_transition := p_new_status IN ('accepted', 'rejected', 'cancelled');
        WHEN 'accepted' THEN
            v_valid_transition := p_new_status IN ('preparing', 'ready', 'cancelled');
        WHEN 'preparing' THEN
            v_valid_transition := p_new_status IN ('ready', 'cancelled');
        WHEN 'ready' THEN
            v_valid_transition := p_new_status IN ('completed', 'cancelled');
        WHEN 'completed' THEN
            v_valid_transition := FALSE;
        WHEN 'rejected' THEN
            v_valid_transition := FALSE;
        WHEN 'cancelled' THEN
            v_valid_transition := FALSE;
        ELSE
            v_valid_transition := FALSE;
    END CASE;

    IF NOT v_valid_transition THEN
        RAISE EXCEPTION 'INVALID_TRANSITION: No se permite cambiar el estado de "%" a "%".', v_sub.status, p_new_status;
    END IF;

    IF p_new_status IN ('rejected', 'cancelled') AND v_sub.stock_reserved = TRUE THEN
        FOR v_item IN 
            SELECT product_id, quantity 
            FROM public.order_items 
            WHERE sub_order_id = p_sub_order_id
        LOOP
            UPDATE public.products
            SET stock = stock + v_item.quantity,
                is_active = TRUE,
                updated_at = now()
            WHERE id = v_item.product_id;

            INSERT INTO public.stock_logs (product_id, quantity_changed, type, notes)
            VALUES (
                v_item.product_id,
                v_item.quantity,
                'restock',
                'Liberación de reserva por subpedido ' || p_new_status || ' #' || p_sub_order_id
            );
        END LOOP;
        v_stock_released := TRUE;
    END IF;

    UPDATE public.sub_orders
    SET status = p_new_status,
        rejection_reason = COALESCE(p_rejection_reason, rejection_reason),
        is_payment_confirmed = (p_new_status = 'completed' OR is_payment_confirmed),
        is_delivery_confirmed = (p_new_status = 'completed' OR is_delivery_confirmed),
        stock_reserved = (CASE WHEN v_stock_released THEN FALSE ELSE stock_reserved END),
        updated_at = now()
    WHERE id = p_sub_order_id;

    RETURN jsonb_build_object(
        'success', true,
        'sub_order_id', p_sub_order_id,
        'status', p_new_status,
        'stock_released', v_stock_released,
        'rejection_reason', p_rejection_reason
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 12. Permissions for authenticated role
GRANT EXECUTE ON FUNCTION public.checkout_order_atomic(UUID, VARCHAR, VARCHAR, VARCHAR, VARCHAR, TEXT, JSONB) TO authenticated;
GRANT EXECUTE ON FUNCTION public.update_suborder_status_atomic(UUID, VARCHAR, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.recalculate_master_order(UUID) TO authenticated;
