package com.example.vallego.features.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.Order
import com.example.vallego.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class OrderTrackingViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState = _uiState.asStateFlow()

    fun initialize(buyerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            orderRepository.observeOrdersForBuyer(buyerId).collect { buyerOrders ->
                _uiState.update {
                    it.copy(
                        orders = buyerOrders,
                        isLoading = false,
                        selectedOrder = it.selectedOrder?.let { sel -> buyerOrders.find { o -> o.id == sel.id } } ?: buyerOrders.firstOrNull()
                    )
                }
            }
        }
    }

    fun selectOrder(order: Order) {
        _uiState.update { it.copy(selectedOrder = order) }
    }
}