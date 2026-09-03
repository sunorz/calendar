package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    error = DarkError,
    onError = DarkOnError,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    error = LightError,
    onError = LightOnError,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

private val ElderlyLightColorScheme = lightColorScheme(
    primary = ElderlyLightPrimary,
    onPrimary = ElderlyLightOnPrimary,
    primaryContainer = ElderlyLightPrimaryContainer,
    onPrimaryContainer = ElderlyLightOnPrimaryContainer,
    error = ElderlyLightError,
    onError = ElderlyLightOnError,
    background = ElderlyLightBackground,
    surface = ElderlyLightSurface,
    surfaceVariant = ElderlyLightSurfaceVariant,
    onBackground = ElderlyLightOnBackground,
    onSurface = ElderlyLightOnSurface,
    onSurfaceVariant = ElderlyLightOnSurfaceVariant,
    outline = ElderlyLightOutline,
    outlineVariant = ElderlyLightOutlineVariant
)

private val ElderlyDarkColorScheme = darkColorScheme(
    primary = ElderlyDarkPrimary,
    onPrimary = ElderlyDarkOnPrimary,
    primaryContainer = ElderlyDarkPrimaryContainer,
    onPrimaryContainer = ElderlyDarkOnPrimaryContainer,
    error = ElderlyDarkError,
    onError = ElderlyDarkOnError,
    background = ElderlyDarkBackground,
    surface = ElderlyDarkSurface,
    surfaceVariant = ElderlyDarkSurfaceVariant,
    onBackground = ElderlyDarkOnBackground,
    onSurface = ElderlyDarkOnSurface,
    onSurfaceVariant = ElderlyDarkOnSurfaceVariant,
    outline = ElderlyDarkOutline,
    outlineVariant = ElderlyDarkOutlineVariant
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isElderlyMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    // In elderly mode, strictly force light mode and prohibit dark mode
    val darkTheme = if (isElderlyMode) {
        false
    } else {
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    val colorScheme = when {
        isElderlyMode -> ElderlyLightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
