package com.example.vallego.features.cart

import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CartCalculationResult
import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.PaymentMethod

data class DeliveryScheduleInfo(
    val slots: List<String>,
    val isCampusClosedNow: Boolean,
    val infoMessage: String
)

fun generateDeliverySchedule(): DeliveryScheduleInfo {
    val cal = java.util.Calendar.getInstance()
    val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val currentMinute = cal.get(java.util.Calendar.MINUTE)

    val campusOpenHour = 8
    val campusCloseHour = 22

    // Si ya pasó de las 21:30 o es noche/madrugada (hasta las 05:59):
    // El campus está cerrado para entregas inmediatas.
    // Solo se programan entregas para MAÑANA (08:00 a 22:00).
    if (currentHour > 21 || (currentHour == 21 && currentMinute > 30)) {
        val tomorrowSlots = mutableListOf<String>()
        var h = campusOpenHour
        var m = 0
        while (h < campusCloseHour || (h == campusCloseHour && m == 0)) {
            tomorrowSlots.add("Mañana %02d:%02d".format(h, m))
            m += 30
            if (m >= 60) {
                m = 0
                h++
            }
        }
        return DeliveryScheduleInfo(
            slots = tomorrowSlots,
            isCampusClosedNow = true,
            infoMessage = "🌙 Campus cerrado por hoy (atención hasta las 22:00). Tu pedido se programará para entrega mañana."
        )
    }

    // Si es antes de la apertura (ej. 06:00 o 07:30 AM):
    // Las entregas de hoy inician a las 08:00 AM.
    if (currentHour < campusOpenHour) {
        val todaySlots = mutableListOf<String>()
        var h = campusOpenHour
        var m = 0
        while (h < campusCloseHour || (h == campusCloseHour && m == 0)) {
            todaySlots.add("Hoy %02d:%02d".format(h, m))
            m += 30
            if (m >= 60) {
                m = 0
                h++
            }
        }
        return DeliveryScheduleInfo(
            slots = todaySlots,
            isCampusClosedNow = false,
            infoMessage = "☀️ El campus inicia entregas a las 08:00 AM. Puedes programar para hoy desde la apertura."
        )
    }

    // Horario regular del día (entre 08:00 y 21:30):
    // Buffer dinámico de 15 a 30 minutos a partir de la hora actual
    val nextMinute = if (currentMinute < 15) 30 else if (currentMinute < 45) 0 else 30
    val nextHour = if (currentMinute < 45) {
        if (currentMinute < 15) currentHour else currentHour + 1
    } else {
        currentHour + 1
    }

    val todaySlots = mutableListOf<String>()
    var h = nextHour
    var m = nextMinute

    while (h < campusCloseHour || (h == campusCloseHour && m == 0)) {
        todaySlots.add("Hoy %02d:%02d".format(h, m))
        m += 30
        if (m >= 60) {
            m = 0
            h++
        }
    }

    if (todaySlots.isEmpty()) {
        val tomorrowSlots = mutableListOf<String>()
        var th = campusOpenHour
        var tm = 0
        while (th < campusCloseHour || (th == campusCloseHour && tm == 0)) {
            tomorrowSlots.add("Mañana %02d:%02d".format(th, tm))
            tm += 30
            if (tm >= 60) {
                tm = 0
                th++
            }
        }
        return DeliveryScheduleInfo(
            slots = tomorrowSlots,
            isCampusClosedNow = true,
            infoMessage = "🌙 Entregas de hoy culminadas. Tu pedido se entregará mañana a primera hora."
        )
    }

    return DeliveryScheduleInfo(
        slots = todaySlots,
        isCampusClosedNow = false,
        infoMessage = "⏱️ Horarios con buffer de preparación de 15 a 30 min."
    )
}

fun generateAvailableTimeSlots(): List<String> = generateDeliverySchedule().slots

val defaultMeetingPoints = listOf(
    CampusMeetingPoint(id = "mp-1", name = "Biblioteca - Puerta Principal", pavilion = "Central"),
    CampusMeetingPoint(id = "mp-2", name = "Pabellón A - Hall Principal", pavilion = "Pabellón A"),
    CampusMeetingPoint(id = "mp-3", name = "Pabellón B - Explanada", pavilion = "Pabellón B"),
    CampusMeetingPoint(id = "mp-4", name = "Cafetería Central", pavilion = "Comedor"),
    CampusMeetingPoint(id = "mp-5", name = "Patio de Ingeniería", pavilion = "Pabellón C")
)

private val defaultSchedule = generateDeliverySchedule()

data class CartUiState(
    val calculation: CartCalculationResult = CartCalculationResult(emptyList()),
    val meetingPoints: List<CampusMeetingPoint> = defaultMeetingPoints,
    val selectedMeetingPoint: CampusMeetingPoint? = defaultMeetingPoints.firstOrNull(),
    val availableTimeSlots: List<String> = defaultSchedule.slots,
    val selectedTimeSlot: String = defaultSchedule.slots.firstOrNull() ?: "Hoy 12:00",
    val isCampusClosedNow: Boolean = defaultSchedule.isCampusClosedNow,
    val deliveryScheduleNote: String = defaultSchedule.infoMessage,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.YAPE,
    val orderNotes: String = "",
    val isSubmitting: Boolean = false,
    val placedOrder: Order? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = calculation.storeGroups.isEmpty()
    val canCheckout: Boolean get() = !isEmpty && selectedMeetingPoint != null && selectedTimeSlot.isNotBlank() && !isSubmitting
}
