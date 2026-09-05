package com.example.vallego.features.seller

import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus

enum class SellerOrderFilter {
    TODOS,
    PENDIENTES,
    EN_PREPARACION,
    LISTOS,
    COMPLETADOS,
    RECHAZADOS
}

data class SellerDashboardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAcceptingOrders: Boolean = true,
    val subOrders: List<SubOrder> = emptyList(),
    val selectedFilter: SellerOrderFilter = SellerOrderFilter.TODOS,
    val selectedSubOrderForRejection: SubOrder? = null,
    val selectedSubOrderForDelivery: SubOrder? = null,
    val totalSubOrdersToday: Int = 0,
    val pendingCount: Int = 0,
    val inPreparationCount: Int = 0,
    val readyCount: Int = 0,
    val completedCount: Int = 0,
    val earningsToday: Double = 0.0
) {
    val filteredSubOrders: List<SubOrder>
        get() = when (selectedFilter) {
            SellerOrderFilter.TODOS -> subOrders
            SellerOrderFilter.PENDIENTES -> subOrders.filter { it.status == SubOrderStatus.PENDIENTE }
            SellerOrderFilter.EN_PREPARACION -> subOrders.filter { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION }
            SellerOrderFilter.LISTOS -> subOrders.filter { it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.ESPERANDO_ENTREGA }
            SellerOrderFilter.COMPLETADOS -> subOrders.filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
            SellerOrderFilter.RECHAZADOS -> subOrders.filter { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO }
        }
}