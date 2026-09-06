package com.example.vallego.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.vallego.MainActivity
import com.example.vallego.R

object ValleGoNotificationHelper {

    const val CHANNEL_ORDERS = "vallego_orders_channel"
    const val CHANNEL_SERVICE = "vallego_service_channel"
    const val SERVICE_NOTIFICATION_ID = 9001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal para notificaciones inmediatas de pedidos (Alta prioridad, sonido y vibración)
            val orderChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "Pedidos y Actualizaciones Valle-Go",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevos pedidos, cambios de estado y entregas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Canal para el servicio en segundo plano (Baja prioridad, silencioso)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Servicio en Segundo Plano Valle-Go",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activa la escucha de pedidos en tiempo real"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(orderChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun showOrderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        orderId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (orderId != null) {
                putExtra("order_id", orderId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("ValleGoNotification", "Permiso de notificaciones denegado", e)
        }
    }

    fun getForegroundServiceNotification(
        context: Context,
        title: String = "Valle-Go Activo",
        content: String = "Escuchando actualizaciones de pedidos en campus"
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
