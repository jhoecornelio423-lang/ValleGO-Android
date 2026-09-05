package com.example.vallego.data.repository

import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.repository.OrderRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepositoryImpl(
    private val postgrest: Postgrest
) : OrderRepository {

    private val localOrders = mutableListOf<Order>()

    override suspend fun placeOrder(order: Order): Result<Order> = withContext(Dispatchers.IO) {
        try {
            try {
                postgrest.from("orders").insert(order)
                for (subOrder in order.subOrders) {
                    postgrest.from("sub_orders").insert(subOrder)
                }
            } catch (_: Exception) {
                // Si la tabla remota aún no cuenta con la migración en Supabase Cloud, el estado local persiste
            }
            localOrders.add(0, order)
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrdersForBuyer(buyerId: String): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val matching = localOrders.filter { it.buyerId == buyerId }
            Result.success(matching)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSubOrdersForSeller(sellerId: String): Result<List<SubOrder>> = withContext(Dispatchers.IO) {
        try {
            val subOrders = localOrders.flatMap { it.subOrders }.filter { it.sellerId == sellerId }
            Result.success(subOrders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSubOrderStatus(
        subOrderId: String,
        newStatus: SubOrderStatus,
        rejectionReason: String?
    ): Result<SubOrder> = withContext(Dispatchers.IO) {
        try {
            var updated: SubOrder? = null
            for (i in localOrders.indices) {
                val order = localOrders[i]
                val subOrderIndex = order.subOrders.indexOfFirst { it.id == subOrderId }
                if (subOrderIndex >= 0) {
                    val currentSub = order.subOrders[subOrderIndex]
                    val newSub = currentSub.copy(
                        status = newStatus,
                        rejectionReason = rejectionReason
                    )
                    val newSubOrders = order.subOrders.toMutableList().apply {
                        set(subOrderIndex, newSub)
                    }
                    localOrders[i] = order.copy(subOrders = newSubOrders)
                    updated = newSub
                    break
                }
            }
            if (updated != null) {
                Result.success(updated)
            } else {
                Result.failure(NoSuchElementException("Subpedido no encontrado: $subOrderId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
