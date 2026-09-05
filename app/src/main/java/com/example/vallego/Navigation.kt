package com.example.vallego

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AuthRepository
import com.example.vallego.features.admin.AdminHomeScreen
import com.example.vallego.features.auth.AuthRoute
import com.example.vallego.features.buyer.BuyerHomeScreen
import com.example.vallego.features.seller.SellerDashboardScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MainNavigation(
    authRepository: AuthRepository = koinInject()
) {
    val isAuthenticated by authRepository.isAuthenticated.collectAsState()
    val currentProfile by authRepository.currentProfile.collectAsState()
    val scope = rememberCoroutineScope()

    if (!isAuthenticated || currentProfile == null) {
        AuthRoute(
            onAuthSuccess = { /* State triggers automatic recomposition */ },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
        )
    } else {
        val profile = currentProfile!!
        val onSignOut: () -> Unit = {
            scope.launch {
                authRepository.signOut()
            }
        }

        when (profile.role) {
            UserRole.COMPRADOR -> {
                BuyerHomeScreen(
                    profile = profile,
                    onSignOut = onSignOut,
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            UserRole.EMPRENDEDOR -> {
                SellerDashboardScreen(
                    profile = profile,
                    onSignOut = onSignOut,
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            UserRole.ADMIN -> {
                AdminHomeScreen(
                    profile = profile,
                    onSignOut = onSignOut,
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            UserRole.SUSPENDED, UserRole.SUSPENDED_BUYER -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tu cuenta ha sido suspendida. Contacta a soporte institucional Valle-Go.")
                }
            }
        }
    }
}
