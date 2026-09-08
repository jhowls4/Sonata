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
enum class DiscordActivityType(val value: Int) {
    PLAYING(0),
    LISTENING(2),
    WATCHING(3),
    COMPETING(5)
}

@Serializable
data class RpcButtonConfig(
    val label: String,
    val urlTemplate: String,
    val isEnabled: Boolean = true
)

@Serializable
data class RpcPreset(
    val name: String,
    val config: RpcCustomizationConfig
)

@Serializable
data class RpcCustomizationConfig(
    val applicationId: String = "1274100694156644382",
    val activityType: DiscordActivityType = DiscordActivityType.LISTENING,
    val detailsTemplate: String = "{title}",
    val stateTemplate: String = "by {artist}",
    val largeImageHoverTemplate: String = "{album}",
    val smallImageHoverTemplate: String = "{app}",
    val timestampMode: TimestampMode = TimestampMode.ELAPSED,
    val coverArtSource: CoverArtSource = CoverArtSource.DYNAMIC,
    val customImageUrl: String? = null,
    val customActivityName: String? = null,
    val fallbackAssetKey: String = "sonata",
    val showSmallBadge: Boolean = true,
    val buttons: List<RpcButtonConfig> = listOf(
        RpcButtonConfig("Listen on {app}", "https://sonata.example.com/play")
    ),
    val buttonsEnabled: Boolean = true
)
