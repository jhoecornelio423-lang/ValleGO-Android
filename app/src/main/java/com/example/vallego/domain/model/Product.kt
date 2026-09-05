package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Product(
    @SerialName("id") val id: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("category_id") val categoryId: String? = null,
    val name: String,
    @SerialName("description") val description: String? = null,
    val price: Double,
    val stock: Int,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("pickup_location") val pickupLocation: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)