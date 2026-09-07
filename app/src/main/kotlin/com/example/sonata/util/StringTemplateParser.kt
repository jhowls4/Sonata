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
            .replace("{title}", title ?: "")
            .replace("{artist}", artist ?: "")
            .replace("{album}", album ?: "")
            .replace("{app}", app ?: "")
            .replace("{duration}", duration ?: "")
            .replace("{progress}", progress ?: "")
            .trim()
    }
}
