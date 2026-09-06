package com.example.vallego.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.vallego.data.repository.RemoteOrderDto
import com.example.vallego.data.repository.RemoteOrderItemDto
import com.example.vallego.data.repository.RemoteSubOrderDto
import com.example.vallego.data.repository.ProductBasicDto
import com.example.vallego.domain.model.UserProfile
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ValleGoPushService : Service(), KoinComponent {

    private val postgrest: Postgrest by inject()
    private val auth: Auth by inject()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    private var monitoringJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val seenSellerOrderIds = mutableSetOf<String>()
    private var isSellerFirstRun = true

    private val lastKnownBuyerStatuses = mutableMapOf<String, String>()
    private var isBuyerFirstRun = true
    private val productNameCache = ConcurrentHashMap<String, String>()

    override fun onCreate() {
        super.onCreate()
        ValleGoNotificationHelper.createNotificationChannels(this)
        val ongoingNotification = ValleGoNotificationHelper.getForegroundServiceNotification(
            context = this,
            title = "Valle-Go",
            content = "Monitoreando pedidos y notificaciones en campus"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ValleGoNotificationHelper.SERVICE_NOTIFICATION_ID,
                ongoingNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(ValleGoNotificationHelper.SERVICE_NOTIFICATION_ID, ongoingNotification)
        }

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ValleGo:PushServiceWakeLock")

        Log.d(TAG, "ValleGoPushService iniciado en primer plano.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startOrderMonitoringLoop()
        return START_STICKY
    }

    private fun isAlreadyNotified(eventKey: String): Boolean {
        val prefs = getSharedPreferences("vallego_notifs_cache", Context.MODE_PRIVATE)
        return prefs.getBoolean(eventKey, false)
    }

    private fun markAsNotified(eventKey: String) {
        val prefs = getSharedPreferences("vallego_notifs_cache", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(eventKey, true).apply()
    }

    private suspend fun getOrderItemsSummary(orderId: String, subOrderId: String? = null): String {
        return try {
            val items = if (subOrderId != null) {
                val subItems = try {
                    postgrest.from("order_items")
                        .select { filter { eq("sub_order_id", subOrderId) } }
                        .decodeList<RemoteOrderItemDto>()
                } catch (_: Exception) {
                    emptyList()
                }
                if (subItems.isNotEmpty()) subItems else {
                    try {
                        postgrest.from("order_items")
                            .select { filter { eq("order_id", orderId) } }
                            .decodeList<RemoteOrderItemDto>()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            } else {
                try {
                    postgrest.from("order_items")
                        .select { filter { eq("order_id", orderId) } }
                        .decodeList<RemoteOrderItemDto>()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            if (items.isEmpty()) return ""

            val missingProductIds = items.map { it.productId }
                .filter { it.isNotBlank() && !productNameCache.containsKey(it) }
                .distinct()

            if (missingProductIds.isNotEmpty()) {
                try {
                    val prods = postgrest.from("products")
                        .select { filter { isIn("id", missingProductIds) } }
                        .decodeList<ProductBasicDto>()
                    for (p in prods) {
                        productNameCache[p.id] = p.name
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            items.joinToString(", ") { item ->
                val name = productNameCache[item.productId] ?: "Producto"
                "$name (x${item.quantity})"
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun startOrderMonitoringLoop() {
        if (monitoringJob?.isActive == true) {
            Log.d(TAG, "Bucle de monitoreo ya activo. Se omite duplicación.")
            return
        }
        monitoringJob = serviceScope.launch {
            Log.d(TAG, "Iniciando bucle de monitoreo de pedidos en segundo plano...")
            while (isActive) {
                try {
                    wakeLock?.acquire(3000L)
                    val user = auth.currentUserOrNull()
                    if (user != null) {
                        val userId = user.id
                        // Determinar rol del usuario desde profiles
                        val profile = runCatching {
                            postgrest.from("profiles")
                                .select {
                                    filter { eq("id", userId) }
                                }
                                .decodeSingleOrNull<UserProfile>()
                        }.getOrNull()

                        val roleStr = profile?.role?.name?.lowercase() ?: "comprador"

                        if (roleStr == "emprendedor" || roleStr == "admin") {
                            monitorSellerOrders(userId)
                        } else {
                            monitorBuyerOrders(userId)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error durante chequeo de pedidos: ${e.message}")
                } finally {
                    if (wakeLock?.isHeld == true) {
                        try { wakeLock?.release() } catch (_: Exception) {}
                    }
                }
                delay(4000)
            }
        }
    }

    private suspend fun monitorSellerOrders(sellerId: String) {
        val subOrders = try {
            postgrest.from("sub_orders")
                .select {
                    filter { eq("seller_id", sellerId) }
                }
                .decodeList<RemoteSubOrderDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val legacyOrders = try {
            postgrest.from("orders")
                .select {
                    filter { eq("seller_id", sellerId) }
                }
                .decodeList<RemoteOrderDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val existingSubOrderOrderIds = subOrders.map { it.orderId }.toSet()
        val missingFromSubOrders = legacyOrders.filter { it.id !in existingSubOrderOrderIds }
        val synthesizedSubs = missingFromSubOrders.map { ro ->
            RemoteSubOrderDto(
                id = ro.id,
                orderId = ro.id,
                sellerId = ro.sellerId ?: sellerId,
                subtotalAmount = ro.totalPrice,
                status = ro.status,
                paymentMethod = ro.paymentMethod,
                createdAt = ro.createdAt,
                updatedAt = ro.updatedAt
            )
        }

        val allSubs = (subOrders + synthesizedSubs).sortedByDescending { it.createdAt }

        if (isSellerFirstRun) {
            allSubs.forEach {
                seenSellerOrderIds.add(it.id)
                markAsNotified("seller_sub_${it.id}")
            }
            isSellerFirstRun = false
            return
        }

        for (sub in allSubs) {
            val eventKey = "seller_sub_${sub.id}"
            if (isAlreadyNotified(eventKey)) {
                seenSellerOrderIds.add(sub.id)
                continue
            }

            val isNew = !seenSellerOrderIds.contains(sub.id)
            val isPending = sub.status.lowercase() in listOf("pending", "pendiente")

            if (isNew && isPending) {
                seenSellerOrderIds.add(sub.id)
                markAsNotified(eventKey)
                val itemsSummary = getOrderItemsSummary(orderId = sub.orderId, subOrderId = sub.id)
                val totalStr = String.format(java.util.Locale.US, "%.2f", sub.subtotalAmount)
                val messageText = if (itemsSummary.isNotBlank()) {
                    "Has recibido un pedido de $itemsSummary por S/. $totalStr. Toca para atenderlo."
                } else {
                    "Has recibido un nuevo pedido por S/. $totalStr. Toca para atenderlo."
                }
                ValleGoNotificationHelper.showOrderNotification(
                    context = this@ValleGoPushService,
                    notificationId = sub.id.hashCode(),
                    title = "🔔 ¡Nuevo pedido recibido!",
                    message = messageText,
                    orderId = sub.orderId
                )
            } else {
                seenSellerOrderIds.add(sub.id)
            }
        }
    }

    private suspend fun monitorBuyerOrders(buyerId: String) {
        val orders = postgrest.from("orders")
            .select {
                filter { eq("buyer_id", buyerId) }
            }
            .decodeList<RemoteOrderDto>()

        if (isBuyerFirstRun) {
            orders.forEach {
                lastKnownBuyerStatuses[it.id] = it.status.lowercase()
                markAsNotified("buyer_order_${it.id}_${it.status.lowercase()}")
            }
            isBuyerFirstRun = false
            return
        }

        for (order in orders) {
            val currentStatus = order.status.lowercase()
            val previousStatus = lastKnownBuyerStatuses[order.id]

            if (previousStatus != null && previousStatus != currentStatus) {
                val eventKey = "buyer_order_${order.id}_$currentStatus"
                if (!isAlreadyNotified(eventKey)) {
                    markAsNotified(eventKey)
                    val itemsSummary = getOrderItemsSummary(order.id)
                    val summaryPart = if (itemsSummary.isNotBlank()) " ($itemsSummary)" else ""
                    when (currentStatus) {
                        "accepted", "aceptado", "in_preparation", "en_preparacion" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = order.id.hashCode(),
                                title = "👨‍🍳 Pedido en preparación",
                                message = "El vendedor comenzó a preparar tu pedido$summaryPart.",
                                orderId = order.id
                            )
                        }
                        "ready", "listo", "esperando_entrega" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = order.id.hashCode(),
                                title = "✅ ¡Tu pedido está listo!",
                                message = "Acércate a recoger tu pedido$summaryPart al punto de encuentro.",
                                orderId = order.id
                            )
                        }
                        "completed", "completado" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = order.id.hashCode(),
                                title = "🎉 ¡Pedido entregado!",
                                message = "Tu pedido$summaryPart ha sido completado exitosamente. ¡Buen provecho!",
                                orderId = order.id
                            )
                        }
                        "cancelled", "cancelado", "rejected", "rechazado" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = order.id.hashCode(),
                                title = "❌ Pedido cancelado",
                                message = "Tu pedido$summaryPart fue cancelado.",
                                orderId = order.id
                            )
                        }
                    }
                }
            }
            lastKnownBuyerStatuses[order.id] = currentStatus
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved invocado. Reprogramando ValleGoPushService...")
        try {
            val restartIntent = Intent(applicationContext, ValleGoPushService::class.java).also {
                it.setPackage(packageName)
            }
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    applicationContext,
                    1001,
                    restartIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    applicationContext,
                    1001,
                    restartIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reprogramando servicio en onTaskRemoved", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        if (wakeLock?.isHeld == true) {
            try { wakeLock?.release() } catch (_: Exception) {}
        }
        Log.d(TAG, "ValleGoPushService destruido.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ValleGoPushService"

        fun start(context: Context) {
            val intent = Intent(context, ValleGoPushService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ValleGoPushService::class.java)
            context.stopService(intent)
        }
    }
}
