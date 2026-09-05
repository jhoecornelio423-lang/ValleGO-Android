package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SubOrderItem(
    @SerialName("id") val id: String,
    @SerialName("sub_order_id") val subOrderId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("unit_price") val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double
)

@Serializable
data class SubOrder(
    @SerialName("id") val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("seller_name") val sellerName: String = "",
    @SerialName("items") val items: List<SubOrderItem> = emptyList(),
    @SerialName("subtotal_amount") val subtotalAmount: Double = 0.0,
    @SerialName("status") val status: SubOrderStatus = SubOrderStatus.PENDIENTE,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("payment_method") val paymentMethod: PaymentMethod? = null,
    @SerialName("is_payment_confirmed") val isPaymentConfirmed: Boolean = false,
    @SerialName("is_delivery_confirmed") val isDeliveryConfirmed: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)