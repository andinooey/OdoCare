package com.odocare.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    MaterialTheme(colorScheme = DarkScheme, typography = CompactTypography, content = content)
}

val StatusYellowBg = Color(0xFF4A3F12)
val StatusYellowText = Color(0xFFFFE082)

/** Angka dengan lebar sama (tabular) agar digit KM dan Rupiah rata kolom. Dipasang di gaya dasar sehingga berlaku untuk semua Text. */
private const val TNUM = "tnum"

/**
 * Gaya teks baku. Aturan: 12sp adalah batas bawah (tidak ada teks 11sp), judul SemiBold, angka kunci Bold/SemiBold, sisanya Normal.
 * Pakai gaya ini (style = AppType.X) alih-alih menulis fontSize manual agar semua layar konsisten.
 */
object AppType {
    val ScreenTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    val Section = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    val CardTitle = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    /** Angka kunci di kartu: biaya, liter, KM. */
    val Value = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    val Body = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM)
    /** Label dan keterangan abu-abu. */
    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TNUM)
}

/** Skala font ringkas: lebih kecil dari default Material agar tampilan minimalis. */
val CompactTypography = Typography(
    headlineSmall = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TNUM),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TNUM),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM)
)

/**
 * Skala spacing baku (kelipatan 4): XS 4 · S 8 · M 12 · L 16 · XL 24.
 * Dipakai untuk padding, jarak antar elemen (Spacer / spacedBy), dan contentPadding. Ukuran komponen (ikon, tinggi tombol) tidak termasuk.
 * FabClearance = ruang kosong di bawah daftar agar item terakhir tidak tertutup tombol tambah (+).
 */
object Spacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 24.dp
    val FabClearance = 88.dp
}
