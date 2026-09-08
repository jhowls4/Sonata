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
import kotlinx.coroutines.*

class MediaNotificationListenerService : NotificationListenerService() {

    private lateinit var mediaSessionManager: MediaSessionManager
    private val controllers = mutableMapOf<MediaSession.Token, MediaController>()
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var debounceJob: Job? = null
    private var lastTitle: String? = null
    
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { 
        registerControllers()
        updateRpc(force = true)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateRpc(force = true)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateRpc(force = false)
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
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateRpc(force = true)
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        registerControllers()
        updateRpc(force = true)
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

    private fun updateRpc(force: Boolean = false) {
        // Re-register to catch new sessions
        registerControllers()

        val activeController = controllers.values.firstOrNull { 
            it.playbackState?.state == PlaybackState.STATE_PLAYING 
        }

        val currentTitle = activeController?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val isPlaying = activeController != null
        
        if (!isPlaying) {
            lastTitle = null
        } else {
            lastTitle = currentTitle
        }

        // Always perform update immediately; the RpcForegroundService will handle debouncing
        performUpdate(activeController)
    }

    private fun performUpdate(activeController: MediaController?) {
        val intent = Intent(this, RpcForegroundService::class.java).apply {
            if (activeController != null) {
                val metadata = activeController.metadata
                val playbackState = activeController.playbackState
                
                val appName = getFriendlyAppName(activeController.packageName, metadata)

                putExtra("title", metadata?.getString(MediaMetadata.METADATA_KEY_TITLE))
                putExtra("artist", metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST))
                putExtra("album", metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM))
                
                putExtra("duration", metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L)
                putExtra("progress", playbackState?.position ?: 0L)
                putExtra("isPlaying", playbackState?.state == PlaybackState.STATE_PLAYING)
                putExtra("packageName", activeController.packageName)
                putExtra("appName", appName)
            } else {
                putExtra("isPlaying", false)
            }
        }
        startForegroundService(intent)
    }

    private fun getFriendlyAppName(packageName: String, metadata: MediaMetadata?): String {
        val friendlyMap = mapOf(
            "com.spotify.music" to "Spotify",
            "com.google.android.apps.youtube.music" to "YouTube Music",
            "com.apple.android.music" to "Apple Music",
            "com.amazon.mp3" to "Amazon Music",
            "com.soundcloud.android" to "SoundCloud",
            "deezer.android.app" to "Deezer",
            "com.tidal.music" to "TIDAL",
            "org.videolan.vlc" to "VLC"
        )

        friendlyMap[packageName]?.let { return it }

        metadata?.let {
            it.getString(MediaMetadata.METADATA_KEY_WRITER)?.let { writer -> if (writer.isNotBlank()) return writer }
            it.getString(MediaMetadata.METADATA_KEY_COMPOSER)?.let { composer -> if (composer.isNotBlank()) return composer }
        }

        val pm = packageManager
        return try {
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            "Unknown Player"
        }
    }
}
