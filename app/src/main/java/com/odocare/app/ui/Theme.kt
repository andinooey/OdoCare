package com.odocare.app.ui

import android.app.Activity
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Palet warna aplikasi. Dua palet (gelap / terang) dengan peran yang sama, sehingga seluruh layar cukup memakai token
 * (Accent, CardBg, TextMuted, dst) dan otomatis ikut berganti tema.
 *
 * Prinsip: tiga tingkat permukaan yang jelas (AppBg < SurfaceBg < CardBg + garis tepi), satu aksen hangat (amber, seperti lampu indikator
 * dashboard) untuk aksi utama, dan warna kategori (servis, BBM, dokumen, odometer) supaya daftar campuran mudah dibedakan.
 * Warna status memakai latar transparan sehingga menyatu dengan permukaan di bawahnya.
 */
@Immutable
class AppPalette(
    val dark: Boolean,
    val bg: Color, val surface: Color, val card: Color, val border: Color, val divider: Color,
    val text: Color, val muted: Color, val accent: Color, val onAccent: Color, val accentSoft: Color,
    val menu: Color, val danger: Color, val secondary: Color, val slate: Color,
    val redBg: Color, val redText: Color, val grayBg: Color, val grayText: Color, val blueBg: Color, val blueText: Color,
    val greenBg: Color, val greenText: Color, val yellowBg: Color, val yellowText: Color,
    val catService: Color, val catFuel: Color, val catDoc: Color, val catOdo: Color
)

private val DarkPalette = AppPalette(
    dark = true,
    bg = Color(0xFF0E1117), surface = Color(0xFF151A23), card = Color(0xFF1E2531), border = Color(0xFF2C3646), divider = Color(0xFF252D3B),
    text = Color(0xFFF1F4F9), muted = Color(0xFFA3AFC0), accent = Color(0xFFF5A524), onAccent = Color(0xFF1F1300), accentSoft = Color(0xFF2B3648),
    menu = Color(0xFF1B2230), danger = Color(0xFFFCA5A5), secondary = Color(0xFF4F8CFF), slate = Color(0xFF334155),
    redBg = Color(0x29F87171), redText = Color(0xFFF87171), grayBg = Color(0x3394A3B8), grayText = Color(0xFFCBD5E1),
    blueBg = Color(0x294F8CFF), blueText = Color(0xFF93B8FF), greenBg = Color(0x2934D399), greenText = Color(0xFF34D399),
    yellowBg = Color(0x29FBBF24), yellowText = Color(0xFFFBBF24),
    catService = Color(0xFF4F8CFF), catFuel = Color(0xFFF5A524), catDoc = Color(0xFF2DD4BF), catOdo = Color(0xFFA78BFA)
)

private val LightPalette = AppPalette(
    dark = false,
    bg = Color(0xFFF4F6FA), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF), border = Color(0xFFD5DCE6), divider = Color(0xFFE3E8F0),
    text = Color(0xFF0F172A), muted = Color(0xFF5B6779), accent = Color(0xFFB45309), onAccent = Color(0xFFFFFFFF), accentSoft = Color(0xFFFFF1DC),
    menu = Color(0xFFFFFFFF), danger = Color(0xFFB42318), secondary = Color(0xFF2563EB), slate = Color(0xFFE2E8F0),
    redBg = Color(0x1FDC2626), redText = Color(0xFFB91C1C), grayBg = Color(0x24647488), grayText = Color(0xFF475569),
    blueBg = Color(0x1F2563EB), blueText = Color(0xFF1D4ED8), greenBg = Color(0x2416A34A), greenText = Color(0xFF166534),
    yellowBg = Color(0x29D97706), yellowText = Color(0xFF92400E),
    catService = Color(0xFF2563EB), catFuel = Color(0xFFB45309), catDoc = Color(0xFF0F766E), catOdo = Color(0xFF6D28D9)
)

/** Palet yang sedang aktif. Berupa state Compose, jadi setiap layar yang membaca token warna otomatis tergambar ulang saat tema berganti. */
object AppTheme {
    var palette by mutableStateOf(DarkPalette)
}

val IsDarkTheme: Boolean get() = AppTheme.palette.dark

// ---- Token warna (dipakai di seluruh layar) ----
val AppBg: Color get() = AppTheme.palette.bg
val SurfaceBg: Color get() = AppTheme.palette.surface
val CardBg: Color get() = AppTheme.palette.card
val FieldBorder: Color get() = AppTheme.palette.border
val DividerColor: Color get() = AppTheme.palette.divider
val TextPrimary: Color get() = AppTheme.palette.text
val TextMuted: Color get() = AppTheme.palette.muted
val Accent: Color get() = AppTheme.palette.accent
val OnAccent: Color get() = AppTheme.palette.onAccent
val PillSelected: Color get() = AppTheme.palette.accentSoft
val ButtonSlate: Color get() = AppTheme.palette.slate
val MenuBg: Color get() = AppTheme.palette.menu
val DangerText: Color get() = AppTheme.palette.danger

val StatusRedBg: Color get() = AppTheme.palette.redBg
val StatusRedText: Color get() = AppTheme.palette.redText
val StatusGrayBg: Color get() = AppTheme.palette.grayBg
val StatusGrayText: Color get() = AppTheme.palette.grayText
val StatusBlueBg: Color get() = AppTheme.palette.blueBg
val StatusBlueText: Color get() = AppTheme.palette.blueText
val StatusGreenBg: Color get() = AppTheme.palette.greenBg
val StatusGreenText: Color get() = AppTheme.palette.greenText
val StatusYellowBg: Color get() = AppTheme.palette.yellowBg
val StatusYellowText: Color get() = AppTheme.palette.yellowText

/** Warna kategori: identitas tiap fitur di ikon dan kartu. */
val CatService: Color get() = AppTheme.palette.catService
val CatFuel: Color get() = AppTheme.palette.catFuel
val CatDoc: Color get() = AppTheme.palette.catDoc
val CatOdo: Color get() = AppTheme.palette.catOdo

/** Pilihan tema di Pengaturan: AUTO mengikuti sistem. */
object ThemeMode {
    const val AUTO = "AUTO"
    const val DARK = "DARK"
    const val LIGHT = "LIGHT"
    fun label(m: String) = when (m) { DARK -> "Gelap"; LIGHT -> "Terang"; else -> "Otomatis" }
}

@Composable
fun OdoCareTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; else -> isSystemInDarkTheme() }
    val palette = if (dark) DarkPalette else LightPalette
    // Dipasang sebelum isi digambar, hanya saat berubah
    if (AppTheme.palette !== palette) AppTheme.palette = palette
    val scheme = remember(dark) {
        val p = palette
        val base = if (dark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = p.accent, onPrimary = p.onAccent, secondary = p.secondary,
            background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text,
            surfaceVariant = p.card, onSurfaceVariant = p.muted, outline = p.border, outlineVariant = p.divider,
            error = if (dark) Color(0xFFF87171) else Color(0xFFDC2626)
        )
    }
    SystemBarsStyle(rememberActivityWindow(), dark)
    MaterialTheme(colorScheme = scheme, typography = CompactTypography, content = content)
}

/** Ikon status bar dan navigation bar mengikuti tema aplikasi (bukan tema sistem). */
@Composable
fun SystemBarsStyle(window: Window?, dark: Boolean) {
    SideEffect {
        if (window != null) {
            val c = WindowCompat.getInsetsController(window, window.decorView)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }
}

@Composable
fun rememberActivityWindow(): Window? {
    var ctx = LocalContext.current
    while (ctx is ContextWrapper) { if (ctx is Activity) return ctx.window; ctx = ctx.baseContext }
    return null
}

/** Jendela dialog saat dipanggil di dalam isi Dialog (dipakai form dan detail layar penuh). */
@Composable
fun rememberDialogWindow(): Window? = (LocalView.current.parent as? DialogWindowProvider)?.window

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
