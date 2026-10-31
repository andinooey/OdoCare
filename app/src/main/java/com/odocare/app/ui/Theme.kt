package com.odocare.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LavenderPrimary = Color(0xFFB8C5FF)
val BgDark = Color(0xFF12131A)
val SurfaceDark = Color(0xFF1A1B23)
val SurfaceVariantDark = Color(0xFF2A2B35)
val TextWhite = Color(0xFFE6E4EC)
val TextGray = Color(0xFFC8C6D2)
val ButtonSlate = Color(0xFF414559)
val PillSelected = Color(0xFF3A3F52)
val MenuBg = Color(0xFF1F2026)
val DangerText = Color(0xFFF2B8B5)

val StatusRedBg = Color(0xFF4A1C1C)
val StatusRedText = Color(0xFFFFB4AB)
val StatusGrayBg = Color(0xFF2C2C2C)
val StatusGrayText = Color(0xFFE0E0E0)
val StatusBlueBg = Color(0xFF1A2A4F)
val StatusBlueText = Color(0xFFD0D9FF)
val StatusGreenBg = Color(0xFF1F4A22)
val StatusGreenText = Color(0xFF8BD18F)

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
