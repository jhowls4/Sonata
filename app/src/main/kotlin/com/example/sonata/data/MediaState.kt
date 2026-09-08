package com.example.sonata.data

data class MediaState(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artUri: String? = null,
    val duration: Long = 0,
    val progress: Long = 0,
    val isPlaying: Boolean = false,
    val packageName: String? = null,
    val appName: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val remainingTime: Long get() = (duration - progress).coerceAtLeast(0)
}
