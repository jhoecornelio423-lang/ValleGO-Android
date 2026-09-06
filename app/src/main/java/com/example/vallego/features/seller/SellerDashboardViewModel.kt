package com.example.vallego.features.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.Category
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class SellerDashboardViewModel(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private var currentSellerId: String = ""
    private var currentPickupLocation: String = "Campus Los Olivos"

    fun initialize(sellerId: String, initialAcceptingOrders: Boolean, businessLocation: String? = null) {
        currentSellerId = sellerId
        if (!businessLocation.isNullOrBlank()) {
            currentPickupLocation = businessLocation
        }
        _uiState.update { it.copy(isAcceptingOrders = initialAcceptingOrders) }
        loadProducts()
        loadSellerProfile()
        viewModelScope.launch {
            orderRepository.observeSubOrdersForSeller(sellerId).collect { orders ->
                val completed = orders.filter { it.status == SubOrderStatus.COMPLETADO }
                val earnings = completed.sumOf { it.subtotalAmount }
                _uiState.update {
                    it.copy(
                        subOrders = orders,
                        totalSubOrdersToday = orders.size,
                        pendingCount = orders.count { s -> s.status == SubOrderStatus.PENDIENTE },
                        inPreparationCount = orders.count { s -> s.status == SubOrderStatus.ACEPTADO || s.status == SubOrderStatus.EN_PREPARACION },
                        readyCount = orders.count { s -> s.status == SubOrderStatus.LISTO || s.status == SubOrderStatus.ESPERANDO_ENTREGA },
                        completedCount = completed.size,
                        earningsToday = earnings,
                        isLoading = false
                    )
                }
                loadProducts()
            }
        }
    }

    fun loadSellerProfile() {
        if (currentSellerId.isBlank()) return
        viewModelScope.launch {
            productRepository.getSellerProfiles().onSuccess { profiles ->
                val myProfile = profiles.find { it.id == currentSellerId }
                if (myProfile != null) {
                    _uiState.update {
                        it.copy(
                            sellerProfile = myProfile,
                            isAcceptingOrders = myProfile.acceptingOrders
                        )
                    }
                }
            }
        }
    }

    fun loadProducts() {
        if (currentSellerId.isBlank()) return
        viewModelScope.launch {
            productRepository.getProductsBySeller(currentSellerId).onSuccess { prods ->
                _uiState.update { it.copy(products = prods) }
            }
            productRepository.getCategories().onSuccess { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun setSelectedTab(tab: SellerTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            SellerTab.PRODUCTOS -> loadProducts()
            SellerTab.MI_PUESTO -> loadSellerProfile()
            SellerTab.PEDIDOS -> {}
        }
    }

    fun openAddProductDialog() {
        _uiState.update { it.copy(showAddProductDialog = true) }
    }

    fun dismissAddProductDialog() {
        _uiState.update { it.copy(showAddProductDialog = false) }
    }

    fun openEditStockDialog(product: Product) {
        _uiState.update { it.copy(selectedProductForStockEdit = product) }
    }

    fun dismissEditStockDialog() {
        _uiState.update { it.copy(selectedProductForStockEdit = null) }
    }

    fun updateStock(productId: String, newStock: Int) {
        if (newStock < 0) return
        viewModelScope.launch {
            val result = productRepository.updateProductStock(productId, newStock)
            if (result.isSuccess) {
                val updated = _uiState.value.products.map {
                    if (it.id == productId) {
                        it.copy(
                            stock = newStock,
                            isActive = if (newStock == 0) false else it.isActive
                        )
                    } else it
                }
                _uiState.update {
                    it.copy(
                        products = updated,
                        selectedProductForStockEdit = null,
                        successMessage = "Stock actualizado correctamente."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Error al actualizar stock en la nube.")
                }
            }
        }
    }

    fun createProduct(
        name: String,
        price: Double,
        stock: Int,
        categoryId: String?,
        description: String?,
        imageUrl: String? = null
    ) {
        if (name.isBlank() || price <= 0 || stock < 0) {
            _uiState.update { it.copy(errorMessage = "Por favor ingresa nombre, precio y stock válidos.") }
            return
        }
        _uiState.update { it.copy(isSavingProduct = true) }
        viewModelScope.launch {
            val fallbackCatId = categoryId?.takeIf { it.isNotBlank() }
                ?: _uiState.value.categories.firstOrNull()?.id
                ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a"
            val fallbackDesc = description?.takeIf { it.isNotBlank() } ?: name.trim()

            val newProduct = Product(
                id = UUID.randomUUID().toString(),
                sellerId = currentSellerId,
                categoryId = fallbackCatId,
                name = name.trim(),
                description = fallbackDesc,
                price = price,
                stock = stock,
                imageUrl = imageUrl?.takeIf { it.isNotBlank() },
                isActive = (stock > 0),
                pickupLocation = currentPickupLocation
            )
            val result = productRepository.createProduct(newProduct)
            if (result.isSuccess) {
                val created = result.getOrNull() ?: newProduct
                val updated = _uiState.value.products.toMutableList()
                updated.add(0, created)
                _uiState.update {
                    it.copy(
                        products = updated,
                        isSavingProduct = false,
                        showAddProductDialog = false,
                        errorMessage = null,
                        successMessage = "¡Producto agregado con éxito!"
                    )
                }
                loadProducts()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al guardar el producto en la nube."
                _uiState.update {
                    it.copy(
                        isSavingProduct = false,
                        errorMessage = "No se pudo guardar el producto: $err"
                    )
                }
            }
        }
    }

    fun toggleProductActive(productId: String, isActive: Boolean) {
        val product = _uiState.value.products.find { it.id == productId }
        if (isActive && (product == null || product.stock <= 0)) {
            _uiState.update { it.copy(errorMessage = "No puedes activar un producto sin stock disponible. Actualiza el stock primero.") }
            return
        }
        viewModelScope.launch {
            productRepository.toggleProductActive(productId, isActive)
            val updated = _uiState.value.products.map {
                if (it.id == productId) it.copy(isActive = isActive) else it
            }
            _uiState.update { it.copy(products = updated) }
        }
    }

    fun setFilter(filter: SellerOrderFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleAcceptingOrders(accepting: Boolean) {
        _uiState.update { it.copy(isAcceptingOrders = accepting) }
        viewModelScope.launch {
            val result = productRepository.updateSellerAcceptingOrders(currentSellerId, accepting)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = if (accepting) "Puesto Abierto: Ahora estás visible en el catálogo de campus." else "Puesto Cerrado: Tu catálogo ha sido pausado."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "No se pudo actualizar el estado del puesto en el servidor.")
                }
            }
        }
    }

    fun acceptSubOrder(subOrderId: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.ACEPTADO)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al aceptar el pedido.") }
            } else {
                loadProducts()
            }
        }
    }

    fun startPreparation(subOrderId: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.EN_PREPARACION)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al iniciar preparación.") }
            }
        }
    }

    fun markReady(subOrderId: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.LISTO)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al marcar pedido listo.") }
            }
        }
    }

    fun openRejectionDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForRejection = subOrder) }
    }

    fun dismissRejectionDialog() {
        _uiState.update { it.copy(selectedSubOrderForRejection = null) }
    }

    fun confirmRejection(subOrderId: String, reason: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.RECHAZADO, reason)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al rechazar el pedido.") }
            } else {
                dismissRejectionDialog()
                loadProducts()
            }
        }
    }

    fun openDeliveryDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForDelivery = subOrder) }
    }

    fun dismissDeliveryDialog() {
        _uiState.update { it.copy(selectedSubOrderForDelivery = null) }
    }

    fun confirmDeliveryAndPayment(subOrderId: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.COMPLETADO)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al confirmar entrega y pago.") }
            } else {
                dismissDeliveryDialog()
            }
        }
    }

    fun openEditProductDialog(product: Product) {
        _uiState.update { it.copy(selectedProductForEdit = product) }
    }

    fun dismissEditProductDialog() {
        _uiState.update { it.copy(selectedProductForEdit = null) }
    }

    fun updateProduct(
        productId: String,
        name: String,
        price: Double,
        stock: Int,
        categoryId: String?,
        description: String?,
        imageUrl: String?
    ) {
        if (name.isBlank() || price <= 0 || stock < 0) {
            _uiState.update { it.copy(errorMessage = "Ingresa nombre, precio y stock válidos.") }
            return
        }
        val current = _uiState.value.products.find { it.id == productId } ?: return
        val updatedProd = current.copy(
            name = name.trim(),
            price = price,
            stock = stock,
            categoryId = categoryId?.takeIf { it.isNotBlank() } ?: current.categoryId,
            description = description?.trim()?.takeIf { it.isNotBlank() } ?: current.description,
            imageUrl = imageUrl?.takeIf { it.isNotBlank() } ?: current.imageUrl,
            isActive = (stock > 0)
        )
        _uiState.update { it.copy(isSavingProduct = true) }
        viewModelScope.launch {
            val result = productRepository.updateProduct(updatedProd)
            if (result.isSuccess) {
                val updatedList = _uiState.value.products.map { if (it.id == productId) updatedProd else it }
                _uiState.update {
                    it.copy(
                        products = updatedList,
                        selectedProductForEdit = null,
                        isSavingProduct = false,
                        successMessage = "¡Producto actualizado exitosamente!"
                    )
                }
                loadProducts()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al actualizar el producto."
                _uiState.update { it.copy(isSavingProduct = false, errorMessage = err) }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            val result = productRepository.deleteProduct(productId)
            if (result.isSuccess) {
                val updatedList = _uiState.value.products.filter { it.id != productId }
                _uiState.update {
                    it.copy(
                        products = updatedList,
                        selectedProductForEdit = null,
                        successMessage = "Producto eliminado con éxito."
                    )
                }
                loadProducts()
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo eliminar el producto.") }
            }
        }
    }

    fun updateBusinessProfile(
        businessName: String,
        businessStatus: String,
        businessDescription: String?,
        businessCategory: String?,
        businessLocation: String?,
        openTime: String?,
        closeTime: String?,
        bannerUrl: String?,
        avatarUrl: String?,
        acceptingOrders: Boolean
    ) {
        val currentProfile = _uiState.value.sellerProfile
        val profileToSave = (currentProfile ?: com.example.vallego.domain.model.UserProfile(
            id = currentSellerId,
            fullName = businessName.ifBlank { "Emprendedor" }
        )).copy(
            id = currentSellerId,
            businessName = businessName.trim().takeIf { it.isNotBlank() },
            businessStatus = businessStatus,
            businessDescription = businessDescription?.trim()?.takeIf { it.isNotBlank() },
            businessCategory = businessCategory?.trim()?.takeIf { it.isNotBlank() },
            businessLocation = businessLocation?.trim()?.takeIf { it.isNotBlank() },
            openTime = openTime?.trim()?.takeIf { it.isNotBlank() },
            closeTime = closeTime?.trim()?.takeIf { it.isNotBlank() },
            bannerUrl = bannerUrl?.trim()?.takeIf { it.isNotBlank() },
            avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotBlank() },
            acceptingOrders = acceptingOrders
        )

        _uiState.update { it.copy(isSavingProfile = true) }
        viewModelScope.launch {
            val result = productRepository.updateBusinessProfile(profileToSave)
            if (result.isSuccess) {
                val updated = result.getOrNull() ?: profileToSave
                _uiState.update {
                    it.copy(
                        sellerProfile = updated,
                        isAcceptingOrders = updated.acceptingOrders,
                        isSavingProfile = false,
                        successMessage = "¡Puesto actualizado con éxito!"
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al guardar el puesto."
                _uiState.update { it.copy(isSavingProfile = false, errorMessage = err) }
            }
        }
    }

    fun openNoShowDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForNoShow = subOrder) }
    }

    fun dismissNoShowDialog() {
        _uiState.update { it.copy(selectedSubOrderForNoShow = null) }
    }

    fun confirmBuyerNoShow(subOrderId: String, reason: String) {
        viewModelScope.launch {
            val result = orderRepository.markBuyerNoShow(subOrderId, reason.ifBlank { "Comprador no se presentó al punto" })
            if (result.isSuccess) {
                dismissNoShowDialog()
                _uiState.update { it.copy(successMessage = "Subpedido marcado como NO entregado (stock devuelto).") }
                loadProducts()
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al reportar inasistencia.") }
            }
        }
    }

    fun onSubOrderExpired(subOrderId: String) {
        viewModelScope.launch {
            orderRepository.expirePendingSuborders()
            _uiState.update { it.copy(errorMessage = "El subpedido ha expirado tras 15 minutos sin ser aceptado.") }
        }
    }

    fun uploadAsset(bucket: String, path: String, bytes: ByteArray, onUploaded: (String) -> Unit) {
        _uiState.update { it.copy(isUploadingAsset = true) }
        viewModelScope.launch {
            val result = productRepository.uploadImage(bucket, path, bytes)
            _uiState.update { it.copy(isUploadingAsset = false) }
            result.onSuccess { url ->
                onUploaded(url)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = "Error al subir imagen: ${err.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}