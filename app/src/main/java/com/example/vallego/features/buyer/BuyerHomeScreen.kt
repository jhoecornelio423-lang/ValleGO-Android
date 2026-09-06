package com.example.vallego.features.buyer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Schedule
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.repository.CartRepository
import com.example.vallego.domain.repository.ProductRepository
import com.example.vallego.features.cart.CartScreen
import com.example.vallego.features.tracking.OrderTrackingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

data class StoreCatalogGroup(
    val sellerId: String,
    val sellerName: String,
    val location: String?,
    val products: List<Product>
)

    var realStoresWithProducts by remember { mutableStateOf<List<StoreCatalogGroup>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    fun loadCatalog(isSilent: Boolean = false) {
        if (!isSilent && realStoresWithProducts.isEmpty()) {
            isLoadingCatalog = true
        }
        coroutineScope.launch {
            val prodsResult = productRepository.getActiveProducts()
            val sellersResult = productRepository.getSellerProfiles()

            val products = prodsResult.getOrDefault(emptyList())
            val sellers = sellersResult.getOrDefault(emptyList()).associateBy { it.id }

            if (products.isNotEmpty()) {
                val grouped = products.groupBy { it.sellerId }.mapNotNull { (sellerId, sellerProds) ->
                    val seller = sellers[sellerId]
                    // Si el vendedor tiene el puesto cerrado (acceptingOrders == false), no se muestra en el catálogo
                    if (seller != null && !seller.acceptingOrders) {
                        return@mapNotNull null
                    }
                    val storeTitle = seller?.fullName?.trim()?.takeIf { it.isNotBlank() }
                        ?: seller?.businessDescription?.trim()?.takeIf { it.isNotBlank() }
                        ?: "Emprendedor Valle-Go"
                    val loc = seller?.businessLocation?.trim()?.takeIf { it.isNotBlank() }
                    StoreCatalogGroup(
                        sellerId = sellerId,
                        sellerName = storeTitle,
                        location = loc,
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
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
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
                                contentDescription = "Carrito"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner de bienvenida
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF003366)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "¡Hola, ${profile.fullName.split(" ").firstOrNull() ?: "Estudiante"}! 👋",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Agrega antojos de varios puestos a la vez. Tu orden se dividirá automáticamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
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
                            text = "Sincronizando catálogo con Supabase Cloud...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (realStoresWithProducts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No hay productos activos en este momento",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Los emprendedores aún no han publicado o sus puestos están pausados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { loadCatalog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                        ) {
                            Text("Recargar Catálogo")
                        }
                    }
                }
            } else {
                realStoresWithProducts.forEach { group ->
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🏪 ${group.sellerName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF003366)
                                )
                                if (!group.location.isNullOrBlank()) {
                                    Text(
                                        text = "📍 ${group.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Badge(containerColor = Color(0xFF2E7D32)) {
                                Text("Abierto", color = Color.White, modifier = Modifier.padding(4.dp))
                            }
                        }

                        HorizontalDivider()

                        group.products.forEach { product ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (product.description != null) {
                                        Text(
                                            text = product.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "S/ %.2f".format(product.price),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF003366),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        cartRepository.setStoreName(group.sellerId, group.sellerName)
                                        cartRepository.addToCart(product, 1)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pedir", fontSize = 12.sp)
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
