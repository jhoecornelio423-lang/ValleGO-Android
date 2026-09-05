package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class CartItem(
    @SerialName("product")
    val product: Product,
    @SerialName("quantity")
    val quantity: Int,
    @SerialName("specialInstructions")
    val specialInstructions: String? = null
) {
    val subtotal: Double get() = product.price * quantity
}