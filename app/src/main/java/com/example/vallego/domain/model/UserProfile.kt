package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserProfile(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("phone") val phone: String = "",
    @SerialName("role") val role: UserRole = UserRole.COMPRADOR,
    @SerialName("rating_average") val ratingAverage: Double = 5.0,
    @SerialName("campus") val campus: String = "Los Olivos",
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("business_description") val businessDescription: String? = null,
    @SerialName("business_category") val businessCategory: String? = null,
    @SerialName("business_location") val businessLocation: String? = null,
    @SerialName("open_time") val openTime: String? = null,
    @SerialName("close_time") val closeTime: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("accepting_orders") val acceptingOrders: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)