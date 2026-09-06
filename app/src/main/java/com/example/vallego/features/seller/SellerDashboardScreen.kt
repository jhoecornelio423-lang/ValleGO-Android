package com.example.vallego.features.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhotoCamera
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.vallego.ui.components.compressImageUri
import java.util.UUID
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
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.ui.components.StoreStatusBadge
import com.example.vallego.ui.components.SubOrderCountdownTimerBadge
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoBusinessBanner
import com.example.vallego.ui.components.ValleGoProductImage
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(profile.id) {
        viewModel.initialize(profile.id, profile.acceptingOrders, profile.businessLocation)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
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
                Text("Rechazar Subpedido", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "El comprador será notificado y su orden total se recalculará automáticamente restando este importe.",
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

    // Modal de Confirmación de Entrega y Cobro
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
                        text = "¿Confirmas que entregaste el pedido al estudiante y recibiste el pago contra entrega?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Subpedido",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Monto a cobrar: S/ %.2f".format(subOrder.subtotalAmount),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF003366),
                                fontSize = 18.sp
                            )
                            val pmName = when (subOrder.paymentMethod) {
                                PaymentMethod.YAPE -> "Yape"
                                PaymentMethod.PLIN -> "Plin"
                                PaymentMethod.EFECTIVO -> "Efectivo"
                                else -> subOrder.paymentMethod?.name ?: "Efectivo"
                            }
                            Text(
                                text = "Medio acordado: $pmName",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
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

    // Modal de Agregar Nuevo Producto al Catálogo
    if (uiState.showAddProductDialog) {
        val context = LocalContext.current
        var prodName by remember { mutableStateOf("") }
        var prodPrice by remember { mutableStateOf("") }
        var prodStock by remember { mutableStateOf("10") }
        var prodDesc by remember { mutableStateOf("") }
        var prodImageUrl by remember { mutableStateOf("") }
        var isUploadingPhoto by remember { mutableStateOf(false) }
        var selectedCatId by remember(uiState.categories) {
            mutableStateOf(uiState.categories.firstOrNull()?.id ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a")
        }
        var validationError by remember { mutableStateOf<String?>(null) }

        val productPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                val bytes = compressImageUri(context, selectedUri, maxDimension = 800, quality = 80)
                if (bytes != null) {
                    isUploadingPhoto = true
                    val path = "products/prod_${UUID.randomUUID()}_${System.currentTimeMillis()}.jpg"
                    viewModel.uploadAsset("product-images", path, bytes) { uploadedUrl ->
                        prodImageUrl = uploadedUrl
                        isUploadingPhoto = false
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissAddProductDialog() },
            title = {
                Text("Nuevo Producto al Catálogo", fontWeight = FontWeight.Bold, color = Color(0xFF003366))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Vista previa de imagen con botón para seleccionar foto de galería
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { productPhotoPicker.launch("image/*") },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ValleGoProductImage(
                                imageUrl = prodImageUrl.takeIf { it.isNotBlank() },
                                categoryName = uiState.categories.find { it.id == selectedCatId }?.name,
                                productName = prodName,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            if (isUploadingPhoto) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ElevatedFilterChip(
                            selected = false,
                            onClick = { productPhotoPicker.launch("image/*") },
                            leadingIcon = {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    text = if (isUploadingPhoto) "Subiendo foto..." else if (prodImageUrl.isBlank()) "Agregar foto desde celular" else "Cambiar foto",
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }

                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it; validationError = null },
                        label = { Text("Nombre del Producto *") },
                        placeholder = { Text("Ej. Triple de Pollo con Palta") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = prodPrice,
                            onValueChange = { prodPrice = it.replace(',', '.'); validationError = null },
                            label = { Text("Precio (S/.) *") },
                            placeholder = { Text("6.50") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = prodStock,
                            onValueChange = { prodStock = it.filter { ch -> ch.isDigit() }; validationError = null },
                            label = { Text("Stock inicial *") },
                            placeholder = { Text("15") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                    }

                    if (uiState.categories.isNotEmpty()) {
                        var expandedCat by remember { mutableStateOf(false) }
                        val currentCatName = uiState.categories.find { it.id == selectedCatId }?.name ?: "Selecciona Categoría"

                        ExposedDropdownMenuBox(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentCatName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCat,
                                onDismissRequest = { expandedCat = false }
                            ) {
                                uiState.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.icon ?: "📦"} ${cat.name}") },
                                        onClick = {
                                            selectedCatId = cat.id
                                            expandedCat = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prodDesc,
                        onValueChange = { prodDesc = it },
                        label = { Text("Descripción corta (opcional)") },
                        placeholder = { Text("Detalles para el alumno") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    val errorToDisplay = validationError ?: uiState.errorMessage
                    if (errorToDisplay != null) {
                        Text(
                            text = errorToDisplay,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPrice = prodPrice.trim().replace(',', '.')
                        val cleanStock = prodStock.trim()
                        val p = cleanPrice.toDoubleOrNull()
                        val s = cleanStock.toIntOrNull()
                        if (prodName.isBlank()) {
                            validationError = "Ingresa el nombre del producto."
                        } else if (p == null || p <= 0.0) {
                            validationError = "Ingresa un precio válido mayor a 0 (ej. 5.50)."
                        } else if (s == null || s < 0) {
                            validationError = "Ingresa una cantidad de stock válida (0 o más)."
                        } else {
                            viewModel.createProduct(
                                name = prodName,
                                price = p,
                                stock = s,
                                categoryId = selectedCatId.ifBlank { uiState.categories.firstOrNull()?.id ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a" },
                                description = prodDesc.ifBlank { prodName },
                                imageUrl = prodImageUrl.takeIf { it.isNotBlank() }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                    enabled = !uiState.isSavingProduct && !isUploadingPhoto
                ) {
                    if (uiState.isSavingProduct) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Guardar Producto")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissAddProductDialog() },
                    enabled = !uiState.isSavingProduct
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Edición Completa de Producto
    if (uiState.selectedProductForEdit != null) {
        val context = LocalContext.current
        val prod = uiState.selectedProductForEdit!!
        var nameInput by remember(prod.id) { mutableStateOf(prod.name) }
        var priceInput by remember(prod.id) { mutableStateOf(prod.price.toString()) }
        var stockInput by remember(prod.id) { mutableStateOf(prod.stock.toString()) }
        var descInput by remember(prod.id) { mutableStateOf(prod.description.orEmpty()) }
        var imageInput by remember(prod.id) { mutableStateOf(prod.imageUrl.orEmpty()) }
        var isUploadingEditPhoto by remember { mutableStateOf(false) }
        var selectedCatId by remember(prod.id) { mutableStateOf(prod.categoryId ?: "") }
        var editError by remember { mutableStateOf<String?>(null) }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        val editProductPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                val bytes = compressImageUri(context, selectedUri, maxDimension = 800, quality = 80)
                if (bytes != null) {
                    isUploadingEditPhoto = true
                    val path = "products/prod_${prod.id}_${System.currentTimeMillis()}.jpg"
                    viewModel.uploadAsset("product-images", path, bytes) { uploadedUrl ->
                        imageInput = uploadedUrl
                        isUploadingEditPhoto = false
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("¿Eliminar producto?", fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que deseas eliminar \"${prod.name}\"?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProduct(prod.id)
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                    ) {
                        Text("Sí, Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissEditProductDialog() },
            title = {
                Text("Editar Producto", fontWeight = FontWeight.Bold, color = Color(0xFF003366))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Vista previa y selector interactivo de imagen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editProductPhotoPicker.launch("image/*") },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ValleGoProductImage(
                                imageUrl = imageInput.takeIf { it.isNotBlank() },
                                categoryName = uiState.categories.find { it.id == selectedCatId }?.name,
                                productName = nameInput,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            if (isUploadingEditPhoto) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ElevatedFilterChip(
                            selected = false,
                            onClick = { editProductPhotoPicker.launch("image/*") },
                            leadingIcon = {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    text = if (isUploadingEditPhoto) "Subiendo foto..." else if (imageInput.isBlank()) "Subir foto desde celular" else "Cambiar foto",
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; editError = null },
                        label = { Text("Nombre del Producto *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it.replace(',', '.'); editError = null },
                            label = { Text("Precio (S/.) *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = stockInput,
                            onValueChange = { stockInput = it.filter { ch -> ch.isDigit() }; editError = null },
                            label = { Text("Stock *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    if (uiState.categories.isNotEmpty()) {
                        var expandedCat by remember { mutableStateOf(false) }
                        val currentCatName = uiState.categories.find { it.id == selectedCatId }?.name ?: "Selecciona Categoría"

                        ExposedDropdownMenuBox(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentCatName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCat,
                                onDismissRequest = { expandedCat = false }
                            ) {
                                uiState.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.icon ?: "📦"} ${cat.name}") },
                                        onClick = {
                                            selectedCatId = cat.id
                                            expandedCat = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    if (editError != null) {
                        Text(
                            text = editError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = priceInput.trim().toDoubleOrNull()
                        val s = stockInput.trim().toIntOrNull()
                        if (nameInput.isBlank()) {
                            editError = "El nombre no puede estar vacío."
                        } else if (p == null || p <= 0) {
                            editError = "Precio inválido."
                        } else if (s == null || s < 0) {
                            editError = "Stock inválido."
                        } else {
                            viewModel.updateProduct(
                                productId = prod.id,
                                name = nameInput,
                                price = p,
                                stock = s,
                                categoryId = selectedCatId.takeIf { it.isNotBlank() },
                                description = descInput,
                                imageUrl = imageInput
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                    enabled = !uiState.isSavingProduct && !isUploadingEditPhoto
                ) {
                    if (uiState.isSavingProduct) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Guardar Cambios")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC8102E))
                    ) {
                        Text("Eliminar")
                    }
                    TextButton(onClick = { viewModel.dismissEditProductDialog() }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Modal de Incidencia: Comprador no se presentó
    if (uiState.selectedSubOrderForNoShow != null) {
        val subOrder = uiState.selectedSubOrderForNoShow!!
        var noShowReason by remember { mutableStateOf("El comprador no asistió al punto en el horario acordado") }

        AlertDialog(
            onDismissRequest = { viewModel.dismissNoShowDialog() },
            title = {
                Text("Comprador no se presentó", fontWeight = FontWeight.Bold, color = Color(0xFFC8102E))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "¿Deseas reportar la inasistencia del comprador? El subpedido cambiará a 'NO ENTREGADO' y las unidades reservadas se restituirán inmediatamente a tu stock.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = noShowReason,
                        onValueChange = { noShowReason = it },
                        label = { Text("Detalle de la incidencia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmBuyerNoShow(subOrder.id, noShowReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text("Confirmar No-Show")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNoShowDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Edición de Stock
    if (uiState.selectedProductForStockEdit != null) {
        val prod = uiState.selectedProductForStockEdit!!
        var stockInput by remember(prod.id) { mutableStateOf(prod.stock.toString()) }
        var stockError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissEditStockDialog() },
            title = {
                Text("Actualizar Stock", fontWeight = FontWeight.Bold, color = Color(0xFF003366))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Producto: ${prod.name}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Modifica la cantidad disponible. Si asignas 0, el producto se pausará automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = {
                            stockInput = it.filter { ch -> ch.isDigit() }
                            stockError = null
                        },
                        label = { Text("Stock disponible *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (stockError != null) {
                        Text(
                            text = stockError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = stockInput.toIntOrNull()
                        if (s == null || s < 0) {
                            stockError = "Ingresa un número entero válido (0 o más)."
                        } else {
                            viewModel.updateStock(prod.id, s)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                ) {
                    Text("Guardar Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEditStockDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.selectedTab == SellerTab.PRODUCTOS) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddProductDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                    text = { Text("Nuevo Producto") },
                    containerColor = Color(0xFF003366),
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = profile.fullName.ifBlank { "Mi Emprendimiento" },
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                        val subtitle = listOfNotNull(
                            profile.businessDescription?.takeIf { it.isNotBlank() },
                            profile.businessCategory?.takeIf { it.isNotBlank() },
                            profile.businessLocation?.takeIf { it.isNotBlank() }
                        ).firstOrNull() ?: "Emprendimiento Valle-Go"
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
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
                            text = if (uiState.isAcceptingOrders) "Aceptando subpedidos en campus" else "No visible en catálogo",
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

            // Selector de Pestañas: Subpedidos vs Mis Productos vs Mi Puesto
            PrimaryTabRow(
                selectedTabIndex = when (uiState.selectedTab) {
                    SellerTab.PEDIDOS -> 0
                    SellerTab.PRODUCTOS -> 1
                    SellerTab.MI_PUESTO -> 2
                },
                containerColor = Color.Transparent,
                contentColor = Color(0xFF003366)
            ) {
                Tab(
                    selected = uiState.selectedTab == SellerTab.PEDIDOS,
                    onClick = { viewModel.setSelectedTab(SellerTab.PEDIDOS) },
                    text = { Text("Subpedidos (${uiState.totalSubOrdersToday})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == SellerTab.PRODUCTOS,
                    onClick = { viewModel.setSelectedTab(SellerTab.PRODUCTOS) },
                    text = { Text("Mis Productos (${uiState.products.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == SellerTab.MI_PUESTO,
                    onClick = { viewModel.setSelectedTab(SellerTab.MI_PUESTO) },
                    text = { Text("Mi Puesto", fontWeight = FontWeight.Bold) }
                )
            }

            when (uiState.selectedTab) {
                SellerTab.PEDIDOS -> {
                    // Resumen de Métricas / KPIs del Día
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
                                    text = "No hay subpedidos en esta sección",
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
                                    onOpenRejection = { viewModel.openRejectionDialog(subOrder) },
                                    onOpenNoShow = { viewModel.openNoShowDialog(subOrder) },
                                    onExpired = { viewModel.onSubOrderExpired(subOrder.id) }
                                )
                            }
                        }
                    }
                }
                SellerTab.PRODUCTOS -> {
                    // Pestaña Mis Productos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Productos en Venta (${uiState.products.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                        Button(
                            onClick = { viewModel.openAddProductDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nuevo")
                        }
                    }

                    if (uiState.products.isEmpty()) {
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
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aún no tienes productos registrados en tu puesto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.openAddProductDialog() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                                ) {
                                    Text("Publicar Primer Producto")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.products, key = { it.id }) { product ->
                                val catName = uiState.categories.find { it.id == product.categoryId }?.name
                                ProductCard(
                                    product = product,
                                    categoryName = catName,
                                    onToggleActive = { isActive ->
                                        viewModel.toggleProductActive(product.id, isActive)
                                    },
                                    onEditStock = {
                                        viewModel.openEditStockDialog(product)
                                    },
                                    onEditProduct = {
                                        viewModel.openEditProductDialog(product)
                                    }
                                )
                            }
                        }
                    }
                }
                SellerTab.MI_PUESTO -> {
                    SellerStoreProfileTab(
                        profile = profile,
                        sellerProfile = uiState.sellerProfile,
                        isSaving = uiState.isSavingProfile,
                        isUploading = uiState.isUploadingAsset,
                        onUploadAsset = { bucket, path, bytes, onUploaded ->
                            viewModel.uploadAsset(bucket, path, bytes, onUploaded)
                        },
                        onSave = { name, status, desc, cat, loc, open, close, banner, avatar, accepting ->
                            viewModel.updateBusinessProfile(
                                businessName = name,
                                businessStatus = status,
                                businessDescription = desc,
                                businessCategory = cat,
                                businessLocation = loc,
                                openTime = open,
                                closeTime = close,
                                bannerUrl = banner,
                                avatarUrl = avatar,
                                acceptingOrders = accepting
                            )
                        }
                    )
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
    onOpenNoShow: () -> Unit,
    onExpired: () -> Unit,
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
            // Cabecera: ID del subpedido, Temporizador de 15 min y Badge de Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subpedido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Total: S/ %.2f".format(subOrder.subtotalAmount),
                        color = Color(0xFF003366),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (subOrder.status == SubOrderStatus.PENDIENTE) {
                        SubOrderCountdownTimerBadge(
                            createdAtIso = subOrder.createdAt,
                            status = subOrder.status,
                            onExpired = onExpired
                        )
                    }
                    StatusBadge(status = subOrder.status)
                }
            }

            HorizontalDivider()

            // Lista de Ítems del subpedido
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (subOrder.items.isEmpty()) {
                    Text(
                        text = "• 1x Subpedido Campus (S/ %.2f)".format(subOrder.subtotalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
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
            }

            HorizontalDivider()

            // Medio de pago y Botones de Acción según el estado actual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (pmBg, pmTint, pmLabel) = when (subOrder.paymentMethod) {
                    PaymentMethod.YAPE -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                    PaymentMethod.PLIN -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                    PaymentMethod.EFECTIVO -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), "Efectivo")
                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), subOrder.paymentMethod?.name ?: "Efectivo")
                }
                Surface(
                    color = pmBg,
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
                            tint = pmTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = pmLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = pmTint
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
                            Text("Iniciar Preparación")
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenNoShow,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("No se presentó", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onOpenDelivery,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Confirmar Entrega", fontSize = 12.sp)
                            }
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
                                text = "Entregado y Cobrado ✓",
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
                    SubOrderStatus.NO_ENTREGADO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonOff,
                                contentDescription = null,
                                tint = Color(0xFFC8102E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "No entregado (Inasistencia)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC8102E)
                            )
                        }
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
        SubOrderStatus.EN_PREPARACION -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "En Preparación")
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

@Composable
fun ProductCard(
    product: Product,
    categoryName: String?,
    onToggleActive: (Boolean) -> Unit,
    onEditStock: () -> Unit,
    onEditProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen del producto con fallback elegante de categoría (pastel + emoji)
            ValleGoProductImage(
                imageUrl = product.imageUrl,
                categoryName = categoryName,
                productName = product.name,
                modifier = Modifier
                    .size(72.dp)
                    .clickable(onClick = onEditProduct)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditProduct)
            ) {
                if (!categoryName.isNullOrBlank()) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF003366),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ %.2f".format(product.price),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF003366),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        onClick = onEditStock,
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOutOfStock) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOutOfStock) Color(0xFFC8102E).copy(alpha = 0.5f) else Color.LightGray
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Stock: ${product.stock}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOutOfStock) Color(0xFFC8102E) else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Stock",
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF003366)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEditProduct,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar producto completo",
                        tint = Color(0xFF003366),
                        modifier = Modifier.size(18.dp)
                    )
                }

                val statusText = when {
                    isOutOfStock -> "Sin stock"
                    product.isActive -> "Activo"
                    else -> "Pausado"
                }
                val statusColor = when {
                    isOutOfStock -> Color(0xFFC8102E)
                    product.isActive -> Color(0xFF2E7D32)
                    else -> Color(0xFFD97706)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = product.isActive && !isOutOfStock,
                    onCheckedChange = { desired ->
                        if (isOutOfStock) {
                            onToggleActive(false)
                        } else {
                            onToggleActive(desired)
                        }
                    },
                    enabled = !isOutOfStock
                )
            }
        }
    }
}

@Composable
fun SellerStoreProfileTab(
    profile: UserProfile,
    sellerProfile: UserProfile?,
    isSaving: Boolean,
    isUploading: Boolean = false,
    onUploadAsset: ((bucket: String, path: String, bytes: ByteArray, onUploaded: (String) -> Unit) -> Unit)? = null,
    onSave: (
        businessName: String,
        businessStatus: String,
        businessDescription: String?,
        businessCategory: String?,
        businessLocation: String?,
        openTime: String?,
        closeTime: String?,
        bannerUrl: String?,
        avatarUrl: String?,
        acceptingOrders: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile = sellerProfile ?: profile
    var businessName by remember(activeProfile.id, activeProfile.businessName) {
        mutableStateOf(activeProfile.businessName ?: activeProfile.fullName)
    }
    var description by remember(activeProfile.id, activeProfile.businessDescription) {
        mutableStateOf(activeProfile.businessDescription.orEmpty())
    }
    var category by remember(activeProfile.id, activeProfile.businessCategory) {
        mutableStateOf(activeProfile.businessCategory.orEmpty())
    }
    var location by remember(activeProfile.id, activeProfile.businessLocation) {
        mutableStateOf(activeProfile.businessLocation ?: activeProfile.campus)
    }
    var openTime by remember(activeProfile.id, activeProfile.openTime) {
        mutableStateOf(activeProfile.openTime ?: "08:00")
    }
    var closeTime by remember(activeProfile.id, activeProfile.closeTime) {
        mutableStateOf(activeProfile.closeTime ?: "18:00")
    }
    var bannerUrl by remember(activeProfile.id, activeProfile.bannerUrl) {
        mutableStateOf(activeProfile.bannerUrl.orEmpty())
    }
    var avatarUrl by remember(activeProfile.id, activeProfile.avatarUrl) {
        mutableStateOf(activeProfile.avatarUrl.orEmpty())
    }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    var businessStatus by remember(activeProfile.id, activeProfile.businessStatus) {
        mutableStateOf(activeProfile.businessStatus.ifBlank { "ABIERTO" })
    }
    var acceptingOrders by remember(activeProfile.id, activeProfile.acceptingOrders) {
        mutableStateOf(activeProfile.acceptingOrders)
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bytes = compressImageUri(context, selectedUri, maxDimension = 1200, quality = 82)
            if (bytes != null && onUploadAsset != null) {
                isUploadingBanner = true
                val path = "banners/banner_${activeProfile.id}_${System.currentTimeMillis()}.jpg"
                onUploadAsset("business-assets", path, bytes) { uploadedUrl ->
                    bannerUrl = uploadedUrl
                    isUploadingBanner = false
                }
            }
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bytes = compressImageUri(context, selectedUri, maxDimension = 512, quality = 85)
            if (bytes != null && onUploadAsset != null) {
                isUploadingAvatar = true
                val path = "avatars/avatar_${activeProfile.id}_${System.currentTimeMillis()}.jpg"
                onUploadAsset("business-assets", path, bytes) { uploadedUrl ->
                    avatarUrl = uploadedUrl
                    isUploadingAvatar = false
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vista previa de cabecera con Banner y Logo comercial interactivos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Banner interactivo: Toca para cambiar foto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .clickable { bannerPickerLauncher.launch("image/*") }
                ) {
                    ValleGoBusinessBanner(
                        bannerUrl = bannerUrl.takeIf { it.isNotBlank() },
                        storeName = businessName,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Botón flotante para cambiar portada
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isUploadingBanner) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Text("Subiendo...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Cambiar portada", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Logo/Avatar interactivo: Toca para cambiar logo
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 85.dp)
                        .clickable { avatarPickerLauncher.launch("image/*") }
                ) {
                    ValleGoBusinessAvatar(
                        avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                        storeName = businessName,
                        size = 68.dp
                    )
                    Surface(
                        color = Color(0xFF003366),
                        shape = CircleShape,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isUploadingAvatar) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Cambiar logo", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = businessName.ifBlank { "Nombre del Puesto" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366)
                    )
                    StoreStatusBadge(status = businessStatus, acceptingOrders = acceptingOrders)
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 $location  •  🕒 $openTime - $closeTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Selector de 4 Estados del Puesto
        Text(
            text = "Estado Actual del Puesto",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF003366)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val states = listOf(
                "ABIERTO" to "🟢 Abierto",
                "SATURADO" to "🟠 Saturado",
                "PAUSADO" to "🟡 Pausado",
                "CERRADO" to "⚪ Cerrado"
            )
            states.forEach { (statusKey, label) ->
                val isSelected = businessStatus.equals(statusKey, ignoreCase = true)
                OutlinedButton(
                    onClick = {
                        businessStatus = statusKey
                        if (statusKey == "CERRADO") acceptingOrders = false
                        if (statusKey == "ABIERTO" || statusKey == "SATURADO") acceptingOrders = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Color(0xFF003366) else Color.Transparent,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        if (businessStatus == "SATURADO") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ Modo Saturado: Los compradores verán un aviso de alta demanda indicando que su pedido puede tardar un poco más.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Datos del puesto
        Text(
            text = "Información del Emprendimiento",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF003366)
        )

        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Nombre Comercial del Puesto *") },
            placeholder = { Text("Ej. El Rincón del Sabor UCV") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción del Negocio") },
            placeholder = { Text("Ej. Hamburguesas artesanales, triples y jugos recién hechos") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Giro / Categoría") },
                placeholder = { Text("Comidas / Postres") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Ubicación en campus") },
                placeholder = { Text("Pabellón A / Cafetería") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = openTime,
                onValueChange = { openTime = it },
                label = { Text("Apertura") },
                placeholder = { Text("08:00 AM") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = closeTime,
                onValueChange = { closeTime = it },
                label = { Text("Cierre") },
                placeholder = { Text("06:00 PM") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Card explicativa de fotos (Cero campos URL)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF003366), modifier = Modifier.size(22.dp))
                Text(
                    text = "💡 Para cambiar tu foto de portada o logotipo comercial, tócalos directamente arriba y selecciónalos desde tu celular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = {
                onSave(
                    businessName,
                    businessStatus,
                    description,
                    category,
                    location,
                    openTime,
                    closeTime,
                    bannerUrl,
                    avatarUrl,
                    acceptingOrders
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
            enabled = !isSaving && !isUploadingBanner && !isUploadingAvatar
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardando...")
            } else {
                Icon(Icons.Default.Store, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Información de Mi Puesto", fontWeight = FontWeight.Bold)
            }
        }
    }
}