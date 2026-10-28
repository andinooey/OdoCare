package com.odocare.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
class OdoColors(
    val bg: Color, val surface: Color, val surface2: Color, val elevated: Color, val border: Color,
    val textPrimary: Color, val textSecondary: Color, val textTertiary: Color,
    val accent: Color, val onAccent: Color, val blue: Color, val warning: Color, val error: Color, val success: Color
)

val DarkOdo = OdoColors(
    bg = Color(0xFF0B0F14), surface = Color(0xFF131922), surface2 = Color(0xFF1A2330), elevated = Color(0xFF202B39),
    border = Color(0xFF273242), textPrimary = Color(0xFFF4F7FA), textSecondary = Color(0xFF9AA7B5), textTertiary = Color(0xFF788696),
    accent = Color(0xFFC5F467), onAccent = Color(0xFF0B0F14), blue = Color(0xFF73B7FF),
    warning = Color(0xFFFFC66D), error = Color(0xFFFF7474), success = Color(0xFF69D6A0)
)

val LightOdo = OdoColors(
    bg = Color(0xFFF5F7FA), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEDF1F5), elevated = Color(0xFFFFFFFF),
    border = Color(0xFFE0E6ED), textPrimary = Color(0xFF17202B), textSecondary = Color(0xFF687586), textTertiary = Color(0xFF5E6B7A),
    accent = Color(0xFF547D18), onAccent = Color(0xFFFFFFFF), blue = Color(0xFF1F5FA8),
    warning = Color(0xFF8A5900), error = Color(0xFFB42332), success = Color(0xFF16794A)
)

val LocalOdoColors = staticCompositionLocalOf { DarkOdo }
object Odo { val colors: OdoColors @Composable get() = LocalOdoColors.current }

object OdoSpacing { val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 20.dp; val xxl = 24.dp; val xxxl = 32.dp }

private val Sans = FontFamily.SansSerif
val OdoTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

val OdoShapes = Shapes(
    small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp)
)

/** mode: "Ikuti sistem", "Gelap", atau "Terang". Bawaan: Gelap. */
@Composable
fun OdoCareTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) { "Terang" -> false; "Ikuti sistem" -> isSystemInDarkTheme(); else -> true }
    val c = if (dark) DarkOdo else LightOdo
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, secondary = c.blue, background = c.bg, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary, surfaceVariant = c.surface2, onSurfaceVariant = c.textSecondary,
        outline = c.border, outlineVariant = c.border, error = c.error, surfaceContainer = c.surface,
        surfaceContainerHigh = c.elevated, surfaceContainerHighest = c.elevated, primaryContainer = c.surface2, onPrimaryContainer = c.accent,
        secondaryContainer = c.surface2, onSecondaryContainer = c.accent)
    else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, secondary = c.blue, background = c.bg, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary, surfaceVariant = c.surface2, onSurfaceVariant = c.textSecondary,
        outline = c.border, outlineVariant = c.border, error = c.error, surfaceContainer = c.surface,
        surfaceContainerHigh = c.elevated, surfaceContainerHighest = c.surface2, primaryContainer = c.surface2, onPrimaryContainer = c.accent,
        secondaryContainer = c.surface2, onSecondaryContainer = c.accent)
    CompositionLocalProvider(LocalOdoColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = OdoTypography, shapes = OdoShapes) {
            Surface(color = c.bg, content = content)
        }
    }
}
