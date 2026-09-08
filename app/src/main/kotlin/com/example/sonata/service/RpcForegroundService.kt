package com.example.sonata.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.sonata.data.AccountManager
import com.example.sonata.data.DataStoreRepository
import com.example.sonata.rpc.DiscordRpcManager
import com.example.sonata.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class RpcForegroundService : Service() {
    private var rpcManagers = mutableListOf<DiscordRpcManager>()
    private val CHANNEL_ID = "SonataRpcChannel"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: DataStoreRepository
    private var updateJob: Job? = null
    private var lastTrackKey: String? = null

    override fun onCreate() {
        super.onCreate()
        repository = DataStoreRepository(this)
        createNotificationChannel()
        val accounts = AccountManager(this).getAccounts()
        accounts.forEach { account ->
            val manager = DiscordRpcManager(account.token)
            manager.connect()
            rpcManagers.add(manager)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action")
        val token = intent?.getStringExtra("token")

        when (action) {
            "CONNECT_ACCOUNT" -> {
                if (token != null && rpcManagers.none { it.getToken() == token }) {
                    val manager = DiscordRpcManager(token)
                    manager.connect()
                    rpcManagers.add(manager)
                }
            }
            "DISCONNECT_ACCOUNT" -> {
                if (token != null) {
                    val manager = rpcManagers.find { it.getToken() == token }
                    manager?.disconnect()
                    rpcManagers.remove(manager)
                }
            }
        }

        val isPlaying = intent?.getBooleanExtra("isPlaying", false) ?: false
        val title = if (isPlaying) intent?.getStringExtra("title") else null
        val artist = if (isPlaying) intent?.getStringExtra("artist") else null
        val album = if (isPlaying) intent?.getStringExtra("album") else null
        val duration = if (isPlaying) intent?.getLongExtra("duration", 0L) ?: 0L else 0L
        val progress = if (isPlaying) intent?.getLongExtra("progress", 0L) ?: 0L else 0L
        val appName = intent?.getStringExtra("appName") ?: "Sonata"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sonata Rich Presence")
            .setContentText(if (isPlaying) "Updating Discord status: $title" else "Waiting for music...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        val currentTrackKey = if (isPlaying) "${artist}_${title}" else null
        
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            if (isPlaying) {
                // Debounce metadata changes by 500ms to handle fast skipping
                if (currentTrackKey != lastTrackKey) {
                    delay(500)
                }
            }
            
            lastTrackKey = currentTrackKey
            val globalConfig = repository.getGlobalConfig().first()
            val managers = rpcManagers.toList()
            managers.forEach { manager ->
                val accountConfig = repository.getAccountConfig(manager.getToken()).first() ?: globalConfig
                manager.updateStatus(
                    config = accountConfig,
                    title = title,
                    artist = artist,
                    album = album,
                    isPlaying = isPlaying,
                    durationMillis = if (duration > 0) duration else null,
                    progressMillis = progress,
                    appName = appName
                )
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        rpcManagers.forEach { manager ->
            manager.disconnect()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Sonata RPC Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
