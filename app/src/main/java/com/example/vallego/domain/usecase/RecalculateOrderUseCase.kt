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
            if (subOrder.status != SubOrderStatus.RECHAZADO && 
                subOrder.status != SubOrderStatus.CANCELADO && 
                subOrder.status != SubOrderStatus.NO_ENTREGADO
            ) {
                subOrder.subtotalAmount
            } else {
                0.0
            }
        }

        val allCancelledOrRejected = newSubOrders.isNotEmpty() && newSubOrders.all {
            it.status == SubOrderStatus.RECHAZADO || 
            it.status == SubOrderStatus.CANCELADO || 
            it.status == SubOrderStatus.NO_ENTREGADO
        }

        val activeSubOrders = newSubOrders.filter {
            it.status != SubOrderStatus.RECHAZADO && 
            it.status != SubOrderStatus.CANCELADO && 
            it.status != SubOrderStatus.NO_ENTREGADO
        }

        val hasAnyPendingOrInProgress = activeSubOrders.any {
            it.status == SubOrderStatus.PENDIENTE ||
            it.status == SubOrderStatus.ACEPTADO ||
            it.status == SubOrderStatus.EN_PREPARACION ||
            it.status == SubOrderStatus.LISTO ||
            it.status == SubOrderStatus.ESPERANDO_ENTREGA
        }

        val allActiveAreCompleted = activeSubOrders.isNotEmpty() && activeSubOrders.all {
            it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO
        }

        val hasRejected = newSubOrders.any { it.status == SubOrderStatus.RECHAZADO }

        val newStatus = when {
            allCancelledOrRejected -> OrderStatus.CANCELADA
            allActiveAreCompleted -> OrderStatus.COMPLETADA
            hasRejected && activeSubOrders.any { it.status != SubOrderStatus.PENDIENTE } -> OrderStatus.PARCIALMENTE_ACEPTADA
            hasAnyPendingOrInProgress -> {
                if (activeSubOrders.all { it.status == SubOrderStatus.PENDIENTE }) OrderStatus.PENDIENTE
                else OrderStatus.EN_PROCESO
            }
            else -> currentOrder.status
        }

        return currentOrder.copy(totalAmount = newTotalAmount, status = newStatus, subOrders = newSubOrders)
    }
}