package com.example.vallego.domain.repository

import com.example.vallego.domain.model.Category
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.UserProfile

interface ProductRepository {
    suspend fun getActiveProducts(): Result<List<Product>>
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getProductsBySeller(sellerId: String): Result<List<Product>>
    suspend fun createProduct(product: Product): Result<Product>
    suspend fun toggleProductActive(productId: String, isActive: Boolean): Result<Unit>
    suspend fun updateProductStock(productId: String, newStock: Int): Result<Unit>
    suspend fun getSellerProfiles(): Result<List<UserProfile>>
    suspend fun updateSellerAcceptingOrders(sellerId: String, accepting: Boolean): Result<Unit>
}
