package dev.shsplayer.ui.me

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SHS Player "Me" hub screen — 4 cards.
 *
 * Original SHS Player code (clean-room reimplementation). Cards:
 *   - Privacy Vault → opens ShsFeaturesActivity FEATURE_VAULT
 *   - Wi-Fi Transfer → opens ShsFeaturesActivity FEATURE_TRANSFER
 *   - About → opens TelegramScreen (developer info)
 *   - Settings → opens VLC's PreferencesActivity
 */
@Composable
fun MeScreen(
    onOpenVault: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Me",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        MeMenuCard(
            title = "Privacy Vault",
            subtitle = "PIN-protected encrypted file storage",
            icon = Icons.Default.Lock,
            gradientColors = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)),
            onClick = onOpenVault,
        )
        MeMenuCard(
            title = "Wi-Fi Transfer",
            subtitle = "Send & receive files over local Wi-Fi",
            icon = Icons.Default.Wifi,
            gradientColors = listOf(Color(0xFF0277BD), Color(0xFF29B6F6)),
            onClick = onOpenTransfer,
        )
        MeMenuCard(
            title = "About SHS Player",
            subtitle = "Developer info, community, donations",
            icon = Icons.Default.Info,
            gradientColors = listOf(Color(0xFFEF6C00), Color(0xFFFFB74D)),
            onClick = onOpenAbout,
        )
        MeMenuCard(
            title = "Settings",
            subtitle = "Player, audio, subtitles, decoder",
            icon = Icons.Default.Settings,
            gradientColors = listOf(Color(0xFF37474F), Color(0xFF607D8B)),
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun MeMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(gradientColors),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
