package com.example.vallego.data.repository

import com.example.vallego.domain.model.Category
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.repository.ProductRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.upload
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
    @kotlinx.serialization.SerialName("image_url") val imageUrl: String? = null,
    @kotlinx.serialization.SerialName("is_active") val isActive: Boolean = true,
    @kotlinx.serialization.SerialName("pickup_location") val pickupLocation: String = "Campus Los Olivos"
)

@kotlinx.serialization.Serializable
data class ProductUpdateDto(
    @kotlinx.serialization.SerialName("name") val name: String,
    @kotlinx.serialization.SerialName("description") val description: String?,
    @kotlinx.serialization.SerialName("price") val price: Double,
    @kotlinx.serialization.SerialName("stock") val stock: Int,
    @kotlinx.serialization.SerialName("category_id") val categoryId: String?,
    @kotlinx.serialization.SerialName("image_url") val imageUrl: String?,
    @kotlinx.serialization.SerialName("is_active") val isActive: Boolean,
    @kotlinx.serialization.SerialName("pickup_location") val pickupLocation: String?
)

@kotlinx.serialization.Serializable
data class SellerBusinessProfileUpdateDto(
    @kotlinx.serialization.SerialName("business_name") val businessName: String?,
    @kotlinx.serialization.SerialName("business_status") val businessStatus: String,
    @kotlinx.serialization.SerialName("business_description") val businessDescription: String?,
    @kotlinx.serialization.SerialName("business_category") val businessCategory: String?,
    @kotlinx.serialization.SerialName("business_location") val businessLocation: String?,
    @kotlinx.serialization.SerialName("open_time") val openTime: String?,
    @kotlinx.serialization.SerialName("close_time") val closeTime: String?,
    @kotlinx.serialization.SerialName("banner_url") val bannerUrl: String?,
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String?,
    @kotlinx.serialization.SerialName("accepting_orders") val acceptingOrders: Boolean
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
    private val auth: Auth,
    private val storage: Storage? = null
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
                imageUrl = product.imageUrl,
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

    override suspend fun updateProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val dto = ProductUpdateDto(
                name = product.name.trim(),
                description = product.description?.trim(),
                price = product.price,
                stock = maxOf(0, product.stock),
                categoryId = product.categoryId,
                imageUrl = product.imageUrl,
                isActive = product.isActive && product.stock > 0,
                pickupLocation = product.pickupLocation
            )
            postgrest.from("products").update(dto) {
                filter {
                    eq("id", product.id)
                }
            }
            android.util.Log.d("ProductRepo", "Producto actualizado exitosamente: ${product.name} (id=${product.id})")
            Result.success(product)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "Error al actualizar producto: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("products").delete {
                filter {
                    eq("id", productId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // Si hay restricción por órdenes históricas, realizar baja lógica (soft-delete)
            try {
                postgrest.from("products").update(
                    mapOf("is_active" to false, "stock" to 0)
                ) {
                    filter {
                        eq("id", productId)
                    }
                }
                Result.success(Unit)
            } catch (inner: Exception) {
                Result.failure(e)
            }
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

    override suspend fun updateBusinessProfile(profile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val targetId = profile.id.ifBlank { auth.currentUserOrNull()?.id ?: "" }
            if (targetId.isBlank()) return@withContext Result.failure(IllegalStateException("No se pudo identificar la cuenta del vendedor."))

            val dto = SellerBusinessProfileUpdateDto(
                businessName = profile.businessName?.trim(),
                businessStatus = profile.businessStatus,
                businessDescription = profile.businessDescription?.trim(),
                businessCategory = profile.businessCategory?.trim(),
                businessLocation = profile.businessLocation?.trim(),
                openTime = profile.openTime,
                closeTime = profile.closeTime,
                bannerUrl = profile.bannerUrl,
                avatarUrl = profile.avatarUrl,
                acceptingOrders = profile.acceptingOrders
            )
            postgrest.from("profiles").update(dto) {
                filter {
                    eq("id", targetId)
                }
            }
            android.util.Log.d("ProductRepo", "Perfil de puesto actualizado exitosamente: ${profile.businessName}")
            Result.success(profile.copy(id = targetId))
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "Error al actualizar perfil de negocio: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(
        bucket: String,
        path: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storageClient = storage ?: return@withContext Result.failure(IllegalStateException("Servicio de almacenamiento no inicializado"))
            val bucketApi = storageClient.from(bucket)
            bucketApi.upload(path = path, data = bytes) {
                upsert = true
            }
            val publicUrl = bucketApi.publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "Error al subir imagen a $bucket: ${e.message}", e)
            Result.failure(e)
        }
    }
}
