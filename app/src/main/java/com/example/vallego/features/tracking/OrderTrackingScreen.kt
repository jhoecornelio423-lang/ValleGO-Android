package com.example.vallego.features.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.OrderStatus
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    buyerProfile: UserProfile,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderTrackingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(buyerProfile.id) {
        viewModel.initialize(buyerProfile.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Seguimiento en Vivo",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF003366)
                )
            } else if (uiState.orders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aún no tienes pedidos en curso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Agrega productos de tus puestos favoritos y realiza tu primer pedido multi-emprendimiento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.orders, key = { it.id }) { order ->
                        BuyerOrderCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
fun BuyerOrderCard(order: Order, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabecera: ID y Estado General
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (order.status == OrderStatus.COMPLETADA) "Pedido Entregado" else "Pedido en Curso",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Total a pagar: S/ %.2f".format(order.totalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF003366),
                        fontSize = 18.sp
                    )
                }
                OrderStatusBadge(status = order.status)
            }

            // Datos de Entrega
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFC8102E), modifier = Modifier.size(16.dp))
                        Text(order.meetingPointName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF003366), modifier = Modifier.size(16.dp))
                        Text(order.scheduledTime, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Alerta si la orden fue Parcialmente Aceptada por rechazo de algún puesto
            if (order.status == OrderStatus.PARCIALMENTE_ACEPTADA) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Un puesto no pudo atender su parte. El total se recalculó automáticamente y no pagarás por los ítems cancelados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Desglose de Subpedidos en Vivo
            Text(
                text = "Puestos participantes (${order.subOrders.size}):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            order.subOrders.forEach { subOrder ->
                SubOrderTrackingItem(subOrder = subOrder)
            }
        }
    }
}

@Composable
fun SubOrderTrackingItem(subOrder: SubOrder) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subOrder.sellerName.ifEmpty { "Emprendimiento Valle-Go" },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "S/ %.2f".format(subOrder.subtotalAmount),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003366)
                )
            }

            // Lista compacta de productos
            subOrder.items.forEach { item ->
                Text(
                    text = "• ${item.quantity}x ${item.productName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stepper / Estado en vivo del subpedido
            if (subOrder.status == SubOrderStatus.RECHAZADO || subOrder.status == SubOrderStatus.CANCELADO) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFC8102E), modifier = Modifier.size(16.dp))
                        Text(
                            text = "No disponible: ${subOrder.rejectionReason ?: "Sin insumos"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8102E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                TrackingStepper(status = subOrder.status)
            }
        }
    }
}

@Composable
fun TrackingStepper(status: SubOrderStatus) {
    val step1Done = true // Solicitado
    val step2Done = status in listOf(SubOrderStatus.ACEPTADO, SubOrderStatus.EN_PREPARACION, SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA, SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO)
    val step3Done = status in listOf(SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA, SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO)
    val step4Done = status in listOf(SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepCircle(label = "Enviado", isDone = step1Done, isActive = status == SubOrderStatus.PENDIENTE)
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step2Done) Color(0xFF2E7D32) else Color.LightGray)
        StepCircle(label = "Preparando", isDone = step2Done, isActive = status == SubOrderStatus.ACEPTADO || status == SubOrderStatus.EN_PREPARACION)
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step3Done) Color(0xFF2E7D32) else Color.LightGray)
        StepCircle(label = "Listo", isDone = step3Done, isActive = status == SubOrderStatus.LISTO || status == SubOrderStatus.ESPERANDO_ENTREGA)
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step4Done) Color(0xFF2E7D32) else Color.LightGray)
        StepCircle(label = "Entregado", isDone = step4Done, isActive = step4Done)
    }
}

@Composable
fun StepCircle(label: String, isDone: Boolean, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> Color(0xFF2E7D32)
                        isActive -> Color(0xFF003366)
                        else -> Color(0xFFE2E8F0)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive || isDone) Color(0xFF003366) else Color.Gray
        )
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (bgColor, textColor, label) = when (status) {
        OrderStatus.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        OrderStatus.EN_PROCESO -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "En Proceso")
        OrderStatus.PARCIALMENTE_ACEPTADA -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "Parcialmente Aceptada")
        OrderStatus.COMPLETADA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Completada")
        OrderStatus.CANCELADA -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Cancelada")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}