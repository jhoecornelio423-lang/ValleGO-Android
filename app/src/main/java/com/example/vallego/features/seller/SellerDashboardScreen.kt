package com.example.vallego.features.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SellerDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(profile.id) {
        viewModel.initialize(profile.id, profile.acceptingOrders)
    }

    // Modal de Rechazo de Subpedido
    if (uiState.selectedSubOrderForRejection != null) {
        val subOrder = uiState.selectedSubOrderForRejection!!
        var selectedReason by remember { mutableStateOf("Sin insumos / agotado") }
        var customReason by remember { mutableStateOf("") }

        val commonReasons = listOf(
            "Sin insumos / agotado",
            "Puesto cerrado por clase / horario",
            "Tiempo de espera muy alto",
            "Otro motivo"
        )

        AlertDialog(
            onDismissRequest = { viewModel.dismissRejectionDialog() },
            title = {
                Text("Rechazar Subpedido #${subOrder.id.take(6).uppercase()}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "El comprador serÃ¡ notificado y su orden total se recalcularÃ¡ automÃ¡ticamente restando este importe.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    commonReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (selectedReason == "Otro motivo") {
                        OutlinedTextField(
                            value = customReason,
                            onValueChange = { customReason = it },
                            label = { Text("Escribe el motivo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = if (selectedReason == "Otro motivo") {
                            customReason.ifBlank { "Cancelado por el puesto" }
                        } else {
                            selectedReason
                        }
                        viewModel.confirmRejection(subOrder.id, finalReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text("Confirmar Rechazo")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRejectionDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de ConfirmaciÃ³n de Entrega y Cobro
    if (uiState.selectedSubOrderForDelivery != null) {
        val subOrder = uiState.selectedSubOrderForDelivery!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeliveryDialog() },
            title = {
                Text("Confirmar Entrega y Cobro", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Â¿Confirmas que entregaste el pedido al estudiante y recibiste el pago contra entrega?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Subpedido #${subOrder.id.take(6).uppercase()}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Monto a cobrar: S/ %.2f".format(subOrder.subtotalAmount),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF003366),
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Medio acordado: ${subOrder.paymentMethod?.name ?: "Efectivo / Yape"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeliveryAndPayment(subOrder.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirmar Cobro y Entrega")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeliveryDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = profile.businessLocation ?: "Panel Emprendedor",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                        Text(
                            text = profile.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesiÃ³n")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Estado del Puesto (Abierto/Cerrado)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isAcceptingOrders) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (uiState.isAcceptingOrders) "Puesto Abierto" else "Puesto Cerrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isAcceptingOrders) Color(0xFF2E7D32) else Color(0xFFC8102E)
                        )
                        Text(
                            text = if (uiState.isAcceptingOrders) "Aceptando subpedidos en campus" else "No visible en catÃ¡logo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isAcceptingOrders,
                        onCheckedChange = { viewModel.toggleAcceptingOrders(it) }
                    )
                }
            }

            // Resumen de MÃ©tricas / KPIs del DÃ­a
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSummaryCard(
                    title = "Ganancias",
                    value = "S/ %.2f".format(uiState.earningsToday),
                    color = Color(0xFF003366),
                    modifier = Modifier.weight(1.3f)
                )
                MetricSummaryCard(
                    title = "Pendientes",
                    value = "${uiState.pendingCount}",
                    color = Color(0xFFF57C00),
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "En prep.",
                    value = "${uiState.inPreparationCount}",
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Listos",
                    value = "${uiState.readyCount}",
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
            }

            // Filtros de Estado en Chips Horizontales
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SellerOrderFilter.values().forEach { filter ->
                    val isSelected = uiState.selectedFilter == filter
                    val label = when (filter) {
                        SellerOrderFilter.TODOS -> "Todos (${uiState.totalSubOrdersToday})"
                        SellerOrderFilter.PENDIENTES -> "Pendientes (${uiState.pendingCount})"
                        SellerOrderFilter.EN_PREPARACION -> "En Prep. (${uiState.inPreparationCount})"
                        SellerOrderFilter.LISTOS -> "Listos (${uiState.readyCount})"
                        SellerOrderFilter.COMPLETADOS -> "Entregados (${uiState.completedCount})"
                        SellerOrderFilter.RECHAZADOS -> "Rechazados"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF003366),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Lista de Subpedidos
            val subOrders = uiState.filteredSubOrders
            if (subOrders.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay subpedidos en esta secciÃ³n",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subOrders, key = { it.id }) { subOrder ->
                        SellerSubOrderCard(
                            subOrder = subOrder,
                            onAccept = { viewModel.acceptSubOrder(subOrder.id) },
                            onStartPrep = { viewModel.startPreparation(subOrder.id) },
                            onMarkReady = { viewModel.markReady(subOrder.id) },
                            onOpenDelivery = { viewModel.openDeliveryDialog(subOrder) },
                            onOpenRejection = { viewModel.openRejectionDialog(subOrder) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SellerSubOrderCard(
    subOrder: SubOrder,
    onAccept: () -> Unit,
    onStartPrep: () -> Unit,
    onMarkReady: () -> Unit,
    onOpenDelivery: () -> Unit,
    onOpenRejection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: ID del subpedido y Badge de Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subpedido #${subOrder.id.take(6).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Total: S/ %.2f".format(subOrder.subtotalAmount),
                        color = Color(0xFF003366),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                StatusBadge(status = subOrder.status)
            }

            HorizontalDivider()

            // Lista de Ãtems del subpedido
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                subOrder.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.productName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "S/ %.2f".format(item.subtotal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Medio de pago y Botones de AcciÃ³n segÃºn el estado actual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFF003366),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = subOrder.paymentMethod?.name ?: "Contra Entrega",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                    }
                }

                when (subOrder.status) {
                    SubOrderStatus.PENDIENTE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenRejection,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Rechazar")
                            }
                            Button(
                                onClick = onAccept,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Aceptar")
                            }
                        }
                    }
                    SubOrderStatus.ACEPTADO -> {
                        Button(
                            onClick = onStartPrep,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Iniciar PreparaciÃ³n")
                        }
                    }
                    SubOrderStatus.EN_PREPARACION -> {
                        Button(
                            onClick = onMarkReady,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Marcar Listo")
                        }
                    }
                    SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> {
                        Button(
                            onClick = onOpenDelivery,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Confirmar Entrega y Cobro")
                        }
                    }
                    SubOrderStatus.COMPLETADO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Entregado y Cobrado âœ“",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    SubOrderStatus.RECHAZADO -> {
                        Text(
                            text = "Rechazado: ${subOrder.rejectionReason ?: "Sin motivo"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8102E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {
                        Text(
                            text = subOrder.status.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: SubOrderStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        SubOrderStatus.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        SubOrderStatus.ACEPTADO -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "Aceptado")
        SubOrderStatus.EN_PREPARACION -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "En PreparaciÃ³n")
        SubOrderStatus.LISTO -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Listo para Entrega")
        SubOrderStatus.ESPERANDO_ENTREGA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Esperando Entrega")
        SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), "Completado")
        SubOrderStatus.RECHAZADO -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Rechazado")
        SubOrderStatus.CANCELADO -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Cancelado")
        SubOrderStatus.NO_ENTREGADO -> Triple(Color(0xFFECEFF1), Color(0xFF455A64), "No entregado")
    }

    Surface(
        color = backgroundColor,
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