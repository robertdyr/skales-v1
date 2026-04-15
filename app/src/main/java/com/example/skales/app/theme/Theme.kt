package com.example.skales.app.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = InkBlack,
    primaryContainer = GoldMuted,
    onPrimaryContainer = TextPrimary,
    secondary = AccentIndigo,
    onSecondary = TextPrimary,
    secondaryContainer = InkPanelSoft,
    onSecondaryContainer = TextPrimary,
    tertiary = GoldMuted,
    background = InkBlack,
    onBackground = TextPrimary,
    surface = InkPanel,
    onSurface = TextPrimary,
    surfaceVariant = InkPanelRaised,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft,
    outlineVariant = OutlineSoft.copy(alpha = 0.5f),
    error = Color(0xFFE66B6B),
)

private val LightColorScheme = lightColorScheme(
    primary = GoldPrimary,
    onPrimary = InkBlack,
    primaryContainer = Color(0xFFF1D995),
    onPrimaryContainer = InkBlack,
    secondary = AccentIndigo,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFFDBD9FF),
    onSecondaryContainer = InkBlack,
    tertiary = GoldMuted,
    background = Color(0xFFF3EFE8),
    onBackground = InkBlack,
    surface = Color(0xFFFFFBF5),
    onSurface = InkBlack,
    surfaceVariant = Color(0xFFE6DED2),
    onSurfaceVariant = Color(0xFF5D564C),
    outline = Color(0xFFC0B6A5),
    outlineVariant = Color(0xFFD7CCBB),
)

private val SkalesShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun SkalesTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
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
        shapes = SkalesShapes,
        content = content
    )
}
