package com.nathan.applock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SystemBlueDark,
    onPrimary = LabelDark,
    secondary = SystemGreen,
    error = SystemRed,
    background = SystemBackgroundDark,
    onBackground = LabelDark,
    surface = SystemBackgroundDark,
    onSurface = LabelDark,
    surfaceVariant = SecondarySystemBackgroundDark,
    onSurfaceVariant = SecondaryLabelDark,
    surfaceContainer = SecondarySystemBackgroundDark,
    surfaceContainerHigh = SecondarySystemBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = SystemBlue,
    onPrimary = LabelLight,
    secondary = SystemGreen,
    error = SystemRed,
    background = SystemBackgroundLight,
    onBackground = LabelLight,
    surface = SystemBackgroundLight,
    onSurface = LabelLight,
    surfaceVariant = SecondarySystemBackgroundLight,
    onSurfaceVariant = SecondaryLabelLight,
    surfaceContainer = SecondarySystemBackgroundLight,
    surfaceContainerHigh = SecondarySystemBackgroundLight
)

@Composable
fun AppLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // iOS theme shouldn't use Android dynamic colors to maintain the Apple aesthetic
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
        content = content
    )
}
