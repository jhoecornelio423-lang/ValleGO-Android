package com.example.vallego.features.tracking

import com.example.vallego.domain.model.Order

data class OrderTrackingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null
)