package com.example.sonata.util

object StringTemplateParser {
    fun parse(
        template: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        app: String? = null,
        duration: String? = null,
        progress: String? = null
    ): String {
        return template
            .replace("{song_name}", title ?: "")
            .replace("{title}", title ?: "")
            .replace("{artist_name}", artist ?: "")
            .replace("{artist}", artist ?: "")
            .replace("{album_name}", album ?: "")
            .replace("{album}", album ?: "")
            .replace("{player_name}", app ?: "")
            .replace("{app}", app ?: "")
            .replace("{duration}", duration ?: "")
            .replace("{progress}", progress ?: "")
            .trim()
    }
}
