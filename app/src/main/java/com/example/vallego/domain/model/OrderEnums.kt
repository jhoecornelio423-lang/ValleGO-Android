package com.example.vallego.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SubOrderStatus {
    PENDIENTE,
    ACEPTADO,
    EN_PREPARACION,
    LISTO,
    ESPERANDO_ENTREGA,
    PAGO_CONFIRMADO,
    COMPLETADO,
    RECHAZADO,
    CANCELADO,
    NO_ENTREGADO;

    val isFinal: Boolean
        get() = this == COMPLETADO || this == RECHAZADO || this == CANCELADO || this == NO_ENTREGADO

    val isTerminalCancelled: Boolean
        get() = this == CANCELADO || this == NO_ENTREGADO
}

@Serializable
enum class OrderStatus {
    PENDIENTE,
    PARCIALMENTE_ACEPTADA,
    EN_PROCESO,
    COMPLETADA,
    CANCELADA
}

@Serializable
enum class PaymentMethod {
    EFECTIVO,
    YAPE,
    PLIN,
    TRANSFERENCIA,
    OTRO
}