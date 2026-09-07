package com.example.vallego.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vallego.domain.model.SubOrderStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class CategoryVisualTheme(
    val emoji: String,
    val backgroundBrush: Brush,
    val contentColor: Color
)

fun resolveCategoryVisualTheme(categoryName: String?, productName: String? = null): CategoryVisualTheme {
    val text = "${categoryName.orEmpty()} ${productName.orEmpty()}".lowercase()
    return when {
        text.contains("hamburguesa") || text.contains("almuerzo") || text.contains("comida") ||
        text.contains("pollo") || text.contains("chaufa") || text.contains("salchipapa") ||
        text.contains("arroz") || text.contains("sandwich") || text.contains("menú") || text.contains("menu") -> {
            CategoryVisualTheme(
                emoji = "🍔",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFFEDD5), Color(0xFFFED7AA))),
                contentColor = Color(0xFFC2410C)
            )
        }
        text.contains("postre") || text.contains("dulce") || text.contains("queque") ||
        text.contains("torta") || text.contains("tarta") || text.contains("alfajor") ||
        text.contains("brownie") || text.contains("pastel") || text.contains("galleta") ||
        text.contains("chocolate") || text.contains("crepa") || text.contains("waffle") -> {
            CategoryVisualTheme(
                emoji = "🧁",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFCE7F3), Color(0xFFFBCFE8))),
                contentColor = Color(0xFFBE185D)
            )
        }
        text.contains("bebida") || text.contains("jugo") || text.contains("chicha") ||
        text.contains("café") || text.contains("cafe") || text.contains("gaseosa") ||
        text.contains("agua") || text.contains("infusión") || text.contains("frappe") ||
        text.contains("batido") || text.contains("smoothie") -> {
            CategoryVisualTheme(
                emoji = "🥤",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD))),
                contentColor = Color(0xFF0369A1)
            )
        }
        text.contains("snack") || text.contains("papa") || text.contains("chips") ||
        text.contains("doritos") || text.contains("piqueo") || text.contains("frutos") ||
        text.contains("canchita") || text.contains("popcorn") -> {
            CategoryVisualTheme(
                emoji = "🍿",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
                contentColor = Color(0xFFB45309)
            )
        }
        text.contains("papel") || text.contains("cuaderno") || text.contains("copia") ||
        text.contains("util") || text.contains("impresion") || text.contains("libro") -> {
            CategoryVisualTheme(
                emoji = "📚",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE))),
                contentColor = Color(0xFF6D28D9)
            )
        }
        else -> {
            CategoryVisualTheme(
                emoji = "🍱",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))),
                contentColor = Color(0xFF475569)
            )
        }
    }
}

@Composable
fun ValleGoProductImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    productName: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(12.dp),
    emojiSize: Int = 26
) {
    val theme = remember(categoryName, productName) {
        resolveCategoryVisualTheme(categoryName, productName)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(theme.backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            var isError by remember(imageUrl) { mutableStateOf(false) }

            if (!isError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = productName ?: "Producto",
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isError = true }
                )
            } else {
                Text(
                    text = theme.emoji,
                    fontSize = emojiSize.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = theme.emoji,
                fontSize = emojiSize.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ValleGoBusinessBanner(
    bannerUrl: String?,
    storeName: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF002244),
                        Color(0xFF003366),
                        Color(0xFFCC0000)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!bannerUrl.isNullOrBlank()) {
            var isError by remember(bannerUrl) { mutableStateOf(false) }
            if (!isError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bannerUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = storeName ?: "Portada del puesto",
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isError = true }
                )
            } else {
                FallbackStoreBannerContent(storeName)
            }
        } else {
            FallbackStoreBannerContent(storeName)
        }
    }
}

@Composable
private fun FallbackStoreBannerContent(storeName: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Store,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = storeName?.trim()?.takeIf { it.isNotBlank() } ?: "Puesto Universitario Valle-Go",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1
        )
    }
}

@Composable
fun ValleGoBusinessAvatar(
    avatarUrl: String?,
    storeName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = CircleShape
) {
    val initials = remember(storeName) {
        val trimmed = storeName?.trim().orEmpty()
        if (trimmed.isNotBlank()) {
            val parts = trimmed.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                "${parts[0].first()}${parts[1].first()}".uppercase()
            } else {
                trimmed.take(2).uppercase()
            }
        } else {
            "VG"
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF003366), Color(0xFF0284C7))
                )
            )
            .border(1.5.dp, Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            var isError by remember(avatarUrl) { mutableStateOf(false) }
            if (!isError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = storeName ?: "Logo del puesto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isError = true }
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            )
        }
    }
}

@Composable
fun ValleGoUserAvatar(
    avatarUrl: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = CircleShape
) {
    val initials = remember(name) {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isNotBlank()) {
            val parts = trimmed.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                "${parts[0].first()}${parts[1].first()}".uppercase()
            } else {
                trimmed.take(2).uppercase()
            }
        } else {
            "U"
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF003366), Color(0xFF1E88E5))
                )
            )
            .border(1.5.dp, Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            var isError by remember(avatarUrl) { mutableStateOf(false) }
            if (!isError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name ?: "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isError = true }
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            )
        }
    }
}

fun isSubOrderExpired(createdAtIso: String?): Boolean {
    if (createdAtIso.isNullOrBlank()) return false
    val target = parseIsoEpochMillis(createdAtIso) + (15 * 60 * 1000L)
    return System.currentTimeMillis() >= target
}

@Composable
fun SubOrderCountdownTimerBadge(
    createdAtIso: String?,
    status: SubOrderStatus,
    modifier: Modifier = Modifier,
    onExpired: (() -> Unit)? = null,
    onWarning5Min: (() -> Unit)? = null,
    onWarning10Min: (() -> Unit)? = null
) {
    if (status != SubOrderStatus.PENDIENTE) return

    val targetEpochMillis = remember(createdAtIso) {
        parseIsoEpochMillis(createdAtIso) + (15 * 60 * 1000L)
    }

    var remainingMillis by remember(targetEpochMillis) {
        mutableLongStateOf(maxOf(0L, targetEpochMillis - System.currentTimeMillis()))
    }

    var hasWarned10Min by remember(createdAtIso) { mutableStateOf(false) }
    var hasWarned5Min by remember(createdAtIso) { mutableStateOf(false) }
    var hasExpiredReported by remember(createdAtIso) { mutableStateOf(false) }

    LaunchedEffect(targetEpochMillis) {
        val initialRem = maxOf(0L, targetEpochMillis - System.currentTimeMillis())
        if (initialRem == 0L && !hasExpiredReported) {
            hasExpiredReported = true
            onExpired?.invoke()
            return@LaunchedEffect
        }

        while (true) {
            val now = System.currentTimeMillis()
            val rem = maxOf(0L, targetEpochMillis - now)
            remainingMillis = rem

            val remSec = rem / 1000
            if (remSec in 1..600 && !hasWarned10Min) {
                hasWarned10Min = true
                onWarning10Min?.invoke()
            }
            if (remSec in 1..300 && !hasWarned5Min) {
                hasWarned5Min = true
                onWarning5Min?.invoke()
            }
            if (rem == 0L && !hasExpiredReported) {
                hasExpiredReported = true
                onExpired?.invoke()
                break
            }
            delay(1000L)
        }
    }

    val totalSeconds = remainingMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val (bgCol, textCol, borderCol) = when {
        totalSeconds > 600 -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFFA5D6A7)) // Verde
        totalSeconds in 301..600 -> Triple(Color(0xFFFFF3E0), Color(0xFFEF6C00), Color(0xFFFFCC80)) // Ámbar
        totalSeconds in 1..300 -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Color(0xFFEF9A9A)) // Rojo urgente
        else -> Triple(Color(0xFFFFCDD2), Color(0xFFB71C1C), Color(0xFFE57373)) // Expirado
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by if (totalSeconds in 1..300) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgCol)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (totalSeconds <= 300) Icons.Default.WarningAmber else Icons.Default.Schedule,
            contentDescription = "Temporizador",
            tint = textCol.copy(alpha = alphaAnim),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (totalSeconds > 0) "Aceptar en: $formattedTime" else "¡Tiempo agotado!",
            color = textCol.copy(alpha = alphaAnim),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun StoreStatusBadge(
    status: String?,
    acceptingOrders: Boolean,
    modifier: Modifier = Modifier
) {
    val normStatus = (status ?: if (acceptingOrders) "ABIERTO" else "CERRADO").uppercase()

    val (bgCol, textCol, label) = when {
        !acceptingOrders || normStatus == "CERRADO" ->
            Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "Cerrado")
        normStatus == "SATURADO" ->
            Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Saturado (Demoras)")
        normStatus == "PAUSADO" ->
            Triple(Color(0xFFFFF7ED), Color(0xFFEA580C), "Pausado")
        else ->
            Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "Abierto")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgCol)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(textCol)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = textCol,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun parseIsoEpochMillis(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return System.currentTimeMillis()
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(isoString)
            if (date != null) return date.time
        } catch (_: Exception) {
            // try next
        }
    }
    return System.currentTimeMillis()
}

fun compressImageUri(
    context: android.content.Context,
    uri: android.net.Uri,
    maxDimension: Int = 1024,
    quality: Int = 80
): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return null

        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = minOf(1f, maxDimension.toFloat() / maxOf(width, height))
        val scaledBitmap = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                originalBitmap,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            originalBitmap
        }

        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        android.util.Log.e("ValleGoVisual", "Error comprimiendo imagen: ${e.message}", e)
        null
    }
}
