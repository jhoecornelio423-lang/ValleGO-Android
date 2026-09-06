package com.example.vallego.data.repository

import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.SubOrderItem
import com.example.vallego.domain.model.OrderStatus
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.usecase.RecalculateOrderUseCase
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
data class RemoteOrderDto(
    val id: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("delivery_place") val deliveryPlace: String? = null,
    val status: String = "pending",
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RemoteOrderInsertDto(
    val id: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("delivery_place") val deliveryPlace: String? = null,
    val status: String = "pending",
    @SerialName("order_code") val orderCode: String? = null
)

@Serializable
data class ProductStockDto(
    val id: String,
    val stock: Int,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class ProductBasicDto(
    val id: String,
    val name: String,
    val price: Double? = null,
    val stock: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class RemoteOrderItemDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("product_id") val productId: String,
    val quantity: Int,
    @SerialName("price_at_sale") val priceAtSale: Double
)

@Serializable
data class OrderStockUpdateDto(
    @SerialName("stock") val stock: Int,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class OrderStatusUpdateDto(
    @SerialName("status") val status: String
)

class OrderRepositoryImpl(
    private val postgrest: Postgrest? = null,
    private val recalculateOrderUseCase: RecalculateOrderUseCase = RecalculateOrderUseCase()
) : OrderRepository {

    private val _ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    val ordersFlow = _ordersFlow.asStateFlow()

    override suspend fun placeOrder(order: Order): Result<Order> = withContext(Dispatchers.IO) {
        try {
            // 1. Guardar en memoria local inmediata
            val currentList = _ordersFlow.value.toMutableList()
            currentList.add(0, order)
            _ordersFlow.value = currentList

            // 2. Persistir en la nube de Supabase (una única inserción limpia y determinista)
            // IMPORTANTE: El stock NO se descuenta aquí en estado pendiente.
            // Se descontará únicamente cuando el emprendedor acepte formalmente el pedido.
            if (postgrest != null) {
                for (subOrder in order.subOrders) {
                    val remoteOrderId = if (isValidUUID(subOrder.id)) subOrder.id else UUID.randomUUID().toString()
                    val remoteOrder = RemoteOrderInsertDto(
                        id = remoteOrderId,
                        buyerId = order.buyerId,
                        sellerId = subOrder.sellerId,
                        totalPrice = subOrder.subtotalAmount,
                        deliveryPlace = "${order.meetingPointName} (${order.scheduledTime})",
                        status = "pending",
                        orderCode = order.id.takeLast(6).uppercase()
                    )

                    try {
                        postgrest.from("orders").insert(remoteOrder)

                        // Insertar ítems del subpedido en order_items
                        for (item in subOrder.items) {
                            val itemId = UUID.randomUUID().toString()
                            val prodId = if (isValidUUID(item.productId)) item.productId else UUID.randomUUID().toString()
                            val remoteItem = RemoteOrderItemDto(
                                id = itemId,
                                orderId = remoteOrderId,
                                productId = prodId,
                                quantity = item.quantity,
                                priceAtSale = item.unitPrice
                            )
                            try {
                                postgrest.from("order_items").insert(remoteItem)
                            } catch (itemErr: Exception) {
                                itemErr.printStackTrace()
                            }
                        }
                    } catch (orderErr: Exception) {
                        orderErr.printStackTrace()
                    }
                }
            }

            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val productNameCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private suspend fun fetchOrderItemsWithNames(orderIds: List<String>): Map<String, List<SubOrderItem>> {
        if (postgrest == null || orderIds.isEmpty()) return emptyMap()
        return try {
            val items = postgrest.from("order_items")
                .select {
                    filter {
                        isIn("order_id", orderIds)
                    }
                }
                .decodeList<RemoteOrderItemDto>()

            if (items.isEmpty()) return emptyMap()

            val missingProductIds = items.map { it.productId }
                .filter { isValidUUID(it) && !productNameCache.containsKey(it) }
                .distinct()

            if (missingProductIds.isNotEmpty()) {
                try {
                    val prods = postgrest.from("products")
                        .select {
                            filter {
                                isIn("id", missingProductIds)
                            }
                        }
                        .decodeList<ProductBasicDto>()
                    for (p in prods) {
                        productNameCache[p.id] = p.name
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            items.groupBy { it.orderId }.mapValues { (_, orderItems) ->
                orderItems.map { oi ->
                    val name = productNameCache[oi.productId] ?: "Producto Valle-Go"
                    SubOrderItem(
                        id = oi.id,
                        subOrderId = oi.orderId,
                        productId = oi.productId,
                        productName = name,
                        quantity = oi.quantity,
                        unitPrice = oi.priceAtSale,
                        subtotal = oi.priceAtSale * oi.quantity
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    override suspend fun getOrdersForBuyer(buyerId: String): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val matching = _ordersFlow.value.filter { it.buyerId == buyerId }
            Result.success(matching)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSubOrdersForSeller(sellerId: String): Result<List<SubOrder>> = withContext(Dispatchers.IO) {
        try {
            val subOrders = _ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId }
            Result.success(subOrders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeOrdersForBuyer(buyerId: String): Flow<List<Order>> = flow {
        // Emitir primero el caché local
        emit(_ordersFlow.value.filter { it.buyerId == buyerId })

        while (true) {
            try {
                if (postgrest != null && isValidUUID(buyerId)) {
                    val remoteOrders = postgrest.from("orders")
                        .select {
                            filter {
                                eq("buyer_id", buyerId)
                            }
                        }
                        .decodeList<RemoteOrderDto>()

                    if (remoteOrders.isNotEmpty()) {
                        val currentLocal = _ordersFlow.value.toMutableList()
                        var hasChanges = false

                        val neededOrderIds = remoteOrders.filter { ro ->
                            currentLocal.find { it.id == ro.id }?.subOrders?.firstOrNull()?.items.isNullOrEmpty()
                        }.map { it.id }

                        val itemsByOrder = if (neededOrderIds.isNotEmpty()) {
                            fetchOrderItemsWithNames(neededOrderIds)
                        } else emptyMap()

                        for (ro in remoteOrders) {
                            val newSubStatus = mapRemoteStatusToLocal(ro.status)
                            var found = false
                            for (i in currentLocal.indices) {
                                val order = currentLocal[i]
                                val subIndex = order.subOrders.indexOfFirst { it.id == ro.id }
                                if (subIndex >= 0) {
                                    found = true
                                    val currentSub = order.subOrders[subIndex]
                                    val updatedItems = if (currentSub.items.isEmpty()) {
                                        itemsByOrder[ro.id] ?: emptyList()
                                    } else currentSub.items

                                    if (currentSub.status != newSubStatus || currentSub.items.isEmpty()) {
                                        val updatedSub = currentSub.copy(
                                            status = newSubStatus,
                                            items = updatedItems
                                        )
                                        val recalculated = recalculateOrderUseCase(order, updatedSub)
                                        currentLocal[i] = recalculated
                                        hasChanges = true
                                    }
                                }
                            }

                            if (!found) {
                                val subItems = itemsByOrder[ro.id] ?: emptyList()
                                val sub = SubOrder(
                                    id = ro.id,
                                    orderId = ro.id,
                                    sellerId = ro.sellerId,
                                    sellerName = "Emprendedor Valle-Go",
                                    items = subItems,
                                    subtotalAmount = ro.totalPrice,
                                    status = newSubStatus,
                                    paymentMethod = PaymentMethod.EFECTIVO,
                                    isPaymentConfirmed = (newSubStatus == SubOrderStatus.COMPLETADO),
                                    isDeliveryConfirmed = (newSubStatus == SubOrderStatus.COMPLETADO),
                                    createdAt = ro.createdAt
                                )
                                val reconstructed = Order(
                                    id = ro.id,
                                    buyerId = buyerId,
                                    buyerName = "Comprador",
                                    meetingPointId = "mp-vallego",
                                    meetingPointName = ro.deliveryPlace ?: "Campus Los Olivos",
                                    scheduledTime = "Turno seleccionado",
                                    totalAmount = ro.totalPrice,
                                    status = mapSubStatusToGeneral(newSubStatus),
                                    subOrders = listOf(sub),
                                    notes = null
                                )
                                currentLocal.add(reconstructed)
                                hasChanges = true
                            }
                        }

                        if (hasChanges) {
                            _ordersFlow.value = currentLocal
                        }
                    }
                }
            } catch (_: Exception) {
                // Polling silencioso
            }

            emit(_ordersFlow.value.filter { it.buyerId == buyerId })
            delay(3000)
        }
    }.flowOn(Dispatchers.IO)

    override fun observeSubOrdersForSeller(sellerId: String): Flow<List<SubOrder>> = flow {
        // Emitir primero el caché local
        emit(_ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId })

        while (true) {
            try {
                if (postgrest != null && isValidUUID(sellerId)) {
                    val remoteOrders = postgrest.from("orders")
                        .select {
                            filter {
                                eq("seller_id", sellerId)
                            }
                        }
                        .decodeList<RemoteOrderDto>()

                    if (remoteOrders.isNotEmpty()) {
                        val currentLocal = _ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId }.toMutableList()
                        val mappedList = mutableListOf<SubOrder>()

                        val neededOrderIds = remoteOrders.filter { ro ->
                            currentLocal.find { it.id == ro.id }?.items.isNullOrEmpty()
                        }.map { it.id }

                        val itemsByOrder = if (neededOrderIds.isNotEmpty()) {
                            fetchOrderItemsWithNames(neededOrderIds)
                        } else emptyMap()

                        for (ro in remoteOrders) {
                            val existing = currentLocal.find { it.id == ro.id }
                            val subStatus = mapRemoteStatusToLocal(ro.status)
                            val items = if (existing != null && existing.items.isNotEmpty()) {
                                existing.items
                            } else {
                                itemsByOrder[ro.id] ?: emptyList()
                            }

                            val sellerName = existing?.sellerName ?: "Emprendedor Valle-Go"

                            mappedList.add(
                                SubOrder(
                                    id = ro.id,
                                    orderId = ro.id,
                                    sellerId = ro.sellerId,
                                    sellerName = sellerName,
                                    items = items,
                                    subtotalAmount = ro.totalPrice,
                                    status = subStatus,
                                    paymentMethod = existing?.paymentMethod ?: PaymentMethod.EFECTIVO,
                                    isPaymentConfirmed = (subStatus == SubOrderStatus.COMPLETADO),
                                    isDeliveryConfirmed = (subStatus == SubOrderStatus.COMPLETADO),
                                    createdAt = ro.createdAt
                                )
                            )
                        }

                        emit(mappedList)
                    } else {
                        emit(_ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId })
                    }
                } else {
                    emit(_ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId })
                }
            } catch (_: Exception) {
                emit(_ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId })
            }

            delay(3000)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateSubOrderStatus(
        subOrderId: String,
        newStatus: SubOrderStatus,
        rejectionReason: String?
    ): Result<SubOrder> = withContext(Dispatchers.IO) {
        try {
            var updated: SubOrder? = null
            var previousStatus: SubOrderStatus? = null
            var targetItems: List<SubOrderItem> = emptyList()

            val currentOrders = _ordersFlow.value.toMutableList()
            for (i in currentOrders.indices) {
                val order = currentOrders[i]
                val subOrderIndex = order.subOrders.indexOfFirst { it.id == subOrderId }
                if (subOrderIndex >= 0) {
                    val currentSub = order.subOrders[subOrderIndex]
                    previousStatus = currentSub.status
                    targetItems = currentSub.items

                    val isPayConfirmed = if (newStatus == SubOrderStatus.COMPLETADO || newStatus == SubOrderStatus.PAGO_CONFIRMADO) true else currentSub.isPaymentConfirmed
                    val isDelivConfirmed = if (newStatus == SubOrderStatus.COMPLETADO) true else currentSub.isDeliveryConfirmed
                    val newSub = currentSub.copy(
                        status = newStatus,
                        rejectionReason = rejectionReason ?: currentSub.rejectionReason,
                        isPaymentConfirmed = isPayConfirmed,
                        isDeliveryConfirmed = isDelivConfirmed
                    )
                    val recalculatedOrder = recalculateOrderUseCase(order, newSub)
                    currentOrders[i] = recalculatedOrder
                    updated = newSub
                    break
                }
            }

            if (updated != null) {
                _ordersFlow.value = currentOrders
            } else {
                updated = SubOrder(
                    id = subOrderId,
                    orderId = subOrderId,
                    sellerId = "",
                    sellerName = "Subpedido",
                    items = emptyList(),
                    subtotalAmount = 0.0,
                    status = newStatus,
                    rejectionReason = rejectionReason
                )
            }

            // Si los ítems no estaban en memoria local, resolverlos desde Supabase order_items
            val itemsToProcess = if (targetItems.isNotEmpty()) {
                targetItems
            } else if (postgrest != null) {
                try {
                    val rawItems = postgrest.from("order_items")
                        .select { filter { eq("order_id", subOrderId) } }
                        .decodeList<RemoteOrderItemDto>()
                    rawItems.map { oi ->
                        SubOrderItem(
                            id = oi.id,
                            subOrderId = subOrderId,
                            productId = oi.productId,
                            productName = "Producto",
                            quantity = oi.quantity,
                            unitPrice = oi.priceAtSale,
                            subtotal = oi.priceAtSale * oi.quantity
                        )
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()

            // GESTIÓN DE STOCK SEGÚN EL CICLO DE VIDA DEL PEDIDO:
            // 1. Cuando el vendedor ACEPTA el pedido por primera vez: descontar el stock
            if (newStatus == SubOrderStatus.ACEPTADO && (previousStatus == null || previousStatus == SubOrderStatus.PENDIENTE)) {
                if (postgrest != null) {
                    for (item in itemsToProcess) {
                        if (isValidUUID(item.productId) && item.quantity > 0) {
                            try {
                                val prod = postgrest.from("products")
                                    .select { filter { eq("id", item.productId) } }
                                    .decodeSingleOrNull<ProductStockDto>()
                                if (prod != null) {
                                    val newStock = maxOf(0, prod.stock - item.quantity)
                                    val isActive = newStock > 0
                                    postgrest.from("products").update(
                                        ProductStockAndActiveDto(stock = newStock, isActive = isActive)
                                    ) {
                                        filter { eq("id", item.productId) }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            // 2. Si un pedido que YA HABÍA SIDO ACEPTADO es cancelado o rechazado: RESTAURAR el stock
            val wasPreviouslyAccepted = previousStatus in listOf(
                SubOrderStatus.ACEPTADO,
                SubOrderStatus.EN_PREPARACION,
                SubOrderStatus.LISTO,
                SubOrderStatus.ESPERANDO_ENTREGA
            )
            if (wasPreviouslyAccepted && newStatus in listOf(SubOrderStatus.RECHAZADO, SubOrderStatus.CANCELADO)) {
                if (postgrest != null) {
                    for (item in itemsToProcess) {
                        if (isValidUUID(item.productId) && item.quantity > 0) {
                            try {
                                val prod = postgrest.from("products")
                                    .select { filter { eq("id", item.productId) } }
                                    .decodeSingleOrNull<ProductStockDto>()
                                if (prod != null) {
                                    val newStock = prod.stock + item.quantity
                                    postgrest.from("products").update(
                                        ProductStockAndActiveDto(stock = newStock, isActive = true)
                                    ) {
                                        filter { eq("id", item.productId) }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            // Sincronizar actualización de estado con Supabase
            try {
                if (postgrest != null) {
                    val remoteStatus = mapLocalStatusToRemote(newStatus)
                    postgrest.from("orders").update(
                        OrderStatusUpdateDto(status = remoteStatus)
                    ) {
                        filter { eq("id", subOrderId) }
                    }
                }
            } catch (_: Exception) {
                // Fallback offline
            }

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapRemoteStatusToLocal(status: String): SubOrderStatus {
        return when (status.lowercase()) {
            "pending" -> SubOrderStatus.PENDIENTE
            "accepted" -> SubOrderStatus.ACEPTADO
            "preparing" -> SubOrderStatus.EN_PREPARACION
            "ready" -> SubOrderStatus.LISTO
            "completed" -> SubOrderStatus.COMPLETADO
            "cancelled" -> SubOrderStatus.RECHAZADO
            else -> SubOrderStatus.PENDIENTE
        }
    }

    private fun mapLocalStatusToRemote(status: SubOrderStatus): String {
        return when (status) {
            SubOrderStatus.PENDIENTE -> "pending"
            SubOrderStatus.ACEPTADO -> "accepted"
            SubOrderStatus.EN_PREPARACION -> "preparing"
            SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> "ready"
            SubOrderStatus.COMPLETADO, SubOrderStatus.PAGO_CONFIRMADO -> "completed"
            SubOrderStatus.RECHAZADO, SubOrderStatus.CANCELADO -> "cancelled"
            else -> "pending"
        }
    }

    private fun isValidUUID(value: String): Boolean {
        return try {
            UUID.fromString(value)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun mapSubStatusToGeneral(status: SubOrderStatus): OrderStatus = when (status) {
        SubOrderStatus.PENDIENTE -> OrderStatus.PENDIENTE
        SubOrderStatus.ACEPTADO, SubOrderStatus.EN_PREPARACION -> OrderStatus.EN_PROCESO
        SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> OrderStatus.EN_PROCESO
        SubOrderStatus.COMPLETADO -> OrderStatus.COMPLETADA
        SubOrderStatus.RECHAZADO, SubOrderStatus.CANCELADO -> OrderStatus.CANCELADA
        else -> OrderStatus.PENDIENTE
    }
}
