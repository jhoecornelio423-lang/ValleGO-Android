package com.example.vallego.features.cart

import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CartCalculationResult
import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.PaymentMethod

data class CartUiState(
    val calculation: CartCalculationResult = CartCalculationResult(emptyList()),
    val meetingPoints: List<CampusMeetingPoint> = defaultMeetingPoints,
    val selectedMeetingPoint: CampusMeetingPoint? = defaultMeetingPoints.firstOrNull(),
    val availableTimeSlots: List<String> = defaultTimeSlots,
    val selectedTimeSlot: String = "13:00",
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.YAPE,
    val orderNotes: String = "",
    val isSubmitting: Boolean = false,
    val placedOrder: Order? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = calculation.storeGroups.isEmpty()
    val canCheckout: Boolean get() = !isEmpty && selectedMeetingPoint != null && selectedTimeSlot.isNotBlank() && !isSubmitting
}

val defaultMeetingPoints = listOf(
    CampusMeetingPoint(id = "mp-1", name = "Biblioteca - Puerta Principal", pavilion = "Central"),
    CampusMeetingPoint(id = "mp-2", name = "Pabellón A - Hall Principal", pavilion = "Pabellón A"),
    CampusMeetingPoint(id = "mp-3", name = "Pabellón B - Explanada", pavilion = "Pabellón B"),
    CampusMeetingPoint(id = "mp-4", name = "Cafetería Central", pavilion = "Comedor"),
    CampusMeetingPoint(id = "mp-5", name = "Patio de Ingeniería", pavilion = "Pabellón C")
)

val defaultTimeSlots = listOf(
    "11:00", "11:30", "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00"
)
