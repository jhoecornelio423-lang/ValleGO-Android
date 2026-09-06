package com.example.vallego.domain.repository

import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus

import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun placeOrder(order: Order): Result<Order>
    suspend fun getOrdersForBuyer(buyerId: String): Result<List<Order>>
    suspend fun getSubOrdersForSeller(sellerId: String): Result<List<SubOrder>>
    fun observeOrdersForBuyer(buyerId: String): Flow<List<Order>>
    fun observeSubOrdersForSeller(sellerId: String): Flow<List<SubOrder>>
    suspend fun updateSubOrderStatus(subOrderId: String, newStatus: SubOrderStatus, rejectionReason: String? = null): Result<SubOrder>
    suspend fun cancelOrderByBuyer(orderId: String): Result<Unit>
    suspend fun markBuyerNoShow(subOrderId: String, reason: String? = null): Result<SubOrder>
    suspend fun expirePendingSuborders(): Result<Int>
    fun clearCache()
}
