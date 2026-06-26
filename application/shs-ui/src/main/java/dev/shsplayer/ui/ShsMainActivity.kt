package dev.shsplayer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.shsplayer.features.ShsFeaturesActivity
import dev.shsplayer.ui.crash.ShsCrashActivity
import dev.shsplayer.ui.crash.ShsGlobalExceptionHandler
import dev.shsplayer.ui.me.MeScreen
import dev.shsplayer.ui.music.MusicScreen
import dev.shsplayer.ui.nav.ShsBottomNavBar
import dev.shsplayer.ui.nav.ShsTab
import dev.shsplayer.ui.splash.AnimatedSplashScreen
import dev.shsplayer.ui.telegram.TelegramScreen
import dev.shsplayer.ui.theme.ShsPlayerTheme

/**
 * SHS Player — main activity (Compose host).
 *
 * Original SHS Player code (clean-room reimplementation, adapted for VLC engine).
 *
 * Single-activity host with a 5-tab bottom navigation:
 *   - Videos → launches VLC's StartActivity (video browser)
 *   - Music → MusicScreen (Compose)
 *   - Watch TV → ShsFeaturesActivity FEATURE_LIVE_TV (IPTV browser)
 *   - Me → MeScreen (Compose)
 *   - Telegram → TelegramScreen (Compose)
 *
 * Shows AnimatedSplashScreen on first launch (1.8s) then the main UI.
 */
class ShsMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Install crash handler
        Thread.setDefaultUncaughtExceptionHandler(
            ShsGlobalExceptionHandler(applicationContext, ShsCrashActivity::class.java),
        )
        enableEdgeToEdge()
        setContent {
            ShsPlayerTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    AnimatedSplashScreen(onAnimationComplete = { showSplash = false })
                } else {
                    ShsMainContent()
                }
            }
        }
    }
}

@Composable
private fun ShsMainContent() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ShsTab.VIDEOS) }

    Scaffold(
        bottomBar = {
            ShsBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == ShsTab.VIDEOS) {
                        // Launch VLC's video browser via package name (avoid hard dep on vlc-android module)
                        try {
                            val intent = Intent().apply {
                                setClassName(context.packageName, "org.videolan.vlc.StartActivity")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback: show inline placeholder
                            selectedTab = tab
                        }
                    } else {
                        selectedTab = tab
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                ShsTab.VIDEOS -> VideosPlaceholder()
                ShsTab.MUSIC -> MusicScreen()
                ShsTab.LIVE_TV -> {
                    LaunchedEffect(Unit) {
                        ShsFeaturesActivity.launch(context, ShsFeaturesActivity.FEATURE_LIVE_TV)
                    }
                    LoadingPlaceholder()
                }
                ShsTab.ME -> MeScreen(
                    onOpenVault = {
                        ShsFeaturesActivity.launch(context, ShsFeaturesActivity.FEATURE_VAULT)
                    },
                    onOpenTransfer = {
                        ShsFeaturesActivity.launch(context, ShsFeaturesActivity.FEATURE_TRANSFER)
                    },
                    onOpenAbout = { selectedTab = ShsTab.TELEGRAM },
                    onOpenSettings = {
                        try {
                            val intent = Intent().apply {
                                setClassName(context.packageName, "org.videolan.vlc.gui.preferences.PreferencesActivity")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                )
                ShsTab.TELEGRAM -> TelegramScreen()
            }
        }
    }
}

@Composable
private fun VideosPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = "Tap Videos tab to open VLC's video browser",
            modifier = Modifier.padding(32.dp),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}
