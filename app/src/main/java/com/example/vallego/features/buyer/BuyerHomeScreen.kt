package com.example.vallego.features.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.Category
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.repository.CartRepository
import com.example.vallego.domain.repository.ProductRepository
import com.example.vallego.features.cart.CartScreen
import com.example.vallego.features.tracking.OrderTrackingScreen
import com.example.vallego.ui.components.StoreStatusBadge
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoBusinessBanner
import com.example.vallego.ui.components.ValleGoProductImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class StoreCatalogGroup(
    val sellerId: String,
    val sellerName: String,
    val location: String?,
    val bannerUrl: String?,
    val avatarUrl: String?,
    val businessStatus: String,
    val openTime: String?,
    val closeTime: String?,
    val description: String?,
    val acceptingOrders: Boolean,
    val products: List<Product>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerHomeScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    cartRepository: CartRepository = koinInject(),
    productRepository: ProductRepository = koinInject()
) {
    var showCart by remember { mutableStateOf(false) }
    var showTracking by remember { mutableStateOf(false) }
    val cartCalculation by cartRepository.cartCalculation.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("TODOS") }
    var onlyOpenStores by remember { mutableStateOf(false) }

    var selectedProductForDetail by remember { mutableStateOf<Pair<Product, StoreCatalogGroup>?>(null) }

    var realStoresWithProducts by remember { mutableStateOf<List<StoreCatalogGroup>>(emptyList()) }
    var categoriesList by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    fun loadCatalog(isSilent: Boolean = false) {
        if (!isSilent && realStoresWithProducts.isEmpty()) {
            isLoadingCatalog = true
        }
        coroutineScope.launch {
            val prodsResult = productRepository.getActiveProducts()
            val sellersResult = productRepository.getSellerProfiles()
            val catsResult = productRepository.getCategories()

            val products = prodsResult.getOrDefault(emptyList())
            val sellers = sellersResult.getOrDefault(emptyList()).associateBy { it.id }
            categoriesList = catsResult.getOrDefault(emptyList())

            if (products.isNotEmpty()) {
                val grouped = products.groupBy { it.sellerId }.mapNotNull { (sellerId, sellerProds) ->
                    val seller = sellers[sellerId]
                    // Si el vendedor tiene el puesto cerrado físicamente y no acepta pedidos, se oculta
                    if (seller != null && !seller.acceptingOrders && seller.businessStatus == "CERRADO") {
                        return@mapNotNull null
                    }
                    val storeTitle = seller?.displayStoreName ?: "Emprendimiento Valle-Go"
                    val loc = seller?.businessLocation?.trim()?.takeIf { it.isNotBlank() } ?: "Campus ${profile.campus}"
                    StoreCatalogGroup(
                        sellerId = sellerId,
                        sellerName = storeTitle,
                        location = loc,
                        bannerUrl = seller?.bannerUrl,
                        avatarUrl = seller?.avatarUrl,
                        businessStatus = seller?.businessStatus ?: "ABIERTO",
                        openTime = seller?.openTime,
                        closeTime = seller?.closeTime,
                        description = seller?.businessDescription,
                        acceptingOrders = seller?.acceptingOrders ?: true,
                        products = sellerProds
                    )
                }
                realStoresWithProducts = grouped
            } else {
                realStoresWithProducts = emptyList()
            }
            isLoadingCatalog = false
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            loadCatalog(isSilent = true)
            delay(8000)
        }
    }

    if (showTracking) {
        OrderTrackingScreen(
            buyerProfile = profile,
            onNavigateBack = { showTracking = false },
            onNavigateToCart = {
                showTracking = false
                showCart = true
            },
            modifier = modifier
        )
        return
    }

    if (showCart) {
        CartScreen(
            buyerProfile = profile,
            onNavigateBack = { showCart = false },
            onNavigateToTracking = {
                showCart = false
                showTracking = true
            },
            modifier = modifier
        )
        return
    }

    // Modal de Detalle de Producto al estilo PedidosYa
    if (selectedProductForDetail != null) {
        val (prod, store) = selectedProductForDetail!!
        val catName = categoriesList.find { it.id == prod.categoryId }?.name
        ProductDetailBottomSheet(
            product = prod,
            storeName = store.sellerName,
            categoryName = catName,
            onDismiss = { selectedProductForDetail = null },
            onAddToCart = { product, quantity, instructions ->
                cartRepository.setStoreName(store.sellerId, store.sellerName)
                cartRepository.addToCart(product, quantity)
            }
        )
    }

    // Filtrado reactivo en tiempo real
    val filteredStores = remember(realStoresWithProducts, searchQuery, selectedCategoryFilter, onlyOpenStores, categoriesList) {
        val q = searchQuery.trim().lowercase()
        realStoresWithProducts.mapNotNull { store ->
            if (onlyOpenStores && (!store.acceptingOrders || store.businessStatus.equals("CERRADO", ignoreCase = true))) {
                return@mapNotNull null
            }
            val storeMatches = q.isEmpty() || store.sellerName.lowercase().contains(q) || (store.description?.lowercase()?.contains(q) == true)
            val matchingProducts = store.products.filter { prod ->
                val matchesSearch = q.isEmpty() || prod.name.lowercase().contains(q) || (prod.description?.lowercase()?.contains(q) == true) || storeMatches
                val catName = categoriesList.find { it.id == prod.categoryId }?.name?.uppercase().orEmpty()
                val matchesCategory = when (selectedCategoryFilter) {
                    "TODOS" -> true
                    "COMIDAS" -> catName.contains("COMIDA") || catName.contains("ALMUERZO") || prod.name.lowercase().contains("hamburguesa") || prod.name.lowercase().contains("pollo")
                    "POSTRES" -> catName.contains("POSTRE") || catName.contains("DULCE") || prod.name.lowercase().contains("queque") || prod.name.lowercase().contains("torta") || prod.name.lowercase().contains("alfajor")
                    "BEBIDAS" -> catName.contains("BEBIDA") || catName.contains("JUGO") || prod.name.lowercase().contains("chicha") || prod.name.lowercase().contains("café") || prod.name.lowercase().contains("cafe")
                    "SNACKS" -> catName.contains("SNACK") || prod.name.lowercase().contains("papa") || prod.name.lowercase().contains("snack") || prod.name.lowercase().contains("galleta")
                    "PAPELERIA" -> catName.contains("PAPEL") || catName.contains("UTIL") || prod.name.lowercase().contains("cuaderno") || prod.name.lowercase().contains("copia")
                    else -> true
                }
                matchesSearch && matchesCategory
            }

            if (matchingProducts.isNotEmpty()) {
                store.copy(products = matchingProducts)
            } else {
                null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Valle-Go",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFC8102E),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Campus ${profile.campus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { loadCatalog() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar catálogo",
                            tint = Color(0xFF003366)
                        )
                    }
                    IconButton(onClick = { showTracking = true }) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Mis Pedidos",
                            tint = Color(0xFF003366)
                        )
                    }
                    IconButton(onClick = { showCart = true }) {
                        BadgedBox(
                            badge = {
                                if (cartCalculation.totalItemCount > 0) {
                                    Badge(containerColor = Color(0xFFC8102E)) {
                                        Text("${cartCalculation.totalItemCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Carrito",
                                tint = Color(0xFF003366)
                            )
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Barra de Búsqueda reactiva
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar hamburguesa, café, postres o puesto...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color(0xFF003366)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Carrusel horizontal de categorías por chips (Estilo PedidosYa)
            val categoryChips = listOf(
                "TODOS" to "🍽️ Todos",
                "COMIDAS" to "🍔 Comidas",
                "POSTRES" to "🧁 Postres",
                "BEBIDAS" to "🥤 Bebidas",
                "SNACKS" to "🍿 Snacks",
                "PAPELERIA" to "📚 Papelería"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip rápido de puestos abiertos
                FilterChip(
                    selected = onlyOpenStores,
                    onClick = { onlyOpenStores = !onlyOpenStores },
                    label = { Text("🟢 Abiertos ahora") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2E7D32),
                        selectedLabelColor = Color.White
                    )
                )

                categoryChips.forEach { (key, label) ->
                    val isSelected = selectedCategoryFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = key },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF003366),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Banner informativo de entrega multicentro
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF003366)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¡Antojos en Campus ${profile.campus}! 🎒",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pide de diferentes puestos en una sola compra. Valle-Go organizará tus subpedidos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (isLoadingCatalog) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF003366))
                        Text(
                            text = "Cargando catálogo universitario...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredStores.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron resultados para \"$searchQuery\"" else "No hay productos disponibles por ahora",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Intenta buscando por otro término o selecciona otra categoría.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedCategoryFilter = "TODOS"
                                onlyOpenStores = false
                                loadCatalog()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                        ) {
                            Text("Restablecer Filtros")
                        }
                    }
                }
            } else {
                // Listado de Puestos con Portada, Logo, Estado y Productos
                filteredStores.forEach { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Cabecera Visual del Puesto: Banner + Avatar Superpuesto
                            Box(modifier = Modifier.fillMaxWidth()) {
                                ValleGoBusinessBanner(
                                    bannerUrl = group.bannerUrl,
                                    storeName = group.sellerName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(start = 14.dp, top = 75.dp)
                                ) {
                                    ValleGoBusinessAvatar(
                                        avatarUrl = group.avatarUrl,
                                        storeName = group.sellerName,
                                        size = 52.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Información del Puesto y Badge de Estado
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.sellerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF003366)
                                    )
                                    StoreStatusBadge(
                                        status = group.businessStatus,
                                        acceptingOrders = group.acceptingOrders
                                    )
                                }

                                if (!group.description.isNullOrBlank()) {
                                    Text(
                                        text = group.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }

                                val schedule = if (!group.openTime.isNullOrBlank() && !group.closeTime.isNullOrBlank()) {
                                    "  •  🕒 ${group.openTime} - ${group.closeTime}"
                                } else ""

                                Text(
                                    text = "📍 ${group.location ?: "Campus"}$schedule",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Aviso de puesto saturado
                                if (group.businessStatus.equals("SATURADO", ignoreCase = true)) {
                                    Surface(
                                        color = Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ Alta demanda: Los pedidos de este puesto pueden tardar unos minutos más.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE65100),
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                            // Lista de Productos del Puesto con Miniatura y Fallback Inteligente
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                group.products.forEach { product ->
                                    val catName = categoriesList.find { it.id == product.categoryId }?.name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedProductForDetail = Pair(product, group)
                                            }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Miniatura con degradado pastel y emoji por categoría si imageUrl es null
                                        ValleGoProductImage(
                                            imageUrl = product.imageUrl,
                                            categoryName = catName,
                                            productName = product.name,
                                            modifier = Modifier.size(64.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = product.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
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
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "S/ %.2f".format(product.price),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF003366),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                if (product.stock in 1..3) {
                                                    Text(
                                                        text = "¡Solo ${product.stock}!",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFFC8102E),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                selectedProductForDetail = Pair(product, group)
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddShoppingCart,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pedir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
