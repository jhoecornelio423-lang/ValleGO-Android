package com.example.vallego

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.vallego.core.notification.ValleGoPushService
import com.example.vallego.theme.ValleGOTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ValleGoPushService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Solicitar permiso de notificaciones para Android 13+ (API 33+)
        checkAndRequestNotificationPermission()

        // Iniciar el servicio en segundo plano de sincronización de pedidos
        try {
            ValleGoPushService.start(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error iniciando ValleGoPushService", e)
        }

        setContent {
            ValleGOTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}
