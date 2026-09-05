package com.example.vallego.features.seller

import com.example.vallego.data.repository.OrderRepositoryImpl
import com.example.vallego.domain.model.*
import com.example.vallego.domain.usecase.RecalculateOrderUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SellerSuborderFlowTest {

    private val recalculateOrderUseCase = RecalculateOrderUseCase()
    private lateinit var orderRepository: OrderRepositoryImpl

    private val subOrderPapu = SubOrder(
        id = "sub-papu-1",
        orderId = "order-100",
        sellerId = "seller-papu",
        sellerName = "Papu Burger",
        items = listOf(
            SubOrderItem(
                id = "item-1",
                subOrderId = "sub-papu-1",
                productId = "prod-1",
                productName = "Hamburguesa Artesanal",
                unitPrice = 12.0,
                quantity = 2,
                subtotal = 24.0
            )
        ),
        subtotalAmount = 24.0,
        status = SubOrderStatus.PENDIENTE,
        paymentMethod = PaymentMethod.YAPE
    )

    private val subOrderDulce = SubOrder(
        id = "sub-dulce-1",
        orderId = "order-100",
        sellerId = "seller-dulce",
        sellerName = "Dulce Valle",
        items = listOf(
            SubOrderItem(
                id = "item-2",
                subOrderId = "sub-dulce-1",
                productId = "prod-2",
                productName = "Brownie con Helado",
                unitPrice = 7.0,
                quantity = 1,
                subtotal = 7.0
            )
        ),
        subtotalAmount = 7.0,
        status = SubOrderStatus.PENDIENTE,
        paymentMethod = PaymentMethod.YAPE
    )

    private val testOrder = Order(
        id = "order-100",
        buyerId = "buyer-juan",
        buyerName = "Juan Perez",
        meetingPointId = "mp-biblio",
        meetingPointName = "Biblioteca Central",
        scheduledTime = "13:00 - 13:30",
        totalAmount = 31.0,
        status = OrderStatus.PENDIENTE,
        subOrders = listOf(subOrderPapu, subOrderDulce)
    )

    @Before
    fun setup() {
        orderRepository = OrderRepositoryImpl(
            postgrest = null,
            recalculateOrderUseCase = recalculateOrderUseCase
        )
    }

    @Test
    fun testSellerObservesOnlyTheirAssignedSuborders() = runTest {
        orderRepository.placeOrder(testOrder)

        val papuSubOrders = orderRepository.observeSubOrdersForSeller("seller-papu").first()
        assertEquals(1, papuSubOrders.size)
        assertEquals("sub-papu-1", papuSubOrders.first().id)
        assertEquals("Papu Burger", papuSubOrders.first().sellerName)

        val dulceSubOrders = orderRepository.observeSubOrdersForSeller("seller-dulce").first()
        assertEquals(1, dulceSubOrders.size)
        assertEquals("sub-dulce-1", dulceSubOrders.first().id)
        assertEquals("Dulce Valle", dulceSubOrders.first().sellerName)
    }

    @Test
    fun testSuborderLifecycleTransitions() = runTest {
        orderRepository.placeOrder(testOrder)

        // 1. Aceptar
        orderRepository.updateSubOrderStatus("sub-papu-1", SubOrderStatus.ACEPTADO)
        var papuSub = orderRepository.observeSubOrdersForSeller("seller-papu").first().first()
        assertEquals(SubOrderStatus.ACEPTADO, papuSub.status)

        // 2. En preparaciÃ³n
        orderRepository.updateSubOrderStatus("sub-papu-1", SubOrderStatus.EN_PREPARACION)
        papuSub = orderRepository.observeSubOrdersForSeller("seller-papu").first().first()
        assertEquals(SubOrderStatus.EN_PREPARACION, papuSub.status)

        // 3. Listo para entrega
        orderRepository.updateSubOrderStatus("sub-papu-1", SubOrderStatus.LISTO)
        papuSub = orderRepository.observeSubOrdersForSeller("seller-papu").first().first()
        assertEquals(SubOrderStatus.LISTO, papuSub.status)

        // 4. Completado con entrega y cobro
        orderRepository.updateSubOrderStatus("sub-papu-1", SubOrderStatus.COMPLETADO)
        papuSub = orderRepository.observeSubOrdersForSeller("seller-papu").first().first()
        assertEquals(SubOrderStatus.COMPLETADO, papuSub.status)
        assertTrue(papuSub.isPaymentConfirmed)
        assertTrue(papuSub.isDeliveryConfirmed)
    }

    @Test
    fun testSellerRejectionTriggersAutomaticRecalculationForBuyer() = runTest {
        orderRepository.placeOrder(testOrder)

        // Papu Burger acepta
        orderRepository.updateSubOrderStatus("sub-papu-1", SubOrderStatus.ACEPTADO)

        // Dulce Valle rechaza por falta de insumos
        orderRepository.updateSubOrderStatus(
            subOrderId = "sub-dulce-1",
            newStatus = SubOrderStatus.RECHAZADO,
            rejectionReason = "Brownies agotados"
        )

        // Verificar lo que ve el comprador en tiempo real
        val buyerOrders = orderRepository.observeOrdersForBuyer("buyer-juan").first()
        val updatedOrder = buyerOrders.first { it.id == "order-100" }

        // El total original era S/ 31.0. Al restar Dulce Valle (S/ 7.0), debe ser S/ 24.0
        assertEquals(24.0, updatedOrder.totalAmount, 0.001)
        assertEquals(OrderStatus.PARCIALMENTE_ACEPTADA, updatedOrder.status)

        val dulceSub = updatedOrder.subOrders.first { it.id == "sub-dulce-1" }
        assertEquals(SubOrderStatus.RECHAZADO, dulceSub.status)
        assertEquals("Brownies agotados", dulceSub.rejectionReason)
    }

    @Test
    fun testAllSubordersRejectedCancelsParentOrder() = runTest {
        orderRepository.placeOrder(testOrder)

        // Ambos puestos rechazan
        orderRepository.updateSubOrderStatus("sub-papu-1", SubOrderStatus.RECHAZADO, "Sin gas")
        orderRepository.updateSubOrderStatus("sub-dulce-1", SubOrderStatus.RECHAZADO, "Cerrado")

        val buyerOrders = orderRepository.observeOrdersForBuyer("buyer-juan").first()
        val updatedOrder = buyerOrders.first { it.id == "order-100" }

        assertEquals(0.0, updatedOrder.totalAmount, 0.001)
        assertEquals(OrderStatus.CANCELADA, updatedOrder.status)
    }
}