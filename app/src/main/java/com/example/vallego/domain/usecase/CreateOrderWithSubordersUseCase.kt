package com.example.vallego.domain.usecase

import com.example.vallego.domain.model.*
import java.util.UUID

class CreateOrderWithSubordersUseCase {
    operator fun invoke(
        buyerProfile: UserProfile,
        meetingPoint: CampusMeetingPoint,
        scheduledTime: String,
        paymentMethod: PaymentMethod,
        cartResult: CartCalculationResult,
        notes: String? = null
    ): Order {
        val orderId = UUID.randomUUID().toString()
        val subOrders = cartResult.storeGroups.map { group ->
            val subOrderId = UUID.randomUUID().toString()
            val subOrderItems = group.items.map { cartItem ->
                SubOrderItem(
                    id = UUID.randomUUID().toString(),
                    subOrderId = subOrderId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    unitPrice = cartItem.product.price,
                    quantity = cartItem.quantity,
                    subtotal = cartItem.subtotal
                )
            }
            SubOrder(
                id = subOrderId,
                orderId = orderId,
                sellerId = group.sellerId,
                sellerName = group.sellerName,
                items = subOrderItems,
                subtotalAmount = group.subtotal,
                status = SubOrderStatus.PENDIENTE,
                paymentMethod = paymentMethod
            )
        }
        return Order(
            id = orderId,
            buyerId = buyerProfile.id,
            buyerName = buyerProfile.fullName,
            meetingPointId = meetingPoint.id,
            meetingPointName = meetingPoint.name,
            scheduledTime = scheduledTime,
            totalAmount = cartResult.grandTotal,
            status = OrderStatus.PENDIENTE,
            subOrders = subOrders,
            notes = notes
        )
    }
}