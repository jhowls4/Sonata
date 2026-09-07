package com.example.sonata.rpc

import android.util.Log
import okhttp3.*
import okio.ByteString
import kotlinx.serialization.json.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import com.example.sonata.data.*
import com.example.sonata.util.StringTemplateParser
import java.util.Locale

class DiscordRpcManager(private val token: String) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var heartbeatInterval: Long = 41250
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    fun getToken(): String = token

    fun connect() {
        val request = Request.Builder()
            .url("wss://gateway.discord.gg/?v=10&encoding=json")
            .header("User-Agent", "Sonata/1.0 (Android)")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("DiscordRpc", "WebSocket Opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = Json.parseToJsonElement(text).jsonObject
                val op = json["op"]?.jsonPrimitive?.int
                when (op) {
                    10 -> { // Hello
                        heartbeatInterval = json["d"]?.jsonObject?.get("heartbeat_interval")?.jsonPrimitive?.long ?: 41250
                        identify(webSocket)
                        startHeartbeat()
                    }
                    11 -> { // Heartbeat ACK
                        Log.d("DiscordRpc", "Heartbeat ACK received")
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d("DiscordRpc", "WebSocket Closing: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("DiscordRpc", "WebSocket Failure", t)
            }
        })
    }

    private fun identify(webSocket: WebSocket) {
        val payload = buildJsonObject {
            put("op", 2)
            put("d", buildJsonObject {
                put("token", token)
                put("properties", buildJsonObject {
                    put("os", "android")
                    put("browser", "Sonata")
                    put("device", "Sonata")
                })
                put("compress", false)
                put("large_threshold", 250)
            })
        }
        webSocket.send(payload.toString())
        isConnected = true
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isConnected) {
                delay(heartbeatInterval)
                webSocket?.send(buildJsonObject {
                    put("op", 1)
                    put("d", JsonNull)
                }.toString())
            }
        }
    }

    fun updateStatus(
        config: RpcCustomizationConfig,
        title: String?,
        artist: String?,
        album: String?,
        artUri: String?,
        isPlaying: Boolean,
        durationMillis: Long? = null,
        progressMillis: Long? = null,
        appName: String? = "Sonata"
    ) {
        if (!isConnected) return

        val activity = if (isPlaying && title != null) {
            val durationStr = durationMillis?.let { formatTime(it) }
            val progressStr = progressMillis?.let { formatTime(it) }

            buildJsonObject {
                put("name", "Sonata")
                put("type", config.activityType)
                put("application_id", config.applicationId)
                put("details", StringTemplateParser.parse(config.detailsTemplate, title, artist, album, appName, durationStr, progressStr))
                put("state", StringTemplateParser.parse(config.stateTemplate, title, artist, album, appName, durationStr, progressStr))
                
                if (config.timestampMode != TimestampMode.OFF) {
                    put("timestamps", buildJsonObject {
                        val now = System.currentTimeMillis()
                        when (config.timestampMode) {
                            TimestampMode.ELAPSED -> {
                                progressMillis?.let { put("start", now - it) }
                            }
                            TimestampMode.REMAINING -> {
                                if (durationMillis != null && progressMillis != null) {
                                    put("end", now + (durationMillis - progressMillis))
                                }
                            }
                            else -> {}
                        }
                    })
                }

                put("assets", buildJsonObject {
                    val largeImage = when (config.coverArtSource) {
                        CoverArtSource.DYNAMIC -> artUri ?: config.fallbackAssetKey
                        CoverArtSource.CUSTOM -> config.customImageUrl ?: config.fallbackAssetKey
                        CoverArtSource.ASSET_KEY -> config.fallbackAssetKey
                    }
                    put("large_image", largeImage)
                    put("large_text", StringTemplateParser.parse(config.largeImageHoverTemplate, title, artist, album, appName, durationStr, progressStr))
                    
                    if (config.showSmallBadge) {
                        put("small_image", config.fallbackAssetKey)
                        put("small_text", StringTemplateParser.parse(config.smallImageHoverTemplate, title, artist, album, appName, durationStr, progressStr))
                    }
                })

                if (config.buttons.isNotEmpty()) {
                    put("buttons", buildJsonArray {
                        config.buttons.take(2).forEach { btn ->
                            add(btn.label)
                        }
                    })
                    // Note: Gateway presence update buttons are often just labels or handled differently.
                    // But standard Rich Presence activities in Gateway v10 can take metadata for buttons.
                    // Actually, for Gateway presence update, 'buttons' is an array of strings (labels) 
                    // and you can't really provide URLs easily there unless using the 'metadata' or 'secrets'.
                    // However, we will store them as strings for now or try the object format.
                }

                put("metadata", buildJsonObject {
                    put("album_name", album ?: "")
                    put("artist_name", artist ?: "")
                    if (config.buttons.isNotEmpty()) {
                        put("button_urls", buildJsonArray {
                            config.buttons.take(2).forEach { btn ->
                                add(StringTemplateParser.parse(btn.urlTemplate, title, artist, album, appName, durationStr, progressStr))
                            }
                        })
                    }
                })
            }
        } else {
            null
        }

        val payload = buildJsonObject {
            put("op", 3)
            put("d", buildJsonObject {
                put("since", JsonNull)
                put("activities", buildJsonArray {
                    if (activity != null) add(activity)
                })
                put("status", "online")
                put("afk", false)
            })
        }
        webSocket?.send(payload.toString())
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }

    fun disconnect() {
        isConnected = false
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Normal Closure")
    }
}
