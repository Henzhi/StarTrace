package com.startrace.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DeepSpaceColorScheme = darkColorScheme(
    primary = StarColors.Primary,
    onPrimary = StarColors.OnPrimary,
    secondary = StarColors.Secondary,
    onSecondary = StarColors.OnSecondary,
    background = StarColors.Background,
    onBackground = StarColors.OnBackground,
    surface = StarColors.Surface,
    onSurface = StarColors.OnSurface,
    error = StarColors.Error,
    onError = StarColors.OnError
)

/**
 * 星迹深空主题 — v1 仅 Dark Theme
 */
@Composable
fun StarTraceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DeepSpaceColorScheme,
        typography = StarTypography,
        content = content
    )
}
