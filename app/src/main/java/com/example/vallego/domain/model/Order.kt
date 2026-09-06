package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Order(
    @SerialName("id") val id: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("buyer_name") var buyerName: String = "",
    @SerialName("meeting_point_id") val meetingPointId: String,
    @SerialName("meeting_point_name") var meetingPointName: String = "",
    @SerialName("scheduled_time") val scheduledTime: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("status") val status: OrderStatus = OrderStatus.PENDIENTE,
    @SerialName("sub_orders") val subOrders: List<SubOrder> = emptyList(),
    @SerialName("payment_method") val paymentMethod: PaymentMethod? = PaymentMethod.EFECTIVO,
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)