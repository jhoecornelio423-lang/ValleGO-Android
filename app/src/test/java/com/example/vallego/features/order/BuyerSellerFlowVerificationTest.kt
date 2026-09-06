package com.example.vallego.features.order

import com.example.vallego.data.repository.CartRepositoryImpl
import com.example.vallego.data.repository.OrderRepositoryImpl
import com.example.vallego.domain.model.*
import com.example.vallego.domain.usecase.CalculateCartUseCase
import com.example.vallego.domain.usecase.CreateOrderWithSubordersUseCase
import com.example.vallego.domain.usecase.RecalculateOrderUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuyerSellerFlowVerificationTest {

    private val calculateCartUseCase = CalculateCartUseCase()
    private val createOrderUseCase = CreateOrderWithSubordersUseCase()
    private val recalculateOrderUseCase = RecalculateOrderUseCase()
    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var cartRepository: CartRepositoryImpl

    private val prodBurger = Product(
        id = "p-burger",
        sellerId = "seller-a",
        name = "Hamburguesa Doble",
        price = 15.0,
        stock = 10
    )

    private val prodDrink = Product(
        id = "p-drink",
        sellerId = "seller-b",
        name = "Chicha Morada",
        price = 5.0,
        stock = 10
    )

    @Before
    fun setup() {
        orderRepository = OrderRepositoryImpl(
            postgrest = null,
            recalculateOrderUseCase = recalculateOrderUseCase
        )
        cartRepository = CartRepositoryImpl(calculateCartUseCase)
        cartRepository.setStoreName("seller-a", "Puesto Burger")
        cartRepository.setStoreName("seller-b", "Puesto Bebidas")
    }

    @Test
    fun testPurchaseWithTwoSellers_createsTwoIndependentSubordersWithParentOrder() = runTest {
        cartRepository.addToCart(prodBurger, 2) // 30.0
        cartRepository.addToCart(prodDrink, 1)  // 5.0

        val cartResult = cartRepository.cartCalculation.value
        assertEquals(2, cartResult.storeGroups.size)
        assertEquals(35.0, cartResult.grandTotal, 0.001)

        val buyer = UserProfile(id = "buyer-1", fullName = "Ana Alumna", role = UserRole.COMPRADOR)
        val meeting = CampusMeetingPoint(id = "mp-1", name = "Pabellón A - Piso 1", pavilion = "A")

        val order = createOrderUseCase(
            buyerProfile = buyer,
            meetingPoint = meeting,
            scheduledTime = "12:00 - 12:30",
            paymentMethod = PaymentMethod.YAPE,
            cartResult = cartResult,
            notes = "Sin mayonesa"
        )

        assertEquals("buyer-1", order.buyerId)
        assertEquals("Pabellón A - Piso 1", order.meetingPointName)
        assertEquals("12:00 - 12:30", order.scheduledTime)
        assertEquals(35.0, order.totalAmount, 0.001)
        assertEquals(OrderStatus.PENDIENTE, order.status)
        assertEquals(2, order.subOrders.size)

        val placeResult = orderRepository.placeOrder(order)
        assertTrue(placeResult.isSuccess)

        // Comprador ve orden agrupada con 2 puestos
        val buyerOrders = orderRepository.observeOrdersForBuyer("buyer-1").first()
        assertEquals(1, buyerOrders.size)
        val placed = buyerOrders.first()
        assertEquals(2, placed.subOrders.size)

        // Cada vendedor ve únicamente su parte
        val sellerAOrders = orderRepository.observeSubOrdersForSeller("seller-a").first()
        assertEquals(1, sellerAOrders.size)
        assertEquals(30.0, sellerAOrders.first().subtotalAmount, 0.001)

        val sellerBOrders = orderRepository.observeSubOrdersForSeller("seller-b").first()
        assertEquals(1, sellerBOrders.size)
        assertEquals(5.0, sellerBOrders.first().subtotalAmount, 0.001)
    }

    @Test
    fun testOrderSaveFailure_preservesCartWithoutEmptying() = runTest {
        cartRepository.addToCart(prodBurger, 1)
        assertEquals(1, cartRepository.items.value.size)

        // Simular intento de confirmar orden con un error (ej. orden sin subpedidos o fallo de red)
        val emptyOrder = Order(
            id = "err-order",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "12:00",
            totalAmount = 0.0,
            status = OrderStatus.PENDIENTE,
            subOrders = emptyList()
        )

        val result = orderRepository.placeOrder(emptyOrder)
        // No debe guardar una orden vacía
        assertTrue(result.isFailure)

        // El carrito debe permanecer intacto
        assertEquals(1, cartRepository.items.value.size)
        assertEquals(15.0, cartRepository.cartCalculation.value.grandTotal, 0.001)
    }

    @Test
    fun testRetryWithSameOrderId_doesNotDuplicatePurchase() = runTest {
        val subA = SubOrder(
            id = "sub-1",
            orderId = "order-dup",
            sellerId = "seller-a",
            subtotalAmount = 15.0,
            status = SubOrderStatus.PENDIENTE
        )
        val order = Order(
            id = "order-dup",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "12:00",
            totalAmount = 15.0,
            status = OrderStatus.PENDIENTE,
            subOrders = listOf(subA)
        )

        // Primer envío
        val res1 = orderRepository.placeOrder(order)
        assertTrue(res1.isSuccess)

        // Reintento con la misma ID
        val res2 = orderRepository.placeOrder(order)
        assertTrue(res2.isSuccess)

        // En la lista de pedidos del comprador NO debe estar duplicado
        val buyerOrders = orderRepository.observeOrdersForBuyer("buyer-1").first()
        val countWithId = buyerOrders.count { it.id == "order-dup" }
        assertEquals(1, countWithId)
    }

    @Test
    fun testOneSuborderCompletedAndOneRejected_finalizesOrderCorrectlyWithRecalculatedTotal() = runTest {
        val subA = SubOrder(
            id = "sub-a",
            orderId = "order-mixed",
            sellerId = "seller-a",
            sellerName = "Puesto Burger",
            subtotalAmount = 30.0,
            status = SubOrderStatus.PENDIENTE
        )
        val subB = SubOrder(
            id = "sub-b",
            orderId = "order-mixed",
            sellerId = "seller-b",
            sellerName = "Puesto Bebidas",
            subtotalAmount = 5.0,
            status = SubOrderStatus.PENDIENTE
        )
        val order = Order(
            id = "order-mixed",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "12:00",
            totalAmount = 35.0,
            status = OrderStatus.PENDIENTE,
            subOrders = listOf(subA, subB)
        )
        orderRepository.placeOrder(order)

        // Vendedor A acepta y luego completa
        orderRepository.updateSubOrderStatus("sub-a", SubOrderStatus.ACEPTADO)
        orderRepository.updateSubOrderStatus("sub-a", SubOrderStatus.COMPLETADO)

        // Vendedor B rechaza por falta de insumos
        orderRepository.updateSubOrderStatus("sub-b", SubOrderStatus.RECHAZADO, "Bebidas agotadas")

        val buyerOrders = orderRepository.observeOrdersForBuyer("buyer-1").first()
        val finalOrder = buyerOrders.first { it.id == "order-mixed" }

        // El total recalculado debe ser solo el de Burger (S/ 30.0), excluyendo Bebidas (S/ 5.0)
        assertEquals(30.0, finalOrder.totalAmount, 0.001)

        // La orden debe estar completada porque todos los subpedidos activos terminaron satisfactoriamente
        assertEquals(OrderStatus.COMPLETADA, finalOrder.status)

        // El subpedido rechazado conserva el motivo
        val rejectedSub = finalOrder.subOrders.first { it.id == "sub-b" }
        assertEquals(SubOrderStatus.RECHAZADO, rejectedSub.status)
        assertEquals("Bebidas agotadas", rejectedSub.rejectionReason)
    }

    @Test
    fun testAllSubordersRejected_marksOrderCancelled() = runTest {
        val subA = SubOrder(id = "sub-a2", orderId = "order-cancel", sellerId = "seller-a", subtotalAmount = 15.0, status = SubOrderStatus.PENDIENTE)
        val subB = SubOrder(id = "sub-b2", orderId = "order-cancel", sellerId = "seller-b", subtotalAmount = 5.0, status = SubOrderStatus.PENDIENTE)
        val order = Order(
            id = "order-cancel",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "12:00",
            totalAmount = 20.0,
            status = OrderStatus.PENDIENTE,
            subOrders = listOf(subA, subB)
        )
        orderRepository.placeOrder(order)

        orderRepository.updateSubOrderStatus("sub-a2", SubOrderStatus.RECHAZADO, "Cerrado")
        orderRepository.updateSubOrderStatus("sub-b2", SubOrderStatus.RECHAZADO, "Sin stock")

        val buyerOrders = orderRepository.observeOrdersForBuyer("buyer-1").first()
        val finalOrder = buyerOrders.first { it.id == "order-cancel" }

        assertEquals(0.0, finalOrder.totalAmount, 0.001)
        assertEquals(OrderStatus.CANCELADA, finalOrder.status)
    }

    @Test
    fun testSignOut_clearsCartAndOrderMemorySoAnotherAccountDoesNotSeePreviousData() = runTest {
        cartRepository.addToCart(prodBurger, 2)
        assertEquals(1, cartRepository.items.value.size)

        val sub = SubOrder(id = "sub-x", orderId = "order-x", sellerId = "seller-a", subtotalAmount = 30.0, status = SubOrderStatus.PENDIENTE)
        val order = Order(id = "order-x", buyerId = "buyer-1", meetingPointId = "mp-1", scheduledTime = "12:00", totalAmount = 30.0, status = OrderStatus.PENDIENTE, subOrders = listOf(sub))
        orderRepository.placeOrder(order)

        assertEquals(1, orderRepository.getOrdersForBuyer("buyer-1").getOrNull()?.size)

        // Simular cierre de sesión
        cartRepository.clearCart()
        orderRepository.clearCache()

        // Verificar que ambas memorias quedaron vacías para la siguiente sesión
        assertTrue(cartRepository.items.value.isEmpty())
        assertTrue(orderRepository.getOrdersForBuyer("buyer-1").getOrNull()?.isEmpty() == true)
    }

    @Test
    fun testCancelOrderByBuyer_cancelsOrderAndPendingSuborders() = runTest {
        val sub1 = SubOrder(id = "sub-c1", orderId = "order-cancel-buyer", sellerId = "seller-a", subtotalAmount = 15.0, status = SubOrderStatus.PENDIENTE)
        val order = Order(
            id = "order-cancel-buyer",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "13:00",
            totalAmount = 15.0,
            status = OrderStatus.PENDIENTE,
            subOrders = listOf(sub1)
        )
        orderRepository.placeOrder(order)

        val cancelResult = orderRepository.cancelOrderByBuyer("order-cancel-buyer")
        assertTrue(cancelResult.isSuccess)

        val orders = orderRepository.observeOrdersForBuyer("buyer-1").first()
        val cancelledOrder = orders.first { it.id == "order-cancel-buyer" }
        assertEquals(OrderStatus.CANCELADA, cancelledOrder.status)
        assertEquals(SubOrderStatus.CANCELADO, cancelledOrder.subOrders.first().status)
    }

    @Test
    fun testMarkBuyerNoShow_marksSuborderAsNotDeliveredAndRecalculates() = runTest {
        val sub1 = SubOrder(id = "sub-ns1", orderId = "order-ns", sellerId = "seller-a", subtotalAmount = 20.0, status = SubOrderStatus.LISTO)
        val order = Order(
            id = "order-ns",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "14:00",
            totalAmount = 20.0,
            status = OrderStatus.EN_PROCESO,
            subOrders = listOf(sub1)
        )
        orderRepository.placeOrder(order)

        val result = orderRepository.markBuyerNoShow("sub-ns1", "No se presentó al patio central")
        assertTrue(result.isSuccess)
        assertEquals(SubOrderStatus.NO_ENTREGADO, result.getOrNull()?.status)

        val sellerSubs = orderRepository.observeSubOrdersForSeller("seller-a").first()
        val sub = sellerSubs.first { it.id == "sub-ns1" }
        assertEquals(SubOrderStatus.NO_ENTREGADO, sub.status)
    }
}
