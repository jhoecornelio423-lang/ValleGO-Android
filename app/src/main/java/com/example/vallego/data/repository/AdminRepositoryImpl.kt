package com.example.vallego.data.repository

import com.example.vallego.domain.model.ApplicationStatus
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.repository.AdminRepository
import com.example.vallego.domain.repository.OrderRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.UUID

class AdminRepositoryImpl(
    private val postgrest: Postgrest? = null,
    private val orderRepository: OrderRepository? = null
) : AdminRepository {

    private val _meetingPointsFlow = MutableStateFlow<List<CampusMeetingPoint>>(
        listOf(
            CampusMeetingPoint(
                id = "mp-1",
                name = "Biblioteca Central - Puerta Principal",
                description = "Zona de torniquetes de acceso",
                pavilion = "Edificio Central",
                isActive = true
            ),
            CampusMeetingPoint(
                id = "mp-2",
                name = "PabellÃ³n A - Zona de Bancas",
                description = "Patio central frente al cafetÃ­n",
                pavilion = "PabellÃ³n A",
                isActive = true
            ),
            CampusMeetingPoint(
                id = "mp-3",
                name = "PabellÃ³n C - Explanada",
                description = "Ãrea techada de mesas de estudio",
                pavilion = "PabellÃ³n C",
                isActive = true
            ),
            CampusMeetingPoint(
                id = "mp-4",
                name = "CafeterÃ­a Campus - Terraza",
                description = "Mesas al aire libre",
                pavilion = "PabellÃ³n D",
                isActive = false
            )
        )
    )

    private val _applicationsFlow = MutableStateFlow<List<SellerApplication>>(
        listOf(
            SellerApplication(
                id = "app-1",
                userId = "user-carlos",
                applicantName = "Carlos Mendoza Ramos",
                studentEmail = "cmendozar@ucvvirtual.edu.pe",
                storeName = "Postres UCV",
                category = "ReposterÃ­a",
                description = "Venta de queques, brownies y pies de limÃ³n caseros para recreo.",
                proposedLocation = "PabellÃ³n B piso 1",
                status = ApplicationStatus.PENDIENTE
            ),
            SellerApplication(
                id = "app-2",
                userId = "user-maria",
                applicantName = "MarÃ­a Fernanda Torres",
                studentEmail = "mtorres@ucvvirtual.edu.pe",
                storeName = "SÃ¡ndwiches Vallejo",
                category = "Comida RÃ¡pida",
                description = "Triples, empanadas y tostadas mixtas para el desayuno.",
                proposedLocation = "PabellÃ³n C entrada",
                status = ApplicationStatus.PENDIENTE
            )
        )
    )

    override fun observeMeetingPoints(): Flow<List<CampusMeetingPoint>> = _meetingPointsFlow.asStateFlow()

    override suspend fun createMeetingPoint(meetingPoint: CampusMeetingPoint): Result<CampusMeetingPoint> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    postgrest.from("campus_meeting_points").insert(meetingPoint)
                } catch (_: Exception) {
                    // Fallback
                }
            }
            val current = _meetingPointsFlow.value.toMutableList()
            current.add(meetingPoint)
            _meetingPointsFlow.value = current
            Result.success(meetingPoint)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleMeetingPoint(id: String, active: Boolean): Result<CampusMeetingPoint> = withContext(Dispatchers.IO) {
        try {
            var updated: CampusMeetingPoint? = null
            val current = _meetingPointsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index >= 0) {
                val point = current[index].copy(isActive = active)
                current[index] = point
                _meetingPointsFlow.value = current
                updated = point
            }
            if (updated != null) {
                Result.success(updated)
            } else {
                Result.failure(NoSuchElementException("Punto de encuentro no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeSellerApplications(): Flow<List<SellerApplication>> = _applicationsFlow.asStateFlow()

    override suspend fun approveSellerApplication(applicationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _applicationsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == applicationId }
            if (index >= 0) {
                current[index] = current[index].copy(status = ApplicationStatus.APROBADA)
                _applicationsFlow.value = current
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Solicitud no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectSellerApplication(applicationId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _applicationsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == applicationId }
            if (index >= 0) {
                current[index] = current[index].copy(
                    status = ApplicationStatus.RECHAZADA,
                    rejectionReason = reason
                )
                _applicationsFlow.value = current
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Solicitud no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeCampusMetrics(): Flow<CampusMetrics> {
        val ordersFlow = if (orderRepository is OrderRepositoryImpl) {
            orderRepository.ordersFlow
        } else {
            MutableStateFlow(emptyList())
        }

        return combine(_meetingPointsFlow, _applicationsFlow, ordersFlow) { points, apps, orders ->
            val activePoints = points.count { it.isActive }
            val pendingApps = apps.count { it.status == ApplicationStatus.PENDIENTE }
            val activeSellers = apps.count { it.status == ApplicationStatus.APROBADA } + 3 // base stalls (Papu, Dulce, Coffee)

            val completedSubOrders = orders.flatMap { it.subOrders }.filter { it.status == SubOrderStatus.COMPLETADO }
            val totalSales = completedSubOrders.sumOf { it.subtotalAmount }

            CampusMetrics(
                totalOrdersToday = orders.size,
                totalSalesToday = totalSales,
                activeSellersCount = activeSellers,
                activeMeetingPointsCount = activePoints,
                pendingApplicationsCount = pendingApps
            )
        }
    }
}