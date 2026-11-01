package com.odocare.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Palet Midnight + Electric Blue (gelap) dan Cloud + Electric Blue (terang).
 * Nama variabel lama (TextWhite, TextGray, SurfaceVariantDark, dst) DIPERTAHANKAN supaya semua layar tetap jalan;
 * nilainya mengikuti mode aktif. LavenderPrimary = warna aksen utama (sekarang biru listrik).
 */
class AppPalette(
    val isDark: Boolean,
    val accent: Color, val onAccent: Color, val secondary: Color,
    val bg: Color, val surface: Color, val surfaceVariant: Color, val menuBg: Color,
    val textPrimary: Color, val textSecondary: Color,
    val buttonSlate: Color, val pillSelected: Color, val fieldBorder: Color, val divider: Color, val track: Color,
    val danger: Color, val error: Color,
    val redBg: Color, val redText: Color, val grayBg: Color, val grayText: Color,
    val blueBg: Color, val blueText: Color, val greenBg: Color, val greenText: Color,
    val yellowBg: Color, val yellowText: Color
)

val MidnightPalette = AppPalette(
    isDark = true,
    accent = Color(0xFF5E8BFF), onAccent = Color(0xFF06122E), secondary = Color(0xFF38BDF8),
    bg = Color(0xFF0B0F19), surface = Color(0xFF131826), surfaceVariant = Color(0xFF1B2236), menuBg = Color(0xFF171D2E),
    textPrimary = Color(0xFFE8ECF5), textSecondary = Color(0xFF8E9AB5),
    buttonSlate = Color(0xFF2A344D), pillSelected = Color(0xFF22305A), fieldBorder = Color(0xFF2B3550),
    divider = Color(0xFF232B42), track = Color(0xFF2A3350),
    danger = Color(0xFFFF8F8F), error = Color(0xFFEF4444),
    redBg = Color(0xFF3B1A22), redText = Color(0xFFFF9A9A), grayBg = Color(0xFF222A3D), grayText = Color(0xFFC4CCDD),
    blueBg = Color(0xFF12343F), blueText = Color(0xFF7FD8F0), greenBg = Color(0xFF143A2C), greenText = Color(0xFF6EDDA5),
    yellowBg = Color(0xFF3E3412), yellowText = Color(0xFFFFD56B)
)

val CloudPalette = AppPalette(
    isDark = false,
    accent = Color(0xFF2563EB), onAccent = Color(0xFFFFFFFF), secondary = Color(0xFF0284C7),
    bg = Color(0xFFEEF2F9), surface = Color(0xFFF8FAFE), surfaceVariant = Color(0xFFFFFFFF), menuBg = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A), textSecondary = Color(0xFF5B6785),
    buttonSlate = Color(0xFFDCE4F5), pillSelected = Color(0xFFDCE7FF), fieldBorder = Color(0xFFC9D3E8),
    divider = Color(0xFFE1E7F3), track = Color(0xFFDDE4F2),
    danger = Color(0xFFC62828), error = Color(0xFFDC2626),
    redBg = Color(0xFFFDE3E3), redText = Color(0xFFB42318), grayBg = Color(0xFFE6EAF2), grayText = Color(0xFF475069),
    blueBg = Color(0xFFD7F1F8), blueText = Color(0xFF0B6B85), greenBg = Color(0xFFDAF5E6), greenText = Color(0xFF117A47),
    yellowBg = Color(0xFFFFF1CC), yellowText = Color(0xFF8A5A00)
)

/**
 * Palet aktif. Diset oleh OdoCareTheme. Berupa state Compose, jadi semua layar yang membaca warna otomatis
 * digambar ulang saat palet berubah (tanpa membuat ulang layar, posisi navigasi tetap).
 */
private var ActivePalette: AppPalette by mutableStateOf(MidnightPalette)

val LavenderPrimary: Color get() = ActivePalette.accent
val OnAccent: Color get() = ActivePalette.onAccent
val BgDark: Color get() = ActivePalette.bg
val SurfaceDark: Color get() = ActivePalette.surface
val SurfaceVariantDark: Color get() = ActivePalette.surfaceVariant
val TextWhite: Color get() = ActivePalette.textPrimary
val TextGray: Color get() = ActivePalette.textSecondary
val ButtonSlate: Color get() = ActivePalette.buttonSlate
val PillSelected: Color get() = ActivePalette.pillSelected
val MenuBg: Color get() = ActivePalette.menuBg
val DangerText: Color get() = ActivePalette.danger
val FieldBorder: Color get() = ActivePalette.fieldBorder
val DividerColor: Color get() = ActivePalette.divider
val TrackColor: Color get() = ActivePalette.track
/** Garis tipis di tepi kartu, hanya di mode terang (kartu putih di atas latar abu-biru). Mode gelap: tanpa garis. */
val CardBorder: BorderStroke? get() = if (ActivePalette.isDark) null else BorderStroke(1.dp, ActivePalette.fieldBorder.copy(alpha = 0.6f))

val StatusRedBg: Color get() = ActivePalette.redBg
val StatusRedText: Color get() = ActivePalette.redText
val StatusGrayBg: Color get() = ActivePalette.grayBg
val StatusGrayText: Color get() = ActivePalette.grayText
val StatusBlueBg: Color get() = ActivePalette.blueBg
val StatusBlueText: Color get() = ActivePalette.blueText
val StatusGreenBg: Color get() = ActivePalette.greenBg
val StatusGreenText: Color get() = ActivePalette.greenText
val StatusYellowBg: Color get() = ActivePalette.yellowBg
val StatusYellowText: Color get() = ActivePalette.yellowText


private fun schemeFor(p: AppPalette): ColorScheme =
    if (p.isDark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, secondary = p.secondary,
        background = p.bg, onBackground = p.textPrimary, surface = p.surface, onSurface = p.textPrimary,
        surfaceVariant = p.surfaceVariant, onSurfaceVariant = p.textSecondary, error = p.error, outline = p.fieldBorder
    ) else lightColorScheme(
        primary = p.accent, onPrimary = p.onAccent, secondary = p.secondary,
        background = p.bg, onBackground = p.textPrimary, surface = p.surface, onSurface = p.textPrimary,
        surfaceVariant = p.surfaceVariant, onSurfaceVariant = p.textSecondary, error = p.error, outline = p.fieldBorder
    )

/** mode: "system" (ikut perangkat), "light", atau "dark". */
@Composable
fun isDarkFor(mode: String): Boolean = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }

@Composable
fun OdoCareTheme(mode: String, content: @Composable () -> Unit) {
    val dark = isDarkFor(mode)
    // Pergantian tema: konten memudar keluar, palet diganti saat transparan, lalu memudar masuk. Saat pertama dibuka tidak ada animasi.
    var shownDark by remember { mutableStateOf(dark) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(dark) {
        if (dark != shownDark) {
            fade.animateTo(0f, tween(150))
            shownDark = dark
        }
        fade.animateTo(1f, tween(150))
    }
    val palette = if (shownDark) MidnightPalette else CloudPalette
    ActivePalette = palette
    val backdrop by animateColorAsState(palette.bg, tween(300), label = "themeBackdrop")
    MaterialTheme(colorScheme = schemeFor(palette), typography = CompactTypography) {
        Box(Modifier.fillMaxSize().background(backdrop)) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }) { content() }
        }
    }
}

/** Angka dengan lebar sama (tabular) agar digit KM dan Rupiah rata kolom. Dipasang di gaya dasar sehingga berlaku untuk semua Text. */
private const val TNUM = "tnum"

/**
 * Gaya teks baku. Aturan: 12sp adalah batas bawah (tidak ada teks di bawah 12sp), teks isi 14sp, judul SemiBold, angka kunci Bold/SemiBold, sisanya Normal.
 * Pakai gaya ini (style = AppType.X) alih-alih menulis fontSize manual agar semua layar konsisten.
 */
object AppType {
    val ScreenTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    val Section = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    val CardTitle = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    /** Angka kunci di kartu: biaya, liter, KM. */
    val Value = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    val Body = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontFeatureSettings = TNUM)
    /** Label dan keterangan abu-abu. */
    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TNUM)
}

/** Skala font ringkas: lebih kecil dari default Material agar tampilan minimalis. */
val CompactTypography = Typography(
    headlineSmall = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TNUM),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontFeatureSettings = TNUM),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TNUM),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM),
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
