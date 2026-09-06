package com.example.vallego.domain.usecase

import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.OrderStatus
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus

class RecalculateOrderUseCase {
    operator fun invoke(currentOrder: Order, updatedSubOrder: SubOrder): Order {
        val newSubOrders = currentOrder.subOrders.map { subOrder ->
            if (subOrder.id == updatedSubOrder.id) updatedSubOrder else subOrder
        }

        val newTotalAmount = newSubOrders.sumOf { subOrder ->
            if (subOrder.status != SubOrderStatus.RECHAZADO && subOrder.status != SubOrderStatus.CANCELADO) {
                subOrder.subtotalAmount
            } else {
                0.0
            }
        }

        val newStatus = when {
            newSubOrders.isNotEmpty() && newSubOrders.all { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO } -> OrderStatus.CANCELADA
            newSubOrders.isNotEmpty() && newSubOrders.all { it.status == SubOrderStatus.COMPLETADO } -> OrderStatus.COMPLETADA
            newSubOrders.any { it.status == SubOrderStatus.RECHAZADO } && newSubOrders.any {
                it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION || it.status == SubOrderStatus.LISTO
            } -> OrderStatus.PARCIALMENTE_ACEPTADA
            newSubOrders.any { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION || it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.PAGO_CONFIRMADO } -> OrderStatus.EN_PROCESO
            else -> currentOrder.status
        }

        return currentOrder.copy(totalAmount = newTotalAmount, status = newStatus, subOrders = newSubOrders)
    }
}