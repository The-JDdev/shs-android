<div align="center">

# 🎬 SHS Android

### VLC-Powered Multimedia Player with Privacy Vault, Wi-Fi Transfer & Live TV

**Fork of [js313/mx-vlc-android](https://github.com/js313/mx-vlc-android)** (which is itself a fork of [VLC for Android](https://github.com/videolan/vlc-android)) — rebranded and extended with custom SHS Player features.

Built by **Sajjad Hussain Shobuj (SHS)** from Bangladesh 🇧🇩

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)
[![Engine](https://img.shields.io/badge/Engine-LibVLC-FF8800?style=for-the-badge&logo=vlc&logoColor=white)](https://videolan.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Telegram](https://img.shields.io/badge/Telegram-Join%20Chat-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aamoviesofficial)

</div>

---

## 📖 Overview

**SHS Android** is a privacy-first, ad-free multimedia player for Android 7.0+ (API 24+). It uses **LibVLC** as the sole playback engine — no ExoPlayer, no proprietary components — and adds custom SHS Player features on top:

- 🔒 **Privacy Vault** — encrypted on-device vault with PIN + biometric unlock, secure-erase delete, lock-on-background (MX Player `PrivateFolder` pattern)
- 📡 **Wi-Fi File Transfer** — NanoHTTPD-based local transfer server with QR-paired auth, no cloud, no internet required
- 📺 **Live TV (IPTV)** — M3U/M3U8 parser with 10 bundled iptv-org playlists categorized into Bangladesh / Sports / News / Popular / Free
- 📱 **QR Scanner** — CameraX + MLKit for scanning Wi-Fi transfer URLs
- 🪟 **PiP (Picture-in-Picture)** — backward compatible to Android 7.0 (API 24+), graceful fallback to background playback on unsupported devices
- 🎨 **Glassmorphism UI kit** — translucent frosted-glass surfaces with vibrant Google Material palette (optional, for custom screens)

---

## 🙏 Credits & Attribution

This project is a fork of:

1. **[js313/mx-vlc-android](https://github.com/js313/mx-vlc-android)** — original MIT-licensed fork that personalized VLC for Android with MX Player-style features. **All credit for the VLC integration, build system, medialibrary wiring, and base UI goes to the original author `js313`.**

2. **[VLC for Android](https://github.com/videolan/vlc-android)** by the VideoLAN team — the underlying VLC engine, LibVLC, medialibrary, and most of the playback / library / browser code is theirs. Licensed under GPLv2.

3. **Custom SHS Player features** (in `application/shs-features/` module) — written from scratch by **Sajjad Hussain Shobuj (SHS)**, MIT-licensed:
   - `dev.shsplayer.features.liveTv` — M3U parser + IPTV channel browser
   - `dev.shsplayer.features.privacyVault` — vault with PIN, biometric, secure-erase, lock-on-bg
   - `dev.shsplayer.features.wifiTransfer` — NanoHTTPD transfer server + QR generator
   - `dev.shsplayer.features.qrScanner` — CameraX + MLKit barcode scanner
   - `dev.shsplayer.features.pip` — backward-compatible PiP helper (API 24+)

The in-app experience is rebranded as **SHS Player**. The VLC name and logo are respected — only the launcher icon and app name strings have been changed. All original copyright notices, license headers, and `COPYING` files from upstream are preserved unchanged.

---

## 📦 Project Structure

```
shs-android/
├── application/
│   ├── app/                    # Application module (launches the app)
│   ├── vlc-android/            # Main VLC playback module (from upstream)
│   ├── shs-features/           # 🆕 SHS Player custom features (NEW in this fork)
│   │   └── src/main/java/dev/shsplayer/features/
│   │       ├── liveTv/         # M3U parser + IPTV browser
│   │       ├── privacyVault/   # PIN + biometric vault
│   │       ├── wifiTransfer/   # NanoHTTPD server + QR
│   │       ├── qrScanner/      # CameraX + MLKit
│   │       └── pip/            # Backward-compatible PiP helper
│   ├── resources/              # Shared resources (icons, strings, themes)
│   ├── medialibrary/           # Media database
│   ├── tools/                  # Build tools
│   ├── television/             # Android TV variant
│   ├── donations/              # Donation screens
│   ├── moviepedia/             # Movie metadata
│   └── remote-access-*/        # Remote access (server + client)
├── medialibrary/               # Medialibrary native module
├── libvlcjni/                  # LibVLC JNI bindings
├── buildsystem/                # Build scripts
├── COPYING                     # GPLv2 license (from upstream VLC)
└── README.md                   # This file
```

---

## 🔧 Building

### Prerequisites

- **JDK 17** (set `JAVA_HOME`)
- **Android SDK** with `platform-android-36` and `build-tools;36.0.0`
- **Android NDK** (for native LibVLC build — but pre-built `.aar` artifacts are used by default)
- ~6 GB free disk for Gradle caches

### Build commands

```bash
# Clone
git clone https://github.com/The-JDdev/shs-android.git
cd shs-android

# Build a debug APK (universal)
./gradlew :application:app:assembleDebug

# Build release APKs (per-ABI splits)
./gradlew :application:app:assembleRelease
```

### Build outputs

```
application/app/build/outputs/apk/debug/app-debug.apk
application/app/build/outputs/apk/release/app-arm64-v8a-release.apk
application/app/build/outputs/apk/release/app-universal-release.apk
```

---

## 🆕 SHS Features Module

The `application/shs-features/` module is the only code added on top of upstream. It is a self-contained Gradle library module that depends only on AndroidX, Compose, NanoHTTPD, ZXing, CameraX, MLKit, and Accompanist — no dependency on the VLC module, so it can be reused in any other Android project.

### Live TV

```kotlin
// Parse an M3U playlist
val channels = M3uParser.parse(context, "https://iptv-org.github.io/iptv/countries/bd.m3u")

// Get the bundled default playlists
val playlists = DefaultIptvPlaylists.playlists  // 10 iptv-org playlists categorized
val bdPlaylists = DefaultIptvPlaylists.forCategory(IptvCategory.BANGLADESH)
```

### Privacy Vault

```kotlin
// First-time setup
VaultManager.setupVault(context, "1234", "What is your pet's name?", "tom")

// Verify password
if (VaultManager.verifyPassword(context, "1234")) { /* unlock */ }

// Move a video into the vault (deletes from MediaStore)
val vaultFile = VaultManager.moveToVault(context, videoUri, "video")

// Restore back to gallery
VaultManager.restoreFromVault(context, vaultFile)

// Secure-erase and delete
VaultManager.deleteFromVault(context, vaultFile)
```

### Wi-Fi File Transfer

```kotlin
// Start the receiver server
val server = WifiTransferServer(context, port = 12345, onFileReceived = { file ->
    Log.d("Transfer", "Received: ${file.name}")
})
server.start()

// Show the QR code (sender scans this)
val ip = WifiTransferUtils.getWifiIpAddress(context) ?: "0.0.0.0"
val url = "http://$ip:12345?token=${server.authToken}"
val qrBitmap = WifiTransferUtils.generateQrCodeBitmap(url)
```

### QR Scanner (Compose)

```kotlin
QrScannerView(
    onResult = { scannedValue ->
        // scannedValue is the URL/text encoded in the QR code
    },
    modifier = Modifier.fillMaxSize(),
)
```

### PiP (backward compatible to API 24)

```kotlin
override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    // Returns true if PiP was entered, false if device doesn't support PiP
    // (in which case you should fall back to background play via foreground service)
    val enteredPip = PipHelper.tryEnterPip(
        activity = this,
        videoWidth = player.videoWidth,
        videoHeight = player.videoHeight,
        isPlaying = player.isPlaying,
    )
    if (!enteredPip) {
        // Fall back: hand off to background service
        BackgroundPlaybackService.startPlayback(this, currentUri)
    }
}
```

---

## 🔐 Privacy & Security

- **No data collection.** SHS Android does not collect, transmit, or share any personal information.
- **All processing is on-device.** Media decoding, thumbnail generation, subtitle parsing, QR decoding — everything happens locally.
- **Wi-Fi File Transfer stays on your LAN.** The NanoHTTPD server binds to your local Wi-Fi interface; there is no cloud relay.
- **Privacy Vault files are stored in app-private storage** at `context.filesDir/vault/{videos,music}/` — invisible to other apps and to the gallery. They are deleted if you uninstall the app (so back them up first!).
- **Secure-erase on delete** — vault files are overwritten with zeroes before being deleted (for files < 100 MB).

---

## 📋 Permissions Explained

| Permission | Why |
|---|---|
| `INTERNET` | Streaming (HLS/DASH/RTSP), IPTV, Wi-Fi transfer server. |
| `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` (Android 13+) | Read your media. |
| `READ_EXTERNAL_STORAGE` (≤ Android 12) | Read media on older Android. |
| `MANAGE_EXTERNAL_STORAGE` | Vault file operations on Android 10 and below. |
| `CAMERA` | QR scanner. |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` / `ACCESS_NETWORK_STATE` / `NEARBY_WIFI_DEVICES` (Android 13+) | Wi-Fi file transfer, device discovery. |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Required by Android to expose Wi-Fi BSSID/SSID for QR transfer format. Never stored or transmitted. |
| `USE_BIOMETRIC` / `USE_FINGERPRINT` | Privacy Vault biometric unlock. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Background playback notification. |
| `WAKE_LOCK` | Keep CPU awake during playback. |
| `VIBRATE` | Haptic feedback on player controls. |

---

## 🌍 Internationalization

Inherits VLC's translation infrastructure — 40+ languages including Bengali (`bn`), Hindi (`hi`), Urdu (`ur`), Punjabi (`pa`), Tamil (`ta`), and more.

---

## 🤝 Contributing

1. Fork the repo
2. Branch from `master`
3. Run `./gradlew ktlintCheck` before committing
4. Open a Pull Request with a clear description

---

## 📄 License

- **Upstream VLC code** (everything in `application/vlc-android/`, `application/resources/`, `medialibrary/`, `libvlcjni/`, etc.): **GPLv2** — see [`COPYING`](COPYING)
- **SHS Player custom features** (`application/shs-features/`): **MIT** — see [`application/shs-features/LICENSE`](application/shs-features/LICENSE)
- **Original `js313/mx-vlc-android` modifications**: **MIT** (per the upstream repo's license)

When redistributing, you must comply with both GPLv2 (for VLC code) and MIT (for SHS features). The GPLv2 is the more restrictive of the two and applies to the combined work.

---

## 💬 Community

| Platform | Link |
|---|---|
| 📱 **Telegram channel** | [t.me/aamoviesofficial](https://t.me/aamoviesofficial) |
| 📘 **Facebook** | [fb.com/itsshsshobuj](https://fb.com/itsshsshobuj) |
| 💻 **GitHub** | [github.com/The-JDdev](https://github.com/The-JDdev) |
| ✉️ **Email** | `thejddev.official@gmail.com` |

---

## 💎 Support the Project

If SHS Android has empowered your workflow, please consider supporting the project:

- **bKash** (Bangladesh): `01310211442`
- **Telegram**: [@aamoviesadmin](https://t.me/aamoviesadmin)

Star ⭐ the repo to help others discover it.

---

<div align="center">

**Built with 🔥 from Bangladesh 🇧🇩**

*Forked from [js313/mx-vlc-android](https://github.com/js313/mx-vlc-android) (MIT) · Powered by [LibVLC](https://videolan.org/) · SHS Player features by Sajjad Hussain Shobuj (SHS)*

</div>
