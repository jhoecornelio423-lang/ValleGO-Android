package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class UserRole {
    @SerialName("comprador")
    COMPRADOR,
    @SerialName("emprendedor")
    EMPRENDEDOR,
    @SerialName("admin")
    ADMIN,
    @SerialName("suspended")
    SUSPENDED,
    @SerialName("suspended_buyer")
    SUSPENDED_BUYER;

    val canBuy: Boolean get() = this == COMPRADOR || this == EMPRENDEDOR || this == ADMIN
    val canSell: Boolean get() = this == EMPRENDEDOR
    val isAdmin: Boolean get() = this == ADMIN
    val isSuspended: Boolean get() = this == SUSPENDED || this == SUSPENDED_BUYER
}