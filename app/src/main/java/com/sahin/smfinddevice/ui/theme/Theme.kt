package com.sahin.smfinddevice.ui.theme

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

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFD9E7FF),
    onPrimaryContainer = NavyDeep,
    secondary = LightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFF6EF),
    onSecondaryContainer = Color(0xFF00352E),
    tertiary = AlertAmber,
    tertiaryContainer = Color(0xFFFFE7B8),
    onTertiaryContainer = Color(0xFF4A3300),
    error = DangerRed,
    errorContainer = Color(0xFFFBDADA),
    onErrorContainer = Color(0xFF5C1210),
    background = LightBackground,
    onBackground = NavyDeep,
    surface = LightSurface,
    onSurface = NavyDeep,
    surfaceVariant = Color(0xFFE7ECF5),
    onSurfaceVariant = Color(0xFF44526B)
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF17417A),
    onPrimaryContainer = Color(0xFFD9E7FF),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF00352E),
    secondaryContainer = Color(0xFF16473F),
    onSecondaryContainer = Color(0xFFCFF6EF),
    tertiary = AlertAmber,
    tertiaryContainer = Color(0xFF5C4200),
    onTertiaryContainer = Color(0xFFFFE7B8),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1210),
    onErrorContainer = Color(0xFFFBDADA),
    background = DarkBackground,
    onBackground = Color(0xFFE6ECFA),
    surface = DarkSurface,
    onSurface = Color(0xFFE6ECFA),
    surfaceVariant = Color(0xFF243254),
    onSurfaceVariant = Color(0xFFB6C2DE)
)

@Composable
fun SMFindDeviceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmTypography,
        shapes = SmShapes,
        content = content
    )
}
