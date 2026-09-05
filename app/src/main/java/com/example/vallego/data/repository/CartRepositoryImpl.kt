package com.example.vallego.data.repository

import com.example.vallego.domain.model.CartCalculationResult
import com.example.vallego.domain.model.CartItem
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.repository.CartRepository
import com.example.vallego.domain.usecase.CalculateCartUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CartRepositoryImpl(
    private val calculateCartUseCase: CalculateCartUseCase = CalculateCartUseCase()
) : CartRepository {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    override val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    private val _storeNamesMap = mutableMapOf<String, String>()

    private val _cartCalculation = MutableStateFlow(calculateCartUseCase(emptyList(), emptyMap()))
    override val cartCalculation: StateFlow<CartCalculationResult> = _cartCalculation.asStateFlow()

    private fun recalculate() {
        _cartCalculation.value = calculateCartUseCase(_items.value, _storeNamesMap)
    }

    override fun setStoreName(sellerId: String, name: String) {
        _storeNamesMap[sellerId] = name
        recalculate()
    }

    override fun addToCart(product: Product, quantity: Int) {
        if (quantity <= 0) return
        _items.update { current ->
            val existingIndex = current.indexOfFirst { it.product.id == product.id }
            if (existingIndex >= 0) {
                val existing = current[existingIndex]
                val newQuantity = existing.quantity + quantity
                current.toMutableList().apply {
                    set(existingIndex, existing.copy(quantity = newQuantity))
                }
            } else {
                current + CartItem(product = product, quantity = quantity)
            }
        }
        recalculate()
    }

    override fun updateQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(productId)
            return
        }
        _items.update { current ->
            current.map {
                if (it.product.id == productId) it.copy(quantity = quantity) else it
            }
        }
        recalculate()
    }

    override fun removeFromCart(productId: String) {
        _items.update { current ->
            current.filterNot { it.product.id == productId }
        }
        recalculate()
    }

    override fun clearCart() {
        _items.value = emptyList()
        recalculate()
    }
}
