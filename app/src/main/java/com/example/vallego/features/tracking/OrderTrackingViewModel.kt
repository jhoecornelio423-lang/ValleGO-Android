package com.example.vallego.features.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.Order
import com.example.vallego.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.repository.CartRepository

class OrderTrackingViewModel(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository
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
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setSelectedTab(tab: TrackingTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openCancelDialog(order: Order) {
        _uiState.update { it.copy(orderToCancel = order) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(orderToCancel = null) }
    }

    fun confirmCancelOrder(orderId: String) {
        _uiState.update { it.copy(isCancelling = true) }
        viewModelScope.launch {
            val result = orderRepository.cancelOrderByBuyer(orderId)
            _uiState.update { it.copy(isCancelling = false, orderToCancel = null) }
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(successMessage = "Pedido cancelado con éxito. El stock fue liberado.")
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al cancelar el pedido.")
                }
            }
        }
    }

    fun repeatOrder(order: Order, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            order.subOrders.forEach { subOrder ->
                if (subOrder.sellerName.isNotBlank()) {
                    cartRepository.setStoreName(subOrder.sellerId, subOrder.sellerName)
                }
                subOrder.items.forEach { item ->
                    val product = Product(
                        id = item.productId,
                        sellerId = subOrder.sellerId,
                        name = item.productName,
                        price = item.unitPrice,
                        stock = 99
                    )
                    cartRepository.addToCart(product, item.quantity)
                }
            }
            _uiState.update { it.copy(successMessage = "¡Productos agregados al carrito de compras!") }
            onCompleted()
        }
    }

    fun expirePendingOrders() {
        viewModelScope.launch {
            orderRepository.expirePendingSuborders()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}