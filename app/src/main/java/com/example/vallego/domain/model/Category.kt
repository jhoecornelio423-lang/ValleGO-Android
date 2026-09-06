package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Category(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String? = null,
    @SerialName("icon") val icon: String? = null
)
