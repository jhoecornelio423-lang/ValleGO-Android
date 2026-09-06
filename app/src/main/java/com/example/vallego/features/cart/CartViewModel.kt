package com.example.vallego.features.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.repository.CartRepository
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.usecase.CreateOrderWithSubordersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CartViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val createOrderWithSubordersUseCase: CreateOrderWithSubordersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        val schedule = generateDeliverySchedule()
        _uiState.update {
            it.copy(
                availableTimeSlots = schedule.slots,
                selectedTimeSlot = schedule.slots.firstOrNull() ?: "Hoy 12:00",
                isCampusClosedNow = schedule.isCampusClosedNow,
                deliveryScheduleNote = schedule.infoMessage
            )
        }
        viewModelScope.launch {
            cartRepository.cartCalculation.collect { calculation ->
                _uiState.update { it.copy(calculation = calculation) }
            }
        }
    }

    fun incrementItem(productId: String) {
        val currentItems = cartRepository.items.value
        val item = currentItems.firstOrNull { it.product.id == productId } ?: return
        cartRepository.updateQuantity(productId, item.quantity + 1)
    }

    fun decrementItem(productId: String) {
        val currentItems = cartRepository.items.value
        val item = currentItems.firstOrNull { it.product.id == productId } ?: return
        cartRepository.updateQuantity(productId, item.quantity - 1)
    }

    fun removeItem(productId: String) {
        cartRepository.removeFromCart(productId)
    }

    fun selectMeetingPoint(point: CampusMeetingPoint) {
        _uiState.update { it.copy(selectedMeetingPoint = point) }
    }

    fun selectTimeSlot(slot: String) {
        _uiState.update { it.copy(selectedTimeSlot = slot) }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(orderNotes = notes) }
    }

    fun clearPlacedOrder() {
        _uiState.update { it.copy(placedOrder = null, errorMessage = null) }
    }

    fun confirmOrder(buyerProfile: UserProfile) {
        val state = _uiState.value
        if (!state.canCheckout) return
        val point = state.selectedMeetingPoint ?: return

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val order = createOrderWithSubordersUseCase(
                    buyerProfile = buyerProfile,
                    meetingPoint = point,
                    scheduledTime = state.selectedTimeSlot,
                    paymentMethod = state.selectedPaymentMethod,
                    cartResult = state.calculation,
                    notes = state.orderNotes.takeIf { it.isNotBlank() }
                )

                orderRepository.placeOrder(order)
                    .onSuccess { placed ->
                        cartRepository.clearCart()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                placedOrder = placed
                            )
                        }
                    }
                    .onFailure { err ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = err.localizedMessage ?: "Error al confirmar pedido"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage ?: "Error inesperado"
                    )
                }
            }
        }
    }
}
