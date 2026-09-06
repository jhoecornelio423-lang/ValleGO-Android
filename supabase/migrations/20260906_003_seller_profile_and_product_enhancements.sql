-- ============================================================================
-- MIGRACIÓN 003: PERFIL DE NEGOCIO, MEJORAS DE PRODUCTO, EXPIRACIÓN A 15 MIN Y CANCELACIÓN
-- Proyecto: Valle-Go
-- ============================================================================

-- 1. Columnas de personalización comercial en profiles
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS business_name VARCHAR(150);
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS business_status VARCHAR(20) DEFAULT 'ABIERTO';

-- 2. Columna image_url directa en products
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS image_url TEXT;

-- 3. Sincronizar imágenes existentes de product_images a products.image_url (si image_url es nula)
UPDATE public.products p
SET image_url = pi.image_url
FROM public.product_images pi
WHERE pi.product_id = p.id 
  AND (pi.is_featured = true OR pi.image_url IS NOT NULL) 
  AND p.image_url IS NULL;

-- 4. Procedimiento atómico de cancelación por el comprador
CREATE OR REPLACE FUNCTION public.cancel_order_by_buyer_atomic(p_order_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_buyer_id UUID := auth.uid();
    v_order RECORD;
    v_sub RECORD;
    v_item RECORD;
    v_cancelled_count INT := 0;
BEGIN
    SELECT * INTO v_order FROM public.orders WHERE id = p_order_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'message', 'Orden no encontrada.');
    END IF;

    -- Si hay usuario autenticado, validar que sea el comprador
    IF v_buyer_id IS NOT NULL AND v_order.buyer_id != v_buyer_id THEN
        RETURN jsonb_build_object('success', false, 'message', 'No tienes autorización para cancelar este pedido.');
    END IF;

    -- Cancelar únicamente subpedidos pendientes
    FOR v_sub IN SELECT * FROM public.sub_orders WHERE order_id = p_order_id AND status = 'pending' LOOP
        IF v_sub.stock_reserved THEN
            FOR v_item IN SELECT * FROM public.order_items WHERE sub_order_id = v_sub.id LOOP
                UPDATE public.products 
                SET stock = stock + v_item.quantity, 
                    is_active = true 
                WHERE id = v_item.product_id;

                INSERT INTO public.stock_logs(product_id, quantity_changed, type, notes)
                VALUES (v_item.product_id, v_item.quantity, 'restock', 'Cancelado por el comprador');
            END LOOP;
        END IF;

        UPDATE public.sub_orders 
        SET status = 'cancelled', 
            stock_reserved = false, 
            updated_at = now() 
        WHERE id = v_sub.id;

        v_cancelled_count := v_cancelled_count + 1;
    END LOOP;

    -- Recalcular orden maestra
    PERFORM public.recalculate_order_master(p_order_id);

    RETURN jsonb_build_object(
        'success', true, 
        'cancelled_suborders', v_cancelled_count, 
        'message', 'Pedido cancelado correctamente.'
    );
END;
$$;

-- 5. Procedimiento atómico de inasistencia (Comprador o Vendedor no se presentó)
CREATE OR REPLACE FUNCTION public.mark_suborder_no_show_atomic(
    p_sub_order_id UUID,
    p_reported_by_seller BOOLEAN DEFAULT TRUE,
    p_reason TEXT DEFAULT 'El comprador no se presentó'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_sub RECORD;
    v_item RECORD;
BEGIN
    SELECT * INTO v_sub FROM public.sub_orders WHERE id = p_sub_order_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'message', 'Subpedido no encontrado.');
    END IF;

    -- Si aún retenía stock reservado y no fue entregado, liberarlo
    IF v_sub.stock_reserved THEN
        FOR v_item IN SELECT * FROM public.order_items WHERE sub_order_id = v_sub.id LOOP
            UPDATE public.products 
            SET stock = stock + v_item.quantity, 
                is_active = true 
            WHERE id = v_item.product_id;

            INSERT INTO public.stock_logs(product_id, quantity_changed, type, notes)
            VALUES (v_item.product_id, v_item.quantity, 'restock', 'No entregado: ' || p_reason);
        END LOOP;
    END IF;

    UPDATE public.sub_orders
    SET status = 'not_delivered',
        rejection_reason = p_reason,
        stock_reserved = false,
        updated_at = now()
    WHERE id = p_sub_order_id;

    PERFORM public.recalculate_order_master(v_sub.order_id);

    RETURN jsonb_build_object('success', true, 'message', 'Subpedido marcado como no entregado.');
END;
$$;

-- 6. Procedimiento atómico de expiración automática tras 15 minutos sin responder
CREATE OR REPLACE FUNCTION public.expire_unanswered_suborders_atomic()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_sub RECORD;
    v_item RECORD;
    v_count INT := 0;
BEGIN
    FOR v_sub IN 
        SELECT id, order_id, stock_reserved 
        FROM public.sub_orders 
        WHERE status = 'pending' 
          AND created_at < (now() - INTERVAL '15 minutes')
    LOOP
        -- Restituir stock reservado
        IF v_sub.stock_reserved THEN
            FOR v_item IN SELECT product_id, quantity FROM public.order_items WHERE sub_order_id = v_sub.id LOOP
                UPDATE public.products 
                SET stock = stock + v_item.quantity, 
                    is_active = true 
                WHERE id = v_item.product_id;

                INSERT INTO public.stock_logs(product_id, quantity_changed, type, notes)
                VALUES (v_item.product_id, v_item.quantity, 'restock', 'Cancelado por tiempo agotado (15 min)');
            END LOOP;
        END IF;

        UPDATE public.sub_orders 
        SET status = 'rejected', 
            rejection_reason = 'Tiempo de respuesta agotado (15 min)', 
            stock_reserved = false, 
            updated_at = now() 
        WHERE id = v_sub.id;

        PERFORM public.recalculate_order_master(v_sub.order_id);
        v_count := v_count + 1;
    END LOOP;

    RETURN jsonb_build_object('success', true, 'expired_count', v_count);
END;
$$;

-- 7. Otorgar permisos de ejecución
GRANT EXECUTE ON FUNCTION public.cancel_order_by_buyer_atomic(UUID) TO authenticated, anon, service_role;
GRANT EXECUTE ON FUNCTION public.mark_suborder_no_show_atomic(UUID, BOOLEAN, TEXT) TO authenticated, anon, service_role;
GRANT EXECUTE ON FUNCTION public.expire_unanswered_suborders_atomic() TO authenticated, anon, service_role;
