package com.example.vallego.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Reinicio detectado ($action). Levantando ValleGoPushService...")
            try {
                ValleGoPushService.start(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error iniciando ValleGoPushService tras reboot", e)
            }
        }
    }
}
