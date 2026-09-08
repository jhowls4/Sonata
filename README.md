# 🎵 Sonata — Multi-Account Discord Media RPC for Android

**Sonata** is a lightweight, background-optimized Android application designed to bridge your local Android media playback with Discord Rich Presence. It broadcasts your playing music (Spotify, YouTube Music, local media players) live to your Discord status across single or multiple accounts using Discord Gateway WebSockets.

---

## ✨ Key Features

- **🔄 Multi-Account Support:** Simultaneously manage and update Rich Presence on multiple Discord accounts using secure token authentication.
- **⚡ Background Performance:** Runs via an Android `ForegroundService` with automatic boot-up (`BOOT_COMPLETED`), keeping your status active without being killed by OS battery optimization.
- **🎧 Active Media Tracking:** Utilizes `MediaSessionManager` and `NotificationListenerService` to update status **only** when audio is actively playing, automatically clearing the status upon pausing or stopping.
- **🖼️ iTunes & Deezer Artwork Fallback:** Automatically fetches high-resolution album art via public Web APIs (iTunes & Deezer) to display artwork on Discord without local image uploading overhead.
- **⏱️ Live Progress & Timestamps:** Recalculates real-time audio progress and sends precise start/end timestamps to render Discord's native countdown bar.
- **🎨 Custom App & Status Names:** Easily override player names and customize the "Listening to..." activity text.
- **🔒 Secure Storage:** Sensitive user tokens are stored locally on-device using Android's `EncryptedSharedPreferences`.

---

## 🛠️ Built With

* **Language:** Kotlin
* **UI:** Jetpack Compose & Material Design 3
* **Networking:** OkHttp (WebSockets for Discord Gateway API `v10` & REST API requests)
* **Android Frameworks:** `MediaSessionManager`, `NotificationListenerService`, `ForegroundService`

---

## 🚀 Getting Started

### Prerequisites
* Android Studio (Ladybug or newer recommended)
* Android SDK 26+ (Android 8.0 Oreo or higher)
* Active Discord Application ID from the [Discord Developer Portal](https://docs.discord.com/developers/events/gateway)

### Installation & Build

1. Clone the repository:
   ```bash
   git clone [https://github.com/jhowls4/Sonata.git](https://github.com/jhowls4/Sonata.git)
