package com.example.vallego.features.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    buyerProfile: UserProfile,
    onNavigateBack: () -> Unit,
    onNavigateToTracking: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialogo de Éxito cuando se genera la orden y sus subpedidos
    if (uiState.placedOrder != null) {
        val order = uiState.placedOrder!!
        AlertDialog(
            onDismissRequest = {
                viewModel.clearPlacedOrder()
                onNavigateBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "¡Pedido Valle-Go Confirmado!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tu orden ha sido dividida automáticamente por cada emprendimiento involucrado:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider()
                    Text(
                        text = "📍 Punto: ${order.meetingPointName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "⏰ Hora: ${order.scheduledTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subpedidos independientes:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    order.subOrders.forEachIndexed { index, subOrder ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${index + 1}. ${subOrder.sellerName}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${subOrder.items.size} producto(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "S/ %.2f".format(subOrder.subtotalAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF003366)
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL GENERAL:", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "S/ %.2f".format(order.totalAmount),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF003366)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPlacedOrder()
                        onNavigateToTracking()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                ) {
                    Text("Ver Seguimiento")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clearPlacedOrder()
                        onNavigateBack()
                    }
                ) {
                    Text("Seguir Comprando")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Carrito Multi-Puesto",
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
        if (uiState.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tu carrito está vacío",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explora los puestos de tu campus y agrega tus antojos favoritos en una sola compra.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                    ) {
                        Text("Explorar Puestos")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Grupos por Emprendimiento
                uiState.calculation.storeGroups.forEach { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cabecera del puesto
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏪 ${group.sellerName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF003366)
                                )
                                Text(
                                    text = "Subtotal: S/ %.2f".format(group.subtotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFC8102E)
                                )
                            }

                            HorizontalDivider()

                            // Lista de productos del puesto
                            group.items.forEach { cartItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cartItem.product.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "S/ %.2f c/u".format(cartItem.product.price),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Stepper cantidad (- 1 +)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.decrementItem(cartItem.product.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (cartItem.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                                contentDescription = "Disminuir",
                                                tint = if (cartItem.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = "${cartItem.quantity}",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        IconButton(
                                            onClick = { viewModel.incrementItem(cartItem.product.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Aumentar"
                                            )
                                        }

                                        Text(
                                            text = "S/ %.2f".format(cartItem.subtotal),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(64.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sección Checkout: Punto de Encuentro
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFC8102E)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Punto de Encuentro en Campus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Dropdown de Puntos de Encuentro
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedMeetingPoint?.name ?: "Selecciona un punto",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                uiState.meetingPoints.forEach { point ->
                                    DropdownMenuItem(
                                        text = { Text("${point.name} (${point.pavilion})") },
                                        onClick = {
                                            viewModel.selectMeetingPoint(point)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Horario de Entrega (Intervalos de 30 min)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF003366)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Horario de Encuentro (30 min)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.availableTimeSlots.forEach { slot ->
                                FilterChip(
                                    selected = uiState.selectedTimeSlot == slot,
                                    onClick = { viewModel.selectTimeSlot(slot) },
                                    label = { Text(slot) }
                                )
                            }
                        }

                        // Método de Pago contra entrega
                        Text(
                            text = "Método de Pago (Contra entrega)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.selectedPaymentMethod == PaymentMethod.YAPE,
                                onClick = { viewModel.selectPaymentMethod(PaymentMethod.YAPE) },
                                label = { Text("Yape") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.selectedPaymentMethod == PaymentMethod.PLIN,
                                onClick = { viewModel.selectPaymentMethod(PaymentMethod.PLIN) },
                                label = { Text("Plin") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.selectedPaymentMethod == PaymentMethod.EFECTIVO,
                                onClick = { viewModel.selectPaymentMethod(PaymentMethod.EFECTIVO) },
                                label = { Text("Efectivo") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Indicaciones opcionales
                        OutlinedTextField(
                            value = uiState.orderNotes,
                            onValueChange = viewModel::onNotesChange,
                            label = { Text("Notas para los vendedores (opcional)") },
                            placeholder = { Text("Ej: Estoy con casaca azul cerca a la puerta") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Resumen y Botón de Confirmación
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF003366))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total General (${uiState.calculation.totalItemCount} ítems):",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "S/ %.2f".format(uiState.calculation.grandTotal),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Button(
                            onClick = { viewModel.confirmOrder(buyerProfile) },
                            enabled = uiState.canCheckout,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "Confirmar Pedido Valle-Go",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
