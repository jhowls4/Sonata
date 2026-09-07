package com.example.sonata.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaNotificationListenerService : NotificationListenerService() {

    private lateinit var mediaSessionManager: MediaSessionManager
    private val controllers = mutableMapOf<MediaSession.Token, MediaController>()
    
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { 
        registerControllers()
        updateRpc()
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateRpc()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateRpc()
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSessionManager.addOnActiveSessionsChangedListener(
            sessionsChangedListener,
            ComponentName(this, MediaNotificationListenerService::class.java)
        )
    }

    override fun onDestroy() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        unregisterControllers()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateRpc()
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        registerControllers()
        updateRpc()
    }

    override fun onListenerDisconnected() {
        unregisterControllers()
        super.onListenerDisconnected()
    }

    private fun registerControllers() {
        val activeSessions = mediaSessionManager.getActiveSessions(
            ComponentName(this, MediaNotificationListenerService::class.java)
        )
        activeSessions.forEach { controller ->
            if (!controllers.containsKey(controller.sessionToken)) {
                controller.registerCallback(controllerCallback)
                controllers[controller.sessionToken] = controller
            }
        }
    }

    private fun unregisterControllers() {
        controllers.values.forEach { it.unregisterCallback(controllerCallback) }
        controllers.clear()
    }

    private fun updateRpc() {
        // Re-register to catch new sessions
        registerControllers()

        val activeController = controllers.values.firstOrNull { 
            it.playbackState?.state == PlaybackState.STATE_PLAYING 
        }

        val intent = Intent(this, RpcForegroundService::class.java).apply {
            if (activeController != null) {
                val metadata = activeController.metadata
                val playbackState = activeController.playbackState
                putExtra("title", metadata?.getString(MediaMetadata.METADATA_KEY_TITLE))
                putExtra("artist", metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST))
                putExtra("album", metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM))
                val artUri = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) ?:
                             metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
                putExtra("artUri", artUri)
                putExtra("duration", metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION))
                putExtra("progress", playbackState?.position)
                putExtra("isPlaying", true)
            } else {
                putExtra("isPlaying", false)
            }
        }
        startForegroundService(intent)
    }
}
