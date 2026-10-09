package com.payledger.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = GreenSoft,
    onPrimaryContainer = GreenDark,
    secondary = GreenSuccess,
    onSecondary = SurfaceWhite,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    outline = TextMuted.copy(alpha = 0.35f),
)

@Composable
fun PayLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = PayLedgerTypography,
        content = content,
    )
}
