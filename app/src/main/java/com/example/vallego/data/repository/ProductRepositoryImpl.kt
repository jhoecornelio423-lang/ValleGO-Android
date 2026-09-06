package com.example.vallego.data.repository

import com.example.vallego.domain.model.Category
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.repository.ProductRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@kotlinx.serialization.Serializable
data class ProductInsertDto(
    @kotlinx.serialization.SerialName("id") val id: String,
    @kotlinx.serialization.SerialName("seller_id") val sellerId: String,
    @kotlinx.serialization.SerialName("category_id") val categoryId: String,
    @kotlinx.serialization.SerialName("name") val name: String,
    @kotlinx.serialization.SerialName("description") val description: String,
    @kotlinx.serialization.SerialName("price") val price: Double,
    @kotlinx.serialization.SerialName("stock") val stock: Int,
    @kotlinx.serialization.SerialName("is_active") val isActive: Boolean = true,
    @kotlinx.serialization.SerialName("pickup_location") val pickupLocation: String = "Campus Los Olivos"
)

@kotlinx.serialization.Serializable
data class ProductStockOnlyDto(
    @kotlinx.serialization.SerialName("stock") val stock: Int
)

@kotlinx.serialization.Serializable
data class ProductStockAndActiveDto(
    @kotlinx.serialization.SerialName("stock") val stock: Int,
    @kotlinx.serialization.SerialName("is_active") val isActive: Boolean
)

class ProductRepositoryImpl(
    private val postgrest: Postgrest,
    private val auth: Auth
) : ProductRepository {

    private fun isValidUUID(value: String): Boolean {
        return try {
            java.util.UUID.fromString(value)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    override suspend fun getActiveProducts(): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val products = postgrest.from("products")
                .select {
                    filter {
                        eq("is_active", true)
                        gt("stock", 0)
                    }
                }
                .decodeList<Product>()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val categories = postgrest.from("categories")
                .select()
                .decodeList<Category>()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductsBySeller(sellerId: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val products = postgrest.from("products")
                .select {
                    filter {
                        eq("seller_id", sellerId)
                    }
                }
                .decodeList<Product>()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUserOrNull()?.id
            val sellerId = when {
                !currentUserId.isNullOrBlank() -> currentUserId
                !product.sellerId.isNullOrBlank() && isValidUUID(product.sellerId) -> product.sellerId
                else -> return@withContext Result.failure(IllegalStateException("No se pudo identificar la cuenta del vendedor activo."))
            }

            val prodId = if (isValidUUID(product.id)) product.id else java.util.UUID.randomUUID().toString()
            val catId = if (!product.categoryId.isNullOrBlank() && isValidUUID(product.categoryId)) {
                product.categoryId
            } else {
                "7cee355d-cf67-477c-bade-fc7867ddbe2a" // Default category: Comidas y Almuerzos
            }
            val desc = if (!product.description.isNullOrBlank()) product.description else product.name
            val pickup = if (!product.pickupLocation.isNullOrBlank()) product.pickupLocation else "Campus Los Olivos"

            val dto = ProductInsertDto(
                id = prodId,
                sellerId = sellerId,
                categoryId = catId,
                name = product.name.trim(),
                description = desc.trim(),
                price = product.price,
                stock = product.stock,
                isActive = product.isActive && product.stock > 0,
                pickupLocation = pickup
            )
            postgrest.from("products").insert(dto)
            android.util.Log.d("ProductRepo", "Producto creado exitosamente: ${dto.name} (id=$prodId, seller=$sellerId)")
            Result.success(product.copy(id = prodId, sellerId = sellerId, categoryId = catId, description = desc, pickupLocation = pickup))
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "Error al crear producto: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleProductActive(productId: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("products").update(
                mapOf("is_active" to isActive)
            ) {
                filter {
                    eq("id", productId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProductStock(productId: String, newStock: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val safeStock = maxOf(0, newStock)
            val isActive = safeStock > 0
            postgrest.from("products").update(
                ProductStockAndActiveDto(stock = safeStock, isActive = isActive)
            ) {
                filter {
                    eq("id", productId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSellerProfiles(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val profiles = postgrest.from("profiles")
                .select()
                .decodeList<UserProfile>()
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSellerAcceptingOrders(sellerId: String, accepting: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetId = sellerId.ifBlank { auth.currentUserOrNull()?.id ?: "" }
            if (targetId.isBlank()) return@withContext Result.failure(IllegalStateException("No se pudo identificar la cuenta del vendedor."))
            postgrest.from("profiles").update(
                mapOf("accepting_orders" to accepting)
            ) {
                filter {
                    eq("id", targetId)
                }
            }
            android.util.Log.d("ProductRepo", "Estado de puesto actualizado: accepting_orders=$accepting para seller=$targetId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "Error al actualizar accepting_orders: ${e.message}", e)
            Result.failure(e)
        }
    }
}
