package com.example.sonata.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sonata.service.RpcForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val serviceIntent = Intent(context, RpcForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
