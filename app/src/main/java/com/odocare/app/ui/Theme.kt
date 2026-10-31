package com.odocare.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LavenderPrimary = Color(0xFFD0D9FF)
val BgDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val SurfaceVariantDark = Color(0xFF2A2A2A)
val TextWhite = Color(0xFFF3F4F6)
val TextGray = Color(0xFFA0A0A0)

val StatusRedBg = Color(0xFF4A1C1C)
val StatusRedText = Color(0xFFFFB4AB)
val StatusGrayBg = Color(0xFF2C2C2C)
val StatusGrayText = Color(0xFFE0E0E0)
val StatusBlueBg = Color(0xFF1A2A4F)
val StatusBlueText = Color(0xFFD0D9FF)
val StatusGreenBg = Color(0xFF2E4A2E)
val StatusGreenText = Color(0xFFA5D6A7)

private val DarkScheme = darkColorScheme(
    primary = LavenderPrimary, onPrimary = Color(0xFF0D1B3E), secondary = Color(0xFF22D3EE),
    background = BgDark, onBackground = TextWhite, surface = SurfaceDark, onSurface = TextWhite,
    surfaceVariant = SurfaceVariantDark, onSurfaceVariant = TextGray, error = Color(0xFFEF4444)
)

@Composable
fun OdoCareTheme(mode: String, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = Typography(), content = content)
}

val StatusYellowBg = Color(0xFF4A3F12)
val StatusYellowText = Color(0xFFFFE082)
