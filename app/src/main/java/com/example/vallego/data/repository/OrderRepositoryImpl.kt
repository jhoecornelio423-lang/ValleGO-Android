package com.example.vallego.data.repository

import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.usecase.RecalculateOrderUseCase
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OrderRepositoryImpl(
    private val postgrest: Postgrest? = null,
    private val recalculateOrderUseCase: RecalculateOrderUseCase = RecalculateOrderUseCase()
) : OrderRepository {

    private val _ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    val ordersFlow = _ordersFlow.asStateFlow()

    override suspend fun placeOrder(order: Order): Result<Order> = withContext(Dispatchers.IO) {
        try {
            try {
                if (postgrest != null) {
                    postgrest.from("orders").insert(order)
                    for (subOrder in order.subOrders) {
                        postgrest.from("sub_orders").insert(subOrder)
                    }
                }
            } catch (_: Exception) {
                // Si la tabla remota aún no cuenta con la migración en Supabase Cloud, el estado local persiste
            }
            val currentList = _ordersFlow.value.toMutableList()
            currentList.add(0, order)
            _ordersFlow.value = currentList
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
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

    override fun observeOrdersForBuyer(buyerId: String): Flow<List<Order>> {
        return _ordersFlow.map { orders ->
            orders.filter { it.buyerId == buyerId }
        }
    }

    override fun observeSubOrdersForSeller(sellerId: String): Flow<List<SubOrder>> {
        return _ordersFlow.map { orders ->
            orders.flatMap { it.subOrders }.filter { it.sellerId == sellerId }
        }
    }

    override suspend fun updateSubOrderStatus(
        subOrderId: String,
        newStatus: SubOrderStatus,
        rejectionReason: String?
    ): Result<SubOrder> = withContext(Dispatchers.IO) {
        try {
            var updated: SubOrder? = null
            val currentOrders = _ordersFlow.value.toMutableList()
            for (i in currentOrders.indices) {
                val order = currentOrders[i]
                val subOrderIndex = order.subOrders.indexOfFirst { it.id == subOrderId }
                if (subOrderIndex >= 0) {
                    val currentSub = order.subOrders[subOrderIndex]
                    val isPayConfirmed = if (newStatus == SubOrderStatus.COMPLETADO || newStatus == SubOrderStatus.PAGO_CONFIRMADO) true else currentSub.isPaymentConfirmed
                    val isDelivConfirmed = if (newStatus == SubOrderStatus.COMPLETADO) true else currentSub.isDeliveryConfirmed
                    val newSub = currentSub.copy(
                        status = newStatus,
                        rejectionReason = rejectionReason ?: currentSub.rejectionReason,
                        isPaymentConfirmed = isPayConfirmed,
                        isDeliveryConfirmed = isDelivConfirmed
                    )
                    // Recalcular orden padre de forma atómica
                    val recalculatedOrder = recalculateOrderUseCase(order, newSub)
                    currentOrders[i] = recalculatedOrder
                    updated = newSub
                    break
                }
            }
            if (updated != null) {
                _ordersFlow.value = currentOrders
                try {
                    if (postgrest != null) {
                        postgrest.from("sub_orders").update(
                            mapOf(
                                "status" to newStatus.name,
                                "rejection_reason" to rejectionReason
                            )
                        ) {
                            filter { eq("id", subOrderId) }
                        }
                    }
                } catch (_: Exception) {
                    // Offline fallback
                }
                Result.success(updated)
            } else {
                Result.failure(NoSuchElementException("Subpedido no encontrado: $subOrderId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
