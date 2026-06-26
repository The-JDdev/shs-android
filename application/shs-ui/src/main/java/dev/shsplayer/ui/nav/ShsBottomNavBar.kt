package dev.shsplayer.ui.nav

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * SHS Player bottom navigation bar — 5 tabs.
 *
 * Original SHS Player code (clean-room reimplementation). Tab order:
 *   1. Videos (VideoLibrary icon)
 *   2. Music (MusicNote icon)
 *   3. Watch TV (Tv icon)
 *   4. Me (AccountCircle icon)
 *   5. Telegram (Email icon)
 */
enum class ShsTab(val label: String, val icon: ImageVector) {
    VIDEOS("Videos", Icons.Default.VideoLibrary),
    MUSIC("Music", Icons.Default.MusicNote),
    LIVE_TV("Watch TV", Icons.Default.Tv),
    ME("Me", Icons.Default.AccountCircle),
    TELEGRAM("Telegram", Icons.Default.Email),
}

@Composable
fun ShsBottomNavBar(
    selectedTab: ShsTab,
    onTabSelected: (ShsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        ShsTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = { Text(tab.label) },
            )
        }
    }
}
