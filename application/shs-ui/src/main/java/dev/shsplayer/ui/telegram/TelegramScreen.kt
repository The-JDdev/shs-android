package dev.shsplayer.ui.telegram

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
 * SHS Player developer info screen — Telegram, Facebook, email, bKash donation.
 *
 * Original SHS Player code (clean-room reimplementation). Shows developer
 * photo placeholder + 4 contact cards + bKash donation card with tap-to-copy.
 */
@Composable
fun TelegramScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "About SHS Player",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        // Developer photo placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF6D00), Color(0xFF7C4DFF)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "SHS",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "Sajjad Hussain Shobuj (SHS)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Built with ❤ from Bangladesh 🇧🇩",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        ContactCard(
            title = "Telegram",
            subtitle = "t.me/aamoviesofficial",
            icon = Icons.Default.Send,
            color = Color(0xFF2CA5E0),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/aamoviesofficial"))
                context.startActivity(intent)
            },
        )
        ContactCard(
            title = "Facebook",
            subtitle = "fb.com/itsshsshobuj",
            icon = Icons.Default.Face,
            color = Color(0xFF1877F2),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://fb.com/itsshsshobuj"))
                context.startActivity(intent)
            },
        )
        ContactCard(
            title = "Email",
            subtitle = "thejddev.official@gmail.com",
            icon = Icons.Default.Email,
            color = Color(0xFFEA4335),
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:thejddev.official@gmail.com"))
                context.startActivity(intent)
            },
        )
        ContactCard(
            title = "GitHub",
            subtitle = "github.com/The-JDdev",
            icon = Icons.Default.Code,
            color = Color(0xFF333333),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/The-JDdev"))
                context.startActivity(intent)
            },
        )
        Spacer(Modifier.height(16.dp))

        // bKash donation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2136E)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Support Development",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "If SHS Player helps you, consider supporting the solo developer.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "bKash: 01310211442",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("bKash", "01310211442"),
                        )
                        android.widget.Toast.makeText(context, "Copied: 01310211442", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) {
                    Text("Tap to copy bKash number")
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
