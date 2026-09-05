package com.example.vallego.features.buyer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.LocationOn
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
import com.example.vallego.features.cart.CartScreen
import com.example.vallego.features.tracking.OrderTrackingScreen
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerHomeScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    cartRepository: CartRepository = koinInject()
) {
    var showCart by remember { mutableStateOf(false) }
    var showTracking by remember { mutableStateOf(false) }
    val cartCalculation by cartRepository.cartCalculation.collectAsState()

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

    // Catálogo de muestra con productos de múltiples puestos del campus
    val sampleProducts = remember {
        listOf(
            Triple(
                "seller-papu",
                "Papu Burger",
                listOf(
                    Product(
                        id = "papu-1",
                        sellerId = "seller-papu",
                        name = "Hamburguesa Artesanal con Papas",
                        description = "Carne 150g, queso cheddar, papas nativas",
                        price = 12.00,
                        stock = 25
                    ),
                    Product(
                        id = "papu-2",
                        sellerId = "seller-papu",
                        name = "Gaseosa Inka Cola 500ml",
                        price = 3.50,
                        stock = 40
                    )
                )
            ),
            Triple(
                "seller-dulce",
                "Dulce Valle",
                listOf(
                    Product(
                        id = "dulce-1",
                        sellerId = "seller-dulce",
                        name = "Brownie con Helado Artesanal",
                        description = "Chocolate bitter 70% con nueces",
                        price = 7.00,
                        stock = 15
                    ),
                    Product(
                        id = "dulce-2",
                        sellerId = "seller-dulce",
                        name = "Pack 4 Alfajores de Maicena",
                        price = 5.00,
                        stock = 20
                    )
                )
            ),
            Triple(
                "seller-coffee",
                "Coffee Campus",
                listOf(
                    Product(
                        id = "coffee-1",
                        sellerId = "seller-coffee",
                        name = "Capuchino de Vainilla",
                        description = "Café de Chanchamayo recién pasado",
                        price = 6.50,
                        stock = 30
                    )
                )
            )
        )
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

            // Catálogo por puestos
            sampleProducts.forEach { (sellerId, sellerName, products) ->
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
                            Text(
                                text = "🏪 $sellerName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003366)
                            )
                            Badge(containerColor = Color(0xFF2E7D32)) {
                                Text("Abierto", color = Color.White, modifier = Modifier.padding(4.dp))
                            }
                        }

                        HorizontalDivider()

                        products.forEach { product ->
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
                                        cartRepository.setStoreName(sellerId, sellerName)
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
