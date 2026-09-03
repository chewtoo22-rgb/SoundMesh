package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SonicCyan,
    onPrimary = Color(0xFF041B26),
    primaryContainer = Color(0xFF09374C),
    onPrimaryContainer = Color(0xFFB8F3FF),
    secondary = SecondaryBlue,
    onSecondary = Color(0xFF021E2F),
    secondaryContainer = Color(0xFF13364D),
    onSecondaryContainer = Color(0xFFCCE8FE),
    tertiary = SonicAmber,
    onTertiary = Color(0xFF2E1500),
    tertiaryContainer = Color(0xFF4C2700),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    error = SonicRose,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme( // Keep audiophile dark palette preferred
    primary = SonicCyanDark,
    onPrimary = Color.White,
    secondary = SecondaryBlue,
    tertiary = SonicAmber,
    background = ObsidianBg,
    surface = ObsidianSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Audiophile studio theme defaults to true for deep stage contrast
    dynamicColor: Boolean = false, // Keep custom acoustic neon palette for distinctive branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
