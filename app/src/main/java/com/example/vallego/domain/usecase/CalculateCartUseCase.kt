package com.example.vallego.domain.usecase

import com.example.vallego.domain.model.CartCalculationResult
import com.example.vallego.domain.model.CartItem
import com.example.vallego.domain.model.StoreCartGroup

class CalculateCartUseCase {
    operator fun invoke(items: List<CartItem>, storeNamesMap: Map<String, String> = emptyMap()): CartCalculationResult {
        val groupedItems = items.groupBy { it.product.sellerId }

        val storeCartGroups = groupedItems.map { (sellerId, sellerItems) ->
            val sellerName = storeNamesMap[sellerId] ?: ("Puesto " + sellerId.take(6))
            val subtotal = sellerItems.sumOf { it.subtotal }
            StoreCartGroup(
                sellerId = sellerId,
                sellerName = sellerName,
                items = sellerItems,
                subtotal = subtotal
            )
        }

        return CartCalculationResult(storeGroups = storeCartGroups)
    }
}