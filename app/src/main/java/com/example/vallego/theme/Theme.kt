package com.example.vallego.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ValleGOLightColorScheme = lightColorScheme(
    primary = Color(0xFF003366),          // Azul Marino Institucional UCV
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EDFF),
    onPrimaryContainer = Color(0xFF001E3D),
    secondary = Color(0xFFCC0000),        // Rojo UCV
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = Color(0xFF0284C7),         // Celeste acento
    onTertiary = Color.White,
    background = Color(0xFFF8F9FA),       // Fondo blanco suave
    onBackground = Color(0xFF1E293B),     // Texto oscuro nítido
    surface = Color(0xFFFFFFFF),          // Superficie blanca pura
    onSurface = Color(0xFF1E293B),        // Texto oscuro legible
    surfaceVariant = Color(0xFFF1F5F9),   // Contenedores claros
    onSurfaceVariant = Color(0xFF475569), // Texto secundario
    outline = Color(0xFFCBD5E1)
)

@Composable
fun ValleGOTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Forzamos la paleta institucional limpia de ValleGO para que el modo oscuro
    // del sistema del celular no rompa el diseño ni altere los contrastes
    MaterialTheme(
        colorScheme = ValleGOLightColorScheme,
        typography = Typography,
        content = content
    )
}
