package com.example.vallego.features.admin

import com.example.vallego.data.repository.AdminRepositoryImpl
import com.example.vallego.data.repository.OrderRepositoryImpl
import com.example.vallego.domain.model.*
import com.example.vallego.domain.usecase.RecalculateOrderUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdminFlowTest {

    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var adminRepository: AdminRepositoryImpl

    @Before
    fun setup() {
        orderRepository = OrderRepositoryImpl(
            postgrest = null,
            recalculateOrderUseCase = RecalculateOrderUseCase()
        )
        adminRepository = AdminRepositoryImpl(
            postgrest = null,
            orderRepository = orderRepository
        )
    }

    @Test
    fun testInitialMeetingPointsAndCreation() = runTest {
        val initialPoints = adminRepository.observeMeetingPoints().first()
        assertTrue(initialPoints.isNotEmpty())
        val initialCount = initialPoints.size

        val newPoint = CampusMeetingPoint(
            id = "mp-test-custom",
            name = "Pabellón E - Puerta Posterior",
            pavilion = "Pabellón E",
            description = "Frente al laboratorio de cómputo",
            isActive = true
        )

        val createResult = adminRepository.createMeetingPoint(newPoint)
        assertTrue(createResult.isSuccess)

        val updatedPoints = adminRepository.observeMeetingPoints().first()
        assertEquals(initialCount + 1, updatedPoints.size)
        assertTrue(updatedPoints.any { it.id == "mp-test-custom" && it.name == "Pabellón E - Puerta Posterior" })
    }

    @Test
    fun testToggleMeetingPointActiveStatus() = runTest {
        // mp-1 is active by default
        var points = adminRepository.observeMeetingPoints().first()
        val mp1 = points.first { it.id == "mp-1" }
        assertTrue(mp1.isActive)

        // Desactivar mp-1
        val toggleResult = adminRepository.toggleMeetingPoint("mp-1", false)
        assertTrue(toggleResult.isSuccess)

        points = adminRepository.observeMeetingPoints().first()
        val updatedMp1 = points.first { it.id == "mp-1" }
        assertFalse(updatedMp1.isActive)

        // Reactivar mp-1
        adminRepository.toggleMeetingPoint("mp-1", true)
        points = adminRepository.observeMeetingPoints().first()
        assertTrue(points.first { it.id == "mp-1" }.isActive)
    }

    @Test
    fun testApproveSellerApplication() = runTest {
        val apps = adminRepository.observeSellerApplications().first()
        val app1 = apps.first { it.id == "app-1" }
        assertEquals(ApplicationStatus.PENDIENTE, app1.status)

        val approveResult = adminRepository.approveSellerApplication("app-1")
        assertTrue(approveResult.isSuccess)

        val updatedApps = adminRepository.observeSellerApplications().first()
        val approvedApp1 = updatedApps.first { it.id == "app-1" }
        assertEquals(ApplicationStatus.APROBADA, approvedApp1.status)
    }

    @Test
    fun testRejectSellerApplicationWithReason() = runTest {
        val rejectResult = adminRepository.rejectSellerApplication(
            applicationId = "app-2",
            reason = "Falta autorización de bienestar universitario"
        )
        assertTrue(rejectResult.isSuccess)

        val updatedApps = adminRepository.observeSellerApplications().first()
        val rejectedApp2 = updatedApps.first { it.id == "app-2" }
        assertEquals(ApplicationStatus.RECHAZADA, rejectedApp2.status)
        assertEquals("Falta autorización de bienestar universitario", rejectedApp2.rejectionReason)
    }

    @Test
    fun testCampusMetricsReflectOrdersAndApplications() = runTest {
        // Generar una orden completada para probar métricas de ventas
        val completedSubOrder = SubOrder(
            id = "sub-metrics-1",
            orderId = "order-metrics-1",
            sellerId = "seller-papu",
            sellerName = "Papu Burger",
            items = listOf(
                SubOrderItem("i1", "sub-metrics-1", "p1", "Hamburguesa", 15.0, 2, 30.0)
            ),
            subtotalAmount = 30.0,
            status = SubOrderStatus.COMPLETADO,
            paymentMethod = PaymentMethod.YAPE
        )

        val order = Order(
            id = "order-metrics-1",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "12:00",
            totalAmount = 30.0,
            status = OrderStatus.COMPLETADA,
            subOrders = listOf(completedSubOrder)
        )

        orderRepository.placeOrder(order)

        val metrics = adminRepository.observeCampusMetrics().first()
        assertEquals(1, metrics.totalOrdersToday)
        assertEquals(30.0, metrics.totalSalesToday, 0.001)
        assertTrue(metrics.activeMeetingPointsCount > 0)
        assertTrue(metrics.pendingApplicationsCount > 0)
    }
}
