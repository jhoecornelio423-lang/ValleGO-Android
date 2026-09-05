package com.example.vallego.features.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class SellerDashboardViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private var currentSellerId: String = ""

    fun initialize(sellerId: String, initialAcceptingOrders: Boolean) {
        currentSellerId = sellerId
        _uiState.update { it.copy(isAcceptingOrders = initialAcceptingOrders) }
        viewModelScope.launch {
            orderRepository.observeSubOrdersForSeller(sellerId).collect { orders ->
                val completed = orders.filter { it.status == SubOrderStatus.COMPLETADO }
                val earnings = completed.sumOf { it.subtotalAmount }
                _uiState.update {
                    it.copy(
                        subOrders = orders,
                        totalSubOrdersToday = orders.size,
                        pendingCount = orders.count { s -> s.status == SubOrderStatus.PENDIENTE },
                        inPreparationCount = orders.count { s -> s.status == SubOrderStatus.ACEPTADO || s.status == SubOrderStatus.EN_PREPARACION },
                        readyCount = orders.count { s -> s.status == SubOrderStatus.LISTO || s.status == SubOrderStatus.ESPERANDO_ENTREGA },
                        completedCount = completed.size,
                        earningsToday = earnings,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setFilter(filter: SellerOrderFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleAcceptingOrders(accepting: Boolean) {
        _uiState.update { it.copy(isAcceptingOrders = accepting) }
    }

    fun acceptSubOrder(subOrderId: String) {
        viewModelScope.launch {
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.ACEPTADO)
        }
    }

    fun startPreparation(subOrderId: String) {
        viewModelScope.launch {
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.EN_PREPARACION)
        }
    }

    fun markReady(subOrderId: String) {
        viewModelScope.launch {
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.LISTO)
        }
    }

    fun openRejectionDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForRejection = subOrder) }
    }

    fun dismissRejectionDialog() {
        _uiState.update { it.copy(selectedSubOrderForRejection = null) }
    }

    fun confirmRejection(subOrderId: String, reason: String) {
        viewModelScope.launch {
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.RECHAZADO, reason)
            dismissRejectionDialog()
        }
    }

    fun openDeliveryDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForDelivery = subOrder) }
    }

    fun dismissDeliveryDialog() {
        _uiState.update { it.copy(selectedSubOrderForDelivery = null) }
    }

    fun confirmDeliveryAndPayment(subOrderId: String) {
        viewModelScope.launch {
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.COMPLETADO)
            dismissDeliveryDialog()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}