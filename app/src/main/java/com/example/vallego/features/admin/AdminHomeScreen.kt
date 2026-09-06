package com.example.vallego.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.ApplicationStatus
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Modal Crear Punto de Encuentro
    if (uiState.showCreateMeetingPointDialog) {
        var pointName by remember { mutableStateOf("") }
        var pavilion by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateMeetingPointDialog() },
            title = {
                Text("Nuevo Punto de Encuentro", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pointName,
                        onValueChange = { pointName = it },
                        label = { Text("Nombre del Punto (ej. Biblioteca)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pavilion,
                        onValueChange = { pavilion = it },
                        label = { Text("Pabellón / Sector (ej. Pabellón C)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Referencia (ej. Frente a torniquetes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createMeetingPoint(pointName, pavilion, description)
                    },
                    enabled = pointName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                ) {
                    Text("Guardar Punto")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreateMeetingPointDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Rechazar Solicitud de Vendedor
    if (uiState.selectedApplicationForRejection != null) {
        val application = uiState.selectedApplicationForRejection!!
        var reasonSelected by remember { mutableStateOf("Falta permiso de bienestar universitario") }
        val commonReasons = listOf(
            "Falta permiso de bienestar universitario",
            "Ubicación propuesta no autorizada",
            "Giro comercial saturado en este turno",
            "Información del estudiante incompleta"
        )

        AlertDialog(
            onDismissRequest = { viewModel.dismissRejectionDialog() },
            title = {
                Text("Rechazar Solicitud", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Indica el motivo para informar al estudiante ${application.applicantName}:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    commonReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = reasonSelected == reason,
                                onClick = { reasonSelected = reason }
                            )
                            Text(text = reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRejection(application.id, reasonSelected) },
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel de Administración",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                        Text(
                            text = "Campus ${profile.campus} • ${profile.fullName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
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
        ) {
            // Tabs Principales del Administrador
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == AdminTab.MEETING_POINTS,
                    onClick = { viewModel.setTab(AdminTab.MEETING_POINTS) },
                    text = { Text("Puntos (${uiState.meetingPoints.size})") },
                    icon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == AdminTab.SELLER_APPLICATIONS,
                    onClick = { viewModel.setTab(AdminTab.SELLER_APPLICATIONS) },
                    text = {
                        val pending = uiState.sellerApplications.count { it.status == ApplicationStatus.PENDIENTE }
                        Text(if (pending > 0) "Solicitudes ($pending)" else "Solicitudes")
                    },
                    icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == AdminTab.CAMPUS_METRICS,
                    onClick = { viewModel.setTab(AdminTab.CAMPUS_METRICS) },
                    text = { Text("Métricas") },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (uiState.selectedTab) {
                    AdminTab.MEETING_POINTS -> {
                        MeetingPointsTabContent(
                            meetingPoints = uiState.meetingPoints,
                            onToggle = { point -> viewModel.toggleMeetingPoint(point.id, point.isActive) },
                            onCreateClick = { viewModel.openCreateMeetingPointDialog() }
                        )
                    }
                    AdminTab.SELLER_APPLICATIONS -> {
                        SellerApplicationsTabContent(
                            applications = uiState.sellerApplications,
                            onApprove = { app -> viewModel.approveApplication(app.id) },
                            onReject = { app -> viewModel.openRejectionDialog(app) }
                        )
                    }
                    AdminTab.CAMPUS_METRICS -> {
                        CampusMetricsTabContent(metrics = uiState.metrics)
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingPointsTabContent(
    meetingPoints: List<CampusMeetingPoint>,
    onToggle: (CampusMeetingPoint) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Puntos Oficiales del Campus", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Solo los puntos activos aparecen en el checkout de los alumnos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(meetingPoints, key = { it.id }) { point ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (point.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (point.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (point.isActive) Color(0xFF2E7D32) else Color(0xFFC8102E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = point.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                point.pavilion?.let {
                                    Text(
                                        text = "📍 $it",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF003366)
                                    )
                                }
                                point.description?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = point.isActive,
                            onCheckedChange = { onToggle(point) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SellerApplicationsTabContent(
    applications: List<SellerApplication>,
    onApprove: (SellerApplication) -> Unit,
    onReject: (SellerApplication) -> Unit
) {
    if (applications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay solicitudes registradas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(applications, key = { it.id }) { app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = app.storeName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF003366)
                            )
                            Text(
                                text = "Categoría: ${app.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ApplicationStatusBadge(status = app.status)
                    }

                    HorizontalDivider()

                    Text(
                        text = "Postulante: ${app.applicantName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Correo: ${app.studentEmail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ubicación propuesta: ${app.proposedLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF003366)
                    )
                    Text(
                        text = "Propuesta: ${app.description}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (app.status == ApplicationStatus.PENDIENTE) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onReject(app) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Rechazar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onApprove(app) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Aprobar Emprendedor")
                            }
                        }
                    } else if (app.status == ApplicationStatus.RECHAZADA && app.rejectionReason != null) {
                        HorizontalDivider()
                        Text(
                            text = "Motivo de rechazo: ${app.rejectionReason}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8102E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CampusMetricsTabContent(metrics: com.example.vallego.domain.model.CampusMetrics) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF003366)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Telemetría en Vivo del Campus",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Monitoreo en tiempo real de transacciones contra entrega y actividad de puestos.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminMetricCard(
                title = "Ventas Hoy",
                value = "S/ %.2f".format(metrics.totalSalesToday),
                color = Color(0xFF003366),
                modifier = Modifier.weight(1.3f)
            )
            AdminMetricCard(
                title = "Pedidos Hoy",
                value = "${metrics.totalOrdersToday}",
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminMetricCard(
                title = "Puestos Activos",
                value = "${metrics.activeSellersCount}",
                color = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )
            AdminMetricCard(
                title = "Puntos Activos",
                value = "${metrics.activeMeetingPointsCount}",
                color = Color(0xFFF57C00),
                modifier = Modifier.weight(1f)
            )
            AdminMetricCard(
                title = "Postulaciones",
                value = "${metrics.pendingApplicationsCount}",
                color = Color(0xFFC8102E),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ApplicationStatusBadge(status: ApplicationStatus) {
    val (bgColor, textColor, text) = when (status) {
        ApplicationStatus.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        ApplicationStatus.APROBADA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Aprobado ✓")
        ApplicationStatus.RECHAZADA -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Rechazado")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
