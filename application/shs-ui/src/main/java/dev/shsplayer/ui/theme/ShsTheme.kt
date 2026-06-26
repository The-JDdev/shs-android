package dev.shsplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

// ════════════════════════════════════════════════════════════════════════════
// SHS Player Material 3 Color Scheme
//
// Inspired by MX Player's warm orange accent + clean Material 3 surfaces.
// Orange is the SHS Player brand color (matches the launcher icon background).
// ════════════════════════════════════════════════════════════════════════════

// Brand orange (SHS Player primary)
val ShsOrange = Color(0xFFFF6D00)
val ShsOrangeLight = Color(0xFFFFAB40)
val ShsOrangeDark = Color(0xFFC43E00)
val ShsOrangeContainer = Color(0xFFFFDCC8)
val ShsOnOrangeContainer = Color(0xFF3B0900)

// Accent purple (secondary)
val ShsPurple = Color(0xFF7C4DFF)
val ShsPurpleContainer = Color(0xFFE1D8FF)
val ShsOnPurpleContainer = Color(0xFF21005D)

// Accent pink (tertiary)
val ShsPink = Color(0xFFFF4081)
val ShsPinkContainer = Color(0xFFFFD9E2)
val ShsOnPinkContainer = Color(0xFF3B0014)

// Neutral surfaces (light theme)
val ShsSurfaceLight = Color(0xFFFCFBF8)
val ShsSurfaceVariantLight = Color(0xFFE7E0D6)
val ShsOnSurfaceLight = Color(0xFF1E1B16)
val ShsOnSurfaceVariantLight = Color(0xFF4D4639)

// Neutral surfaces (dark theme)
val ShsSurfaceDark = Color(0xFF1A1814)
val ShsSurfaceVariantDark = Color(0xFF4D4639)
val ShsOnSurfaceDark = Color(0xFFE6E1D8)
val ShsOnSurfaceVariantDark = Color(0xFFCFC6B8)

// Error
val ShsError = Color(0xFFBA1A1A)
val ShsErrorContainer = Color(0xFFFFDAD6)

// Glassmorphism accent colors (for custom overlays)
val GlassBlue = Color(0xFF4285F4)
val GlassRed = Color(0xFFEA4335)
val GlassYellow = Color(0xFFFBBC05)
val GlassGreen = Color(0xFF34A853)
val GlassPurple = Color(0xFF9C27B0)
val GlassCyan = Color(0xFF00BCD4)

private val ShsLightColorScheme = lightColorScheme(
    primary = ShsOrange,
    onPrimary = Color.White,
    primaryContainer = ShsOrangeContainer,
    onPrimaryContainer = ShsOnOrangeContainer,
    secondary = ShsPurple,
    onSecondary = Color.White,
    secondaryContainer = ShsPurpleContainer,
    onSecondaryContainer = ShsOnPurpleContainer,
    tertiary = ShsPink,
    onTertiary = Color.White,
    tertiaryContainer = ShsPinkContainer,
    onTertiaryContainer = ShsOnPinkContainer,
    error = ShsError,
    onError = Color.White,
    errorContainer = ShsErrorContainer,
    onErrorContainer = Color(0xFF93000A),
    background = ShsSurfaceLight,
    onBackground = ShsOnSurfaceLight,
    surface = ShsSurfaceLight,
    onSurface = ShsOnSurfaceLight,
    surfaceVariant = ShsSurfaceVariantLight,
    onSurfaceVariant = ShsOnSurfaceVariantLight,
    outline = Color(0xFF80756B),
    outlineVariant = Color(0xFFD0C8BC),
    scrim = Color.Black,
    inverseSurface = Color(0xFF33302A),
    inverseOnSurface = Color(0xFFF6F0E7),
    inversePrimary = ShsOrangeLight,
)

private val ShsDarkColorScheme = darkColorScheme(
    primary = ShsOrangeLight,
    onPrimary = Color(0xFF542100),
    primaryContainer = ShsOrangeDark,
    onPrimaryContainer = ShsOrangeContainer,
    secondary = Color(0xFFCFBCFF),
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4F378A),
    onSecondaryContainer = ShsPurpleContainer,
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF5E112C),
    tertiaryContainer = Color(0xFF7B2944),
    onTertiaryContainer = ShsPinkContainer,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = ShsErrorContainer,
    background = ShsSurfaceDark,
    onBackground = ShsOnSurfaceDark,
    surface = ShsSurfaceDark,
    onSurface = ShsOnSurfaceDark,
    surfaceVariant = ShsSurfaceVariantDark,
    onSurfaceVariant = ShsOnSurfaceVariantDark,
    outline = Color(0xFF999081),
    outlineVariant = Color(0xFF4D4639),
    scrim = Color.Black,
    inverseSurface = Color(0xFFE6E1D8),
    inverseOnSurface = Color(0xFF33302A),
    inversePrimary = ShsOrange,
)

/**
 * SHS Player Material 3 theme.
 *
 * - On Android 12+ (S), supports Material You dynamic color (off by default —
 *   SHS Player uses its own brand orange for consistency).
 * - On older Android, falls back to the static SHS orange scheme.
 * - Pass `dynamicColor = true` to enable Material You on S+.
 */
@Composable
fun ShsPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ShsDarkColorScheme
        else -> ShsLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShsTypography,
        content = content,
    )
}
