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
        if (tab == SellerTab.PRODUCTOS) {
            loadProducts()
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
        description: String?
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
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.ACEPTADO)
            loadProducts()
        }
    }

    fun startPreparation(subOrderId: String) {
        viewModelScope.launch {
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.EN_PREPARACION)
        }
    }

    fun markReady(subOrderId: String) {
        viewModelScope.launch {
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.LISTO)
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
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.RECHAZADO, reason)
            dismissRejectionDialog()
            loadProducts()
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
            orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.COMPLETADO)
            dismissDeliveryDialog()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}