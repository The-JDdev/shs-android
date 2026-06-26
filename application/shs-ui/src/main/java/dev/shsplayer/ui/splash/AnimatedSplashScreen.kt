package dev.shsplayer.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animated splash screen for SHS Player.
 *
 * Original SHS Player code (clean-room reimplementation). Shows the SHS logo
 * with a scale + pulse animation over a brand-orange-to-purple gradient.
 * Calls [onAnimationComplete] after ~1.8 seconds.
 *
 * Inspired by MX Player's clean splash but uses SHS Player's orange brand
 * color instead of MX Player's blue.
 */
@Composable
fun AnimatedSplashScreen(
    onAnimationComplete: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Logo pulse: scale 0.9 → 1.05 → 0.95 → 1.0 (infinite)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoScale",
    )

    // Auto-advance after 1.8s
    LaunchedEffect(Unit) {
        delay(1800)
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF6D00),  // SHS orange
                        Color(0xFFFFAB40),  // SHS orange light
                        Color(0xFF7C4DFF),  // SHS purple
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "SHS",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6D00),
            )
        }
    }
}
