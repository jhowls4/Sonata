package com.example.sonata.util

import android.util.Base64
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object ArtworkFetcher {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = LruCache<String, String>(100)

    suspend fun getArtworkUrl(artist: String?, title: String?): String? = withContext(Dispatchers.IO) {
        if (artist.isNullOrBlank() || title.isNullOrBlank()) return@withContext null

        val cacheKey = "${artist.lowercase()}_${title.lowercase()}"
        cache.get(cacheKey)?.let {
            Log.d("ArtworkFetcher", "Cache hit for $cacheKey: $it")
            return@withContext it
        }

        // Tier 1: iTunes
        val itunesUrl = fetchFromITunes(artist, title)
        if (itunesUrl != null) {
            val formatted = formatProxyUrl(itunesUrl)
            cache.put(cacheKey, formatted)
            return@withContext formatted
        }

        // Tier 2: Deezer Fallback
        val deezerUrl = fetchFromDeezer(artist, title)
        if (deezerUrl != null) {
            val formatted = formatProxyUrl(deezerUrl)
            cache.put(cacheKey, formatted)
            return@withContext formatted
        }

        null
    }

    private fun formatProxyUrl(url: String): String {
        val encoded = Base64.encodeToString(url.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return "mp:external/$encoded"
    }

    private fun fetchFromITunes(artist: String, title: String): String? {
        try {
            val query = URLEncoder.encode("$artist $title", "UTF-8")
            val url = "https://itunes.apple.com/search?term=$query&entity=song&limit=1"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    val jsonResponse = json.parseToJsonElement(body).jsonObject
                    val results = jsonResponse["results"]?.jsonArray
                    if (results != null && results.isNotEmpty()) {
                        val artworkUrl = results[0].jsonObject["artworkUrl100"]?.jsonPrimitive?.content
                        return artworkUrl?.replace("100x100bb.jpg", "600x600bb.jpg")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ArtworkFetcher", "iTunes search failed", e)
        }
        return null
    }

    private fun fetchFromDeezer(artist: String, title: String): String? {
        try {
            val query = URLEncoder.encode("$artist $title", "UTF-8")
            val url = "https://api.deezer.com/search?q=$query&limit=1"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    val jsonResponse = json.parseToJsonElement(body).jsonObject
                    val data = jsonResponse["data"]?.jsonArray
                    if (data != null && data.isNotEmpty()) {
                        return data[0].jsonObject["album"]?.jsonObject?.get("cover_big")?.jsonPrimitive?.content
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ArtworkFetcher", "Deezer search failed", e)
        }
        return null
    }
}
