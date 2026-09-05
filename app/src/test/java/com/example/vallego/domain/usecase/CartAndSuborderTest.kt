package com.example.vallego.domain.usecase

import com.example.vallego.data.repository.CartRepositoryImpl
import com.example.vallego.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartAndSuborderTest {

    private val calculateCartUseCase = CalculateCartUseCase()
    private val createOrderUseCase = CreateOrderWithSubordersUseCase()
    private val recalculateOrderUseCase = RecalculateOrderUseCase()

    private val burger = Product(
        id = "p-burger",
        sellerId = "seller-papu",
        name = "Hamburguesa Clásica",
        price = 10.0,
        stock = 20
    )

    private val soda = Product(
        id = "p-soda",
        sellerId = "seller-papu",
        name = "Gaseosa 500ml",
        price = 3.0,
        stock = 50
    )

    private val brownie = Product(
        id = "p-brownie",
        sellerId = "seller-dulce",
        name = "Brownie con Helado",
        price = 5.0,
        stock = 15
    )

    private val coffee = Product(
        id = "p-coffee",
        sellerId = "seller-coffee",
        name = "Café Americano",
        price = 6.0,
        stock = 30
    )

    @Test
    fun testMultiVendorCartGroupingAndSubtotals() {
        val items = listOf(
            CartItem(product = burger, quantity = 2), // 20.0
            CartItem(product = soda, quantity = 1),   // 3.0 (Papu subtotal = 23.0)
            CartItem(product = brownie, quantity = 2),// 10.0 (Dulce subtotal = 10.0)
            CartItem(product = coffee, quantity = 1)  // 6.0  (Coffee subtotal = 6.0)
        )

        val storeNames = mapOf(
            "seller-papu" to "Papu Burger",
            "seller-dulce" to "Dulce Valle",
            "seller-coffee" to "Coffee Campus"
        )

        val result = calculateCartUseCase(items, storeNames)

        assertEquals(3, result.storeGroups.size)
        assertEquals(39.0, result.grandTotal, 0.001)
        assertEquals(6, result.totalItemCount)

        val papuGroup = result.storeGroups.first { it.sellerId == "seller-papu" }
        assertEquals("Papu Burger", papuGroup.sellerName)
        assertEquals(23.0, papuGroup.subtotal, 0.001)
        assertEquals(2, papuGroup.items.size)

        val dulceGroup = result.storeGroups.first { it.sellerId == "seller-dulce" }
        assertEquals("Dulce Valle", dulceGroup.sellerName)
        assertEquals(10.0, dulceGroup.subtotal, 0.001)

        val coffeeGroup = result.storeGroups.first { it.sellerId == "seller-coffee" }
        assertEquals("Coffee Campus", coffeeGroup.sellerName)
        assertEquals(6.0, coffeeGroup.subtotal, 0.001)
    }

    @Test
    fun testCartRepositoryMutations() {
        val cartRepo = CartRepositoryImpl(calculateCartUseCase)
        cartRepo.setStoreName("seller-papu", "Papu Burger")

        cartRepo.addToCart(burger, quantity = 2)
        assertEquals(1, cartRepo.items.value.size)
        assertEquals(20.0, cartRepo.cartCalculation.value.grandTotal, 0.001)

        // Add more of same product
        cartRepo.addToCart(burger, quantity = 1)
        assertEquals(3, cartRepo.items.value.first().quantity)
        assertEquals(30.0, cartRepo.cartCalculation.value.grandTotal, 0.001)

        // Update quantity
        cartRepo.updateQuantity("p-burger", quantity = 1)
        assertEquals(1, cartRepo.items.value.first().quantity)
        assertEquals(10.0, cartRepo.cartCalculation.value.grandTotal, 0.001)

        // Remove
        cartRepo.removeFromCart("p-burger")
        assertTrue(cartRepo.items.value.isEmpty())
        assertEquals(0.0, cartRepo.cartCalculation.value.grandTotal, 0.001)
    }

    @Test
    fun testCreateOrderWithSubordersAtomicHierarchy() {
        val items = listOf(
            CartItem(product = burger, quantity = 2),
            CartItem(product = brownie, quantity = 2)
        )
        val cartResult = calculateCartUseCase(items, mapOf("seller-papu" to "Papu Burger", "seller-dulce" to "Dulce Valle"))

        val buyer = UserProfile(
            id = "buyer-123",
            fullName = "Juan Pérez",
            phone = "987654321",
            role = UserRole.COMPRADOR
        )

        val meetingPoint = CampusMeetingPoint(
            id = "mp-biblio",
            name = "Biblioteca - Puerta Principal",
            pavilion = "Central"
        )

        val order = createOrderUseCase(
            buyerProfile = buyer,
            meetingPoint = meetingPoint,
            scheduledTime = "13:00",
            paymentMethod = PaymentMethod.YAPE,
            cartResult = cartResult,
            notes = "Por favor llamar al llegar"
        )

        assertEquals("buyer-123", order.buyerId)
        assertEquals("Juan Pérez", order.buyerName)
        assertEquals("Biblioteca - Puerta Principal", order.meetingPointName)
        assertEquals("13:00", order.scheduledTime)
        assertEquals(30.0, order.totalAmount, 0.001)
        assertEquals(OrderStatus.PENDIENTE, order.status)
        assertEquals(2, order.subOrders.size)

        val subOrderPapu = order.subOrders.first { it.sellerId == "seller-papu" }
        assertEquals(20.0, subOrderPapu.subtotalAmount, 0.001)
        assertEquals(PaymentMethod.YAPE, subOrderPapu.paymentMethod)
        assertEquals(order.id, subOrderPapu.orderId)
    }

    @Test
    fun testRecalculateOrderWhenSellerRejects() {
        // Orden original de S/39: Papu (S/23), Dulce (S/10), Coffee (S/6)
        val sub1 = SubOrder(
            id = "sub-1",
            orderId = "ord-500",
            sellerId = "seller-papu",
            sellerName = "Papu Burger",
            subtotalAmount = 23.0,
            status = SubOrderStatus.ACEPTADO
        )
        val sub2 = SubOrder(
            id = "sub-2",
            orderId = "ord-500",
            sellerId = "seller-dulce",
            sellerName = "Dulce Valle",
            subtotalAmount = 10.0,
            status = SubOrderStatus.ACEPTADO
        )
        val sub3 = SubOrder(
            id = "sub-3",
            orderId = "ord-500",
            sellerId = "seller-coffee",
            sellerName = "Coffee Campus",
            subtotalAmount = 6.0,
            status = SubOrderStatus.PENDIENTE
        )

        val initialOrder = Order(
            id = "ord-500",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "13:00",
            totalAmount = 39.0,
            status = OrderStatus.PENDIENTE,
            subOrders = listOf(sub1, sub2, sub3)
        )

        // Coffee Campus rechaza su subpedido
        val rejectedSub3 = sub3.copy(status = SubOrderStatus.RECHAZADO, rejectionReason = "Sin stock de café")
        val recalculatedOrder = recalculateOrderUseCase(initialOrder, rejectedSub3)

        // El nuevo total debe excluir los S/6 rechazados -> S/33.0
        assertEquals(33.0, recalculatedOrder.totalAmount, 0.001)
        assertEquals(OrderStatus.PARCIALMENTE_ACEPTADA, recalculatedOrder.status)
    }
}
