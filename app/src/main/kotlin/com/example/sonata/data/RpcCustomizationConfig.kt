package com.example.sonata.data

import kotlinx.serialization.Serializable

@Serializable
enum class TimestampMode {
    ELAPSED, REMAINING, OFF
}

@Serializable
enum class CoverArtSource {
    DYNAMIC, CUSTOM, ASSET_KEY
}

@Serializable
data class RpcButtonConfig(
    val label: String,
    val urlTemplate: String
)

@Serializable
data class RpcCustomizationConfig(
    val applicationId: String = "1274100694156644382",
    val activityType: Int = 2, // 0: Playing, 2: Listening, 3: Watching, 5: Competing
    val detailsTemplate: String = "{title}",
    val stateTemplate: String = "by {artist}",
    val largeImageHoverTemplate: String = "{album}",
    val smallImageHoverTemplate: String = "{app}",
    val timestampMode: TimestampMode = TimestampMode.ELAPSED,
    val coverArtSource: CoverArtSource = CoverArtSource.DYNAMIC,
    val customImageUrl: String? = null,
    val fallbackAssetKey: String = "sonata",
    val showSmallBadge: Boolean = true,
    val buttons: List<RpcButtonConfig> = listOf(
        RpcButtonConfig("Listen on {app}", "https://sonata.example.com/play")
    )
)
