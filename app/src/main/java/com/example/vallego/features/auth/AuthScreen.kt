package com.example.vallego.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.UserRole
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthRoute(
    onAuthSuccess: (com.example.vallego.domain.model.UserProfile) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess, uiState.profile) {
        val profile = uiState.profile
        if (uiState.isSuccess && profile != null) {
            onAuthSuccess(profile)
        }
    }

    AuthScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onFullNameChange = viewModel::onFullNameChange,
        onPhoneChange = viewModel::onPhoneChange,
        onRoleChange = viewModel::onRoleChange,
        onTabSelected = viewModel::setLoginMode,
        onSubmit = viewModel::submit,
        onDismissError = viewModel::clearError,
        modifier = modifier
    )
}

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onTabSelected: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header UCV
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Valle-Go",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        ),
                        color = Color(0xFF003366) // Azul UCV
                    )
                    Text(
                        text = "Tu campus, tus antojos universitarios",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Tabs: Iniciar Sesión / Registro
                PrimaryTabRow(
                    selectedTabIndex = if (uiState.isLoginMode) 0 else 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = uiState.isLoginMode,
                        onClick = { onTabSelected(true) },
                        text = { Text("Iniciar Sesión", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = !uiState.isLoginMode,
                        onClick = { onTabSelected(false) },
                        text = { Text("Registrarse", fontWeight = FontWeight.SemiBold) }
                    )
                }

                // Error Banner
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Campo Correo
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    label = { Text("Correo Electrónico") },
                    placeholder = { Text("ejemplo@correo.com") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = uiState.email.isNotBlank() && !uiState.isEmailValid,
                    supportingText = {
                        Text(
                            text = if (uiState.isEmailValid) {
                                "✓ Correo electrónico válido"
                            } else {
                                "Ingresa un correo electrónico válido"
                            },
                            color = if (uiState.isEmailValid) {
                                Color(0xFF2E7D32)
                            } else if (uiState.email.isNotBlank()) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    trailingIcon = {
                        if (uiState.isEmailValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Válido",
                                tint = Color(0xFF2E7D32)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo Contraseña
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Contraseña") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (uiState.isLoginMode) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (uiState.canSubmit) onSubmit()
                        }
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Campos Adicionales de Registro
                AnimatedVisibility(
                    visible = !uiState.isLoginMode,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.fullName,
                            onValueChange = onFullNameChange,
                            label = { Text("Nombre y Apellidos") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.phone,
                            onValueChange = onPhoneChange,
                            label = { Text("Celular (para Yape / Plin / Contacto)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "¿Cómo usarás Valle-Go?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.selectedRole == UserRole.COMPRADOR,
                                onClick = { onRoleChange(UserRole.COMPRADOR) },
                                label = { Text("Comprar (Estudiante)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.selectedRole == UserRole.EMPRENDEDOR,
                                onClick = { onRoleChange(UserRole.EMPRENDEDOR) },
                                label = { Text("Vender (Negocio / Emprendedor)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón Principal
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                    enabled = uiState.canSubmit,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.isLoginMode) "Iniciar Sesión" else "Crear Cuenta Valle-Go",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}