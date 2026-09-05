package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class StoreCartGroup(
    @SerialName("seller_id")
    val sellerId: String,
    @SerialName("seller_name")
    val sellerName: String,
    val items: List<CartItem>,
    val subtotal: Double = items.sumOf { it.subtotal }
)

@Serializable
data class CartCalculationResult(
    val storeGroups: List<StoreCartGroup>,
    val grandTotal: Double = storeGroups.sumOf { it.subtotal },
    val totalItemCount: Int = storeGroups.sumOf { group -> group.items.sumOf { it.quantity } }
)