package com.example.vallego.data.repository

import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.OrderStatus
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderItem
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class RemoteOrderDto(
    val id: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("delivery_place") val deliveryPlace: String? = null,
    @SerialName("meeting_point_id") val meetingPointId: String? = null,
    @SerialName("meeting_point_name") val meetingPointName: String? = null,
    @SerialName("scheduled_time") val scheduledTime: String? = null,
    val notes: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val status: String = "pending",
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RemoteSubOrderDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("subtotal_amount") val subtotalAmount: Double,
    val status: String = "pending",
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("is_payment_confirmed") val isPaymentConfirmed: Boolean = false,
    @SerialName("is_delivery_confirmed") val isDeliveryConfirmed: Boolean = false,
    @SerialName("stock_reserved") val stockReserved: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RemoteOrderItemDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("sub_order_id") val subOrderId: String? = null,
    @SerialName("product_id") val productId: String,
    val quantity: Int,
    @SerialName("price_at_sale") val priceAtSale: Double
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
data class ProfileBasicDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    @SerialName("business_description") val businessDescription: String? = null
)

@Serializable
data class CheckoutResponseDto(
    val success: Boolean = false,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    val status: String? = null,
    @SerialName("is_duplicate") val isDuplicate: Boolean? = null,
    val message: String? = null
)

@Serializable
data class UpdateSuborderStatusResponseDto(
    val success: Boolean = false,
    @SerialName("sub_order_id") val subOrderId: String? = null,
    val status: String? = null,
    @SerialName("stock_released") val stockReleased: Boolean? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

class OrderRepositoryImpl(
    private val postgrest: Postgrest? = null,
    private val recalculateOrderUseCase: RecalculateOrderUseCase = RecalculateOrderUseCase()
) : OrderRepository {

    private val _ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    val ordersFlow = _ordersFlow.asStateFlow()

    private val productNameCache = ConcurrentHashMap<String, String>()
    private val profileNameCache = ConcurrentHashMap<String, String>()
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    override fun clearCache() {
        _ordersFlow.value = emptyList()
    }

    override suspend fun placeOrder(order: Order): Result<Order> = withContext(Dispatchers.IO) {
        try {
            if (order.subOrders.isEmpty()) {
                return@withContext Result.failure(Exception("El pedido no contiene ningún producto."))
            }

            if (postgrest != null) {

                val subordersArray = buildJsonArray {
                    for (sub in order.subOrders) {
                        val subId = if (isValidUUID(sub.id)) sub.id else UUID.randomUUID().toString()
                        add(buildJsonObject {
                            put("id", subId)
                            put("seller_id", sub.sellerId)
                            put("items", buildJsonArray {
                                for (item in sub.items) {
                                    val itemId = if (isValidUUID(item.id)) item.id else UUID.randomUUID().toString()
                                    val prodId = if (isValidUUID(item.productId)) item.productId else UUID.randomUUID().toString()
                                    add(buildJsonObject {
                                        put("id", itemId)
                                        put("product_id", prodId)
                                        put("quantity", item.quantity)
                                        put("unit_price", item.unitPrice)
                                    })
                                }
                            })
                        })
                    }
                }

                val orderId = if (isValidUUID(order.id)) order.id else UUID.randomUUID().toString()
                val paymentMethodName = order.subOrders.firstOrNull()?.paymentMethod?.name ?: PaymentMethod.EFECTIVO.name

                val rpcResult = postgrest.rpc(
                    function = "checkout_order_atomic",
                    parameters = buildJsonObject {
                        put("p_order_id", orderId)
                        put("p_meeting_point_id", order.meetingPointId)
                        put("p_meeting_point_name", order.meetingPointName)
                        put("p_scheduled_time", order.scheduledTime)
                        put("p_payment_method", paymentMethodName)
                        put("p_notes", order.notes)
                        put("p_suborders", subordersArray)
                    }
                )
                val response = jsonParser.decodeFromString<CheckoutResponseDto>(rpcResult.data)

                if (!response.success) {
                    return@withContext Result.failure(Exception(response.message ?: "No se pudo confirmar el pedido en el servidor."))
                }

                val confirmedOrder = order.copy(
                    id = response.orderId ?: order.id,
                    totalAmount = response.totalAmount ?: order.totalAmount,
                    status = OrderStatus.PENDIENTE
                )

                val currentList = _ordersFlow.value.toMutableList()
                currentList.removeAll { it.id == confirmedOrder.id }
                currentList.add(0, confirmedOrder)
                _ordersFlow.value = currentList

                Result.success(confirmedOrder)
            } else {
                val currentList = _ordersFlow.value.toMutableList()
                currentList.removeAll { it.id == order.id }
                currentList.add(0, order)
                _ordersFlow.value = currentList
                Result.success(order)
            }
        } catch (e: Exception) {
            val friendlyMsg = mapExceptionToUserFriendlyMessage(e)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    private fun mapExceptionToUserFriendlyMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("INSUFFICIENT_STOCK", ignoreCase = true) -> {
                val detail = msg.substringAfter("INSUFFICIENT_STOCK:").substringBefore("\n").trim()
                if (detail.isNotBlank()) detail else "Stock insuficiente para uno de los productos seleccionados."
            }
            msg.contains("SELLER_CLOSED", ignoreCase = true) -> {
                "Uno de los puestos del carrito no está aceptando pedidos en este momento."
            }
            msg.contains("SELF_PURCHASE", ignoreCase = true) -> {
                "No puedes realizar un pedido a tu propio emprendimiento."
            }
            msg.contains("PRODUCT_UNAVAILABLE", ignoreCase = true) -> {
                "Uno de los productos seleccionados ya no se encuentra disponible."
            }
            msg.contains("UNAUTHENTICATED", ignoreCase = true) -> {
                "Sesión no autenticada. Por favor vuelve a iniciar sesión."
            }
            msg.contains("INVALID_DATA", ignoreCase = true) -> {
                msg.substringAfter("INVALID_DATA:").substringBefore("\n").trim()
            }
            else -> e.localizedMessage ?: "Error de conexión al confirmar el pedido. Por favor reintenta."
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
                        .sortedByDescending { it.createdAt }

                    if (remoteOrders.isNotEmpty()) {
                        val orderIds = remoteOrders.map { it.id }

                        val remoteSubOrders = try {
                            postgrest.from("sub_orders")
                                .select {
                                    filter {
                                        isIn("order_id", orderIds)
                                    }
                                }
                                .decodeList<RemoteSubOrderDto>()
                        } catch (_: Exception) {
                            emptyList()
                        }

                        val remoteItems = try {
                            postgrest.from("order_items")
                                .select {
                                    filter {
                                        isIn("order_id", orderIds)
                                    }
                                }
                                .decodeList<RemoteOrderItemDto>()
                        } catch (_: Exception) {
                            emptyList()
                        }

                        val missingProdIds = remoteItems.map { it.productId }
                            .filter { isValidUUID(it) && !productNameCache.containsKey(it) }
                            .distinct()
                        if (missingProdIds.isNotEmpty()) {
                            try {
                                val prods = postgrest.from("products")
                                    .select { filter { isIn("id", missingProdIds) } }
                                    .decodeList<ProductBasicDto>()
                                for (p in prods) {
                                    productNameCache[p.id] = p.name
                                }
                            } catch (_: Exception) {}
                        }

                        val missingSellerIds = remoteSubOrders.map { it.sellerId }
                            .filter { isValidUUID(it) && !profileNameCache.containsKey(it) }
                            .distinct()
                        if (missingSellerIds.isNotEmpty()) {
                            try {
                                val profiles = postgrest.from("profiles")
                                    .select { filter { isIn("id", missingSellerIds) } }
                                    .decodeList<ProfileBasicDto>()
                                for (pr in profiles) {
                                    profileNameCache[pr.id] = pr.fullName ?: "Emprendedor"
                                }
                            } catch (_: Exception) {}
                        }

                        val itemsBySubOrder = remoteItems.groupBy { it.subOrderId ?: it.orderId }
                        val subOrdersByOrder = remoteSubOrders.groupBy { it.orderId }

                        val mappedOrders = remoteOrders.map { ro ->
                            val subsForOrder = subOrdersByOrder[ro.id] ?: emptyList()
                            val domainSubOrders = if (subsForOrder.isNotEmpty()) {
                                subsForOrder.map { rso ->
                                    val sItems = (itemsBySubOrder[rso.id] ?: emptyList()).map { oi ->
                                        SubOrderItem(
                                            id = oi.id,
                                            subOrderId = rso.id,
                                            productId = oi.productId,
                                            productName = productNameCache[oi.productId] ?: "Producto",
                                            quantity = oi.quantity,
                                            unitPrice = oi.priceAtSale,
                                            subtotal = oi.priceAtSale * oi.quantity
                                        )
                                    }
                                    val subStatus = mapRemoteStatusToSubOrderStatus(rso.status)
                                    SubOrder(
                                        id = rso.id,
                                        orderId = ro.id,
                                        sellerId = rso.sellerId,
                                        sellerName = profileNameCache[rso.sellerId] ?: "Emprendimiento",
                                        items = sItems,
                                        subtotalAmount = rso.subtotalAmount,
                                        status = subStatus,
                                        rejectionReason = rso.rejectionReason,
                                        paymentMethod = parsePaymentMethod(rso.paymentMethod),
                                        isPaymentConfirmed = rso.isPaymentConfirmed,
                                        isDeliveryConfirmed = rso.isDeliveryConfirmed,
                                        createdAt = rso.createdAt,
                                        updatedAt = rso.updatedAt
                                    )
                                }
                            } else {
                                val legacyItems = (itemsBySubOrder[ro.id] ?: emptyList()).map { oi ->
                                    SubOrderItem(
                                        id = oi.id,
                                        subOrderId = ro.id,
                                        productId = oi.productId,
                                        productName = productNameCache[oi.productId] ?: "Producto",
                                        quantity = oi.quantity,
                                        unitPrice = oi.priceAtSale,
                                        subtotal = oi.priceAtSale * oi.quantity
                                    )
                                }
                                val subStatus = mapRemoteStatusToSubOrderStatus(ro.status)
                                listOf(
                                    SubOrder(
                                        id = ro.id,
                                        orderId = ro.id,
                                        sellerId = ro.sellerId ?: "",
                                        sellerName = ro.sellerId?.let { profileNameCache[it] } ?: "Emprendimiento",
                                        items = legacyItems,
                                        subtotalAmount = ro.totalPrice,
                                        status = subStatus,
                                        paymentMethod = parsePaymentMethod(ro.paymentMethod),
                                        isPaymentConfirmed = (subStatus == SubOrderStatus.COMPLETADO),
                                        isDeliveryConfirmed = (subStatus == SubOrderStatus.COMPLETADO),
                                        createdAt = ro.createdAt,
                                        updatedAt = ro.updatedAt
                                    )
                                )
                            }

                            val orderStatus = mapRemoteStatusToOrderStatus(ro.status)
                            val meetingPlace = ro.meetingPointName ?: ro.deliveryPlace ?: "Campus Universitario"
                            val schedule = ro.scheduledTime ?: extractScheduleFromDeliveryPlace(ro.deliveryPlace)

                            Order(
                                id = ro.id,
                                buyerId = ro.buyerId,
                                buyerName = "Comprador",
                                meetingPointId = ro.meetingPointId ?: "mp-default",
                                meetingPointName = meetingPlace,
                                scheduledTime = schedule,
                                totalAmount = ro.totalPrice,
                                status = orderStatus,
                                subOrders = domainSubOrders,
                                notes = ro.notes,
                                createdAt = ro.createdAt,
                                updatedAt = ro.updatedAt
                            )
                        }

                        _ordersFlow.value = mappedOrders
                        emit(mappedOrders)
                    } else {
                        emit(emptyList())
                    }
                }
            } catch (_: Exception) {
            }

            delay(3000)
        }
    }.flowOn(Dispatchers.IO)

    override fun observeSubOrdersForSeller(sellerId: String): Flow<List<SubOrder>> = flow {
        emit(_ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId })

        while (true) {
            try {
                if (postgrest != null && isValidUUID(sellerId)) {
                    val remoteSubOrders = try {
                        postgrest.from("sub_orders")
                            .select {
                                filter {
                                    eq("seller_id", sellerId)
                                }
                            }
                            .decodeList<RemoteSubOrderDto>()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val remoteLegacyOrders = try {
                        postgrest.from("orders")
                            .select {
                                filter {
                                    eq("seller_id", sellerId)
                                }
                            }
                            .decodeList<RemoteOrderDto>()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val existingSubOrderOrderIds = remoteSubOrders.map { it.orderId }.toSet()
                    val missingFromSubOrders = remoteLegacyOrders.filter { it.id !in existingSubOrderOrderIds }
                    val synthesizedSubs = missingFromSubOrders.map { ro ->
                        RemoteSubOrderDto(
                            id = ro.id,
                            orderId = ro.id,
                            sellerId = ro.sellerId ?: sellerId,
                            subtotalAmount = ro.totalPrice,
                            status = ro.status,
                            paymentMethod = ro.paymentMethod,
                            createdAt = ro.createdAt,
                            updatedAt = ro.updatedAt
                        )
                    }

                    val combinedSubOrders = (remoteSubOrders + synthesizedSubs).sortedByDescending { it.createdAt }

                    if (combinedSubOrders.isNotEmpty()) {
                        val parentOrderIds = combinedSubOrders.map { it.orderId }.distinct()

                        val remoteItems = try {
                            postgrest.from("order_items")
                                .select { filter { isIn("order_id", parentOrderIds) } }
                                .decodeList<RemoteOrderItemDto>()
                        } catch (_: Exception) {
                            emptyList()
                        }

                        val missingProdIds = remoteItems.map { it.productId }
                            .filter { isValidUUID(it) && !productNameCache.containsKey(it) }
                            .distinct()
                        if (missingProdIds.isNotEmpty()) {
                            try {
                                val prods = postgrest.from("products")
                                    .select { filter { isIn("id", missingProdIds) } }
                                    .decodeList<ProductBasicDto>()
                                for (p in prods) {
                                    productNameCache[p.id] = p.name
                                }
                            } catch (_: Exception) {}
                        }

                        if (!profileNameCache.containsKey(sellerId)) {
                            try {
                                val pr = postgrest.from("profiles")
                                    .select { filter { eq("id", sellerId) } }
                                    .decodeSingleOrNull<ProfileBasicDto>()
                                if (pr != null) {
                                    profileNameCache[sellerId] = pr.fullName ?: "Mi Emprendimiento"
                                }
                            } catch (_: Exception) {}
                        }

                        val itemsBySubOrder = remoteItems.groupBy { it.subOrderId ?: it.orderId }

                        val mappedSubOrders = combinedSubOrders.map { rso ->
                            val sItems = (itemsBySubOrder[rso.id] ?: itemsBySubOrder[rso.orderId] ?: emptyList()).map { oi ->
                                SubOrderItem(
                                    id = oi.id,
                                    subOrderId = rso.id,
                                    productId = oi.productId,
                                    productName = productNameCache[oi.productId] ?: "Producto",
                                    quantity = oi.quantity,
                                    unitPrice = oi.priceAtSale,
                                    subtotal = oi.priceAtSale * oi.quantity
                                )
                            }
                            val subStatus = mapRemoteStatusToSubOrderStatus(rso.status)
                            SubOrder(
                                id = rso.id,
                                orderId = rso.orderId,
                                sellerId = rso.sellerId,
                                sellerName = profileNameCache[rso.sellerId] ?: "Mi Emprendimiento",
                                items = sItems,
                                subtotalAmount = rso.subtotalAmount,
                                status = subStatus,
                                rejectionReason = rso.rejectionReason,
                                paymentMethod = parsePaymentMethod(rso.paymentMethod),
                                isPaymentConfirmed = rso.isPaymentConfirmed,
                                isDeliveryConfirmed = rso.isDeliveryConfirmed,
                                createdAt = rso.createdAt,
                                updatedAt = rso.updatedAt
                            )
                        }

                        emit(mappedSubOrders)
                    } else {
                        emit(emptyList())
                    }
                }
            } catch (_: Exception) {
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
            if (postgrest != null && isValidUUID(subOrderId)) {
                val remoteStatusStr = mapLocalStatusToRemote(newStatus)

                val rpcResult = postgrest.rpc(
                    function = "update_suborder_status_atomic",
                    parameters = buildJsonObject {
                        put("p_sub_order_id", subOrderId)
                        put("p_new_status", remoteStatusStr)
                        if (rejectionReason != null) {
                            put("p_rejection_reason", rejectionReason)
                        }
                    }
                )
                val response = jsonParser.decodeFromString<UpdateSuborderStatusResponseDto>(rpcResult.data)

                if (!response.success) {
                    return@withContext Result.failure(Exception("No se pudo actualizar el estado del subpedido en el servidor."))
                }
            }

            var updated: SubOrder? = null
            val currentOrders = _ordersFlow.value.toMutableList()
            for (i in currentOrders.indices) {
                val order = currentOrders[i]
                val subIndex = order.subOrders.indexOfFirst { it.id == subOrderId }
                if (subIndex >= 0) {
                    val curSub = order.subOrders[subIndex]
                    val isPayConfirmed = if (newStatus == SubOrderStatus.COMPLETADO || newStatus == SubOrderStatus.PAGO_CONFIRMADO) true else curSub.isPaymentConfirmed
                    val isDelivConfirmed = if (newStatus == SubOrderStatus.COMPLETADO) true else curSub.isDeliveryConfirmed
                    val newSub = curSub.copy(
                        status = newStatus,
                        rejectionReason = rejectionReason ?: curSub.rejectionReason,
                        isPaymentConfirmed = isPayConfirmed,
                        isDeliveryConfirmed = isDelivConfirmed
                    )
                    val recalculated = recalculateOrderUseCase(order, newSub)
                    currentOrders[i] = recalculated
                    updated = newSub
                    break
                }
            }

            if (updated != null) {
                _ordersFlow.value = currentOrders
                Result.success(updated)
            } else {
                val fallbackSub = SubOrder(
                    id = subOrderId,
                    orderId = subOrderId,
                    sellerId = "",
                    sellerName = "Subpedido",
                    status = newStatus,
                    rejectionReason = rejectionReason
                )
                Result.success(fallbackSub)
            }
        } catch (e: Exception) {
            val friendlyMsg = when {
                e.message.orEmpty().contains("INVALID_TRANSITION", ignoreCase = true) -> {
                    e.message?.substringAfter("INVALID_TRANSITION:")?.substringBefore("\n")?.trim() ?: "Transición de estado no permitida."
                }
                e.message.orEmpty().contains("FORBIDDEN", ignoreCase = true) -> {
                    "No tienes permisos para modificar este subpedido."
                }
                else -> e.localizedMessage ?: "Error al actualizar estado del subpedido."
            }
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    private fun mapRemoteStatusToSubOrderStatus(status: String): SubOrderStatus = when (status.lowercase()) {
        "pending" -> SubOrderStatus.PENDIENTE
        "accepted" -> SubOrderStatus.ACEPTADO
        "preparing" -> SubOrderStatus.EN_PREPARACION
        "ready", "waiting_delivery" -> SubOrderStatus.LISTO
        "completed", "payment_confirmed" -> SubOrderStatus.COMPLETADO
        "rejected" -> SubOrderStatus.RECHAZADO
        "cancelled" -> SubOrderStatus.CANCELADO
        "not_delivered" -> SubOrderStatus.NO_ENTREGADO
        else -> SubOrderStatus.PENDIENTE
    }

    private fun mapLocalStatusToRemote(status: SubOrderStatus): String = when (status) {
        SubOrderStatus.PENDIENTE -> "pending"
        SubOrderStatus.ACEPTADO -> "accepted"
        SubOrderStatus.EN_PREPARACION -> "preparing"
        SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> "ready"
        SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO -> "completed"
        SubOrderStatus.RECHAZADO -> "rejected"
        SubOrderStatus.CANCELADO -> "cancelled"
        SubOrderStatus.NO_ENTREGADO -> "not_delivered"
    }

    private fun mapRemoteStatusToOrderStatus(status: String): OrderStatus = when (status.lowercase()) {
        "pending" -> OrderStatus.PENDIENTE
        "accepted", "preparing", "ready", "in_progress" -> OrderStatus.EN_PROCESO
        "partially_accepted" -> OrderStatus.PARCIALMENTE_ACEPTADA
        "completed" -> OrderStatus.COMPLETADA
        "cancelled", "rejected" -> OrderStatus.CANCELADA
        else -> OrderStatus.PENDIENTE
    }

    private fun parsePaymentMethod(method: String?): PaymentMethod = when (method?.uppercase()) {
        "YAPE" -> PaymentMethod.YAPE
        "PLIN" -> PaymentMethod.PLIN
        "TRANSFERENCIA" -> PaymentMethod.TRANSFERENCIA
        "OTRO" -> PaymentMethod.OTRO
        else -> PaymentMethod.EFECTIVO
    }

    private fun extractScheduleFromDeliveryPlace(deliveryPlace: String?): String {
        if (deliveryPlace == null) return "Turno seleccionado"
        val openParen = deliveryPlace.indexOf('(')
        val closeParen = deliveryPlace.indexOf(')')
        return if (openParen in 0 until closeParen) {
            deliveryPlace.substring(openParen + 1, closeParen).trim()
        } else "Turno seleccionado"
    }

    private fun isValidUUID(value: String): Boolean = try {
        UUID.fromString(value)
        true
    } catch (_: Exception) {
        false
    }
}
