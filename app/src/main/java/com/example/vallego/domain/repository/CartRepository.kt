package com.example.vallego.domain.repository

import com.example.vallego.domain.model.CartCalculationResult
import com.example.vallego.domain.model.CartItem
import com.example.vallego.domain.model.Product
import kotlinx.coroutines.flow.StateFlow

interface CartRepository {
    val items: StateFlow<List<CartItem>>
    val cartCalculation: StateFlow<CartCalculationResult>

    fun addToCart(product: Product, quantity: Int = 1)
    fun updateQuantity(productId: String, quantity: Int)
    fun removeFromCart(productId: String)
    fun clearCart()
    fun setStoreName(sellerId: String, name: String)
}