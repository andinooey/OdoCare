@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.odocare.app.data.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odocare.app.data.ServiceRecord
import com.odocare.app.domain.IntervalUnit
import com.odocare.app.domain.ScheduleView
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val NoInsets = WindowInsets(0, 0, 0, 0)
private val idLocale = Locale("id", "ID")
private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val dateTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

fun fmtDate(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)
fun fmtDateTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateTimeFmt)
fun msToLocalDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
fun localDateToMillis(d: LocalDate): Long = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun todayMillis(): Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun toPickerMillis(sys: Long): Long = msToLocalDate(sys).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun fromPickerMillis(pic: Long): Long = Instant.ofEpochMilli(pic).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun posInt(s: String): Int? = s.toIntOrNull()?.takeIf { it > 0 }
/** Pemisah ribuan di seluruh aplikasi (input dan tampilan). Ganti ke '.' bila ingin gaya Indonesia: 5.000 */
const val THOUSAND_SEP = ','
fun fmtNum(n: Long): String = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US).apply { groupingSeparator = THOUSAND_SEP }).format(n)
fun fmtKm(n: Int): String = fmtNum(n.toLong())
fun rupiah(n: Long): String = "Rp " + fmtNum(n)
fun fmtTime(hour: Int, minute: Int): String = String.format(Locale.US, "%02d:%02d", hour, minute)
fun intervalLabel(km: Int?, months: Int?): String {
    val parts = mutableListOf<String>()
    km?.takeIf { it > 0 }?.let { parts += "tiap ${fmtKm(it)} km" }
    months?.takeIf { it > 0 }?.let { parts += "tiap $it bulan" }
    return parts.joinToString(" · ")
}

val docTypes: List<Pair<String, String>> = listOf(
    "PAJAK_TAHUNAN" to "Pajak Tahunan", "GANTI_PLAT" to "Ganti Plat 5 Tahunan", "STNK_5_TAHUNAN" to "STNK 5 Tahunan",
    "SIM_A" to "SIM A", "SIM_C" to "SIM C", "ASURANSI" to "Asuransi", "GARANSI" to "Garansi", "LAINNYA" to "Lainnya"
)
fun docTypeLabel(k: String): String = docTypes.firstOrNull { it.first == k }?.second ?: k
fun vehicleTypeLabel(t: String): String = when (t) { "MOBIL" -> "Mobil"; "MOTOR" -> "Motor"; "ALL" -> "Semua"; else -> "Lainnya" }
fun docDaysText(days: Long): String = when { days < 0 -> "Lewat ${-days} hari"; days == 0L -> "Hari ini"; else -> "$days hari lagi" }

fun kmLine(rem: Int, interval: Int?): String = when {
    rem < 0 -> "Lewat ${fmtKm(-rem)} km"
    interval != null && rem < interval * 0.2 -> "Segera, sisa ${fmtKm(rem)} km"
    else -> "Sisa ${fmtKm(rem)} km"
}
fun dayLine(d: Long): String = when { d < 0 -> "Lewat ${-d} hari"; d == 0L -> "Segera, jatuh tempo hari ini"; d < 30 -> "Segera, $d hari lagi"; else -> "Sisa $d hari" }
fun scheduleDetail(v: ScheduleView): String {
    val parts = mutableListOf<String>()
    v.kmRemaining?.let { parts += kmLine(it, v.schedule.intervalKm) }
    v.daysRemaining?.let { parts += dayLine(it) }
    return if (parts.isEmpty()) "Belum ada data" else parts.joinToString(" · ")
}

/** 6 bulan terakhir (termasuk bulan ini): label + total biaya */
fun sixMonthSeries(records: List<ServiceRecord>): Pair<List<String>, List<Long>> {
    val now = YearMonth.now()
    val months = (5 downTo 0).map { now.minusMonths(it.toLong()) }
    val sums = months.map { ym -> records.filter { YearMonth.from(msToLocalDate(it.date)) == ym }.sumOf { it.cost } }
    return months.map { it.month.getDisplayName(TextStyle.SHORT, idLocale) } to sums
}

@Composable
fun appBarColors() = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, color = TextWhite) }, colors = appBarColors(),
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextWhite) } },
        actions = actions
    )
}

/** Menampilkan angka dengan pemisah ribuan (5000 -> 5,000). Nilai yang disimpan di state tetap digit murni. */
object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val n = raw.length
        val sb = StringBuilder()
        val map = IntArray(n + 1)
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) sb.append(THOUSAND_SEP)
            map[i] = sb.length
            sb.append(raw[i])
        }
        map[n] = sb.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = map[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int): Int {
                var res = 0
                for (i in 0..n) if (map[i] <= offset) res = i
                return res
            }
        }
        return TransformedText(AnnotatedString(sb.toString()), mapping)
    }
}

/** Field angka bulat dengan pemisah ribuan. currency = true menambahkan awalan "Rp". */
@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, currency: Boolean = false) {
    val prefixSlot: (@Composable () -> Unit)? = if (currency) { { Text("Rp ") } } else null
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.filter(Char::isDigit).trimStart('0').take(12)) },
        label = { Text(label) }, singleLine = true, prefix = prefixSlot,
        visualTransformation = ThousandsTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/** Tombol jam yang membuka time picker 24 jam. */
@Composable
fun TimeField(label: String, hour: Int, minute: Int, onPicked: (hour: Int, minute: Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) { Text("$label: ${fmtTime(hour, minute)}") }
    if (open) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp, modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))
                    TimePicker(state = state)
                    Row(Modifier.height(40.dp).fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { open = false }) { Text("Batal") }
                        TextButton(onClick = { onPicked(state.hour, state.minute); open = false }) { Text("Pilih") }
                    }
                }
            }
        }
    }
}

/** Switch notifikasi on/off + pilihan jam (jam hanya muncul saat aktif). */
@Composable
fun NotifyOptions(enabled: Boolean, hour: Int, minute: Int, onChange: (enabled: Boolean, hour: Int, minute: Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Notifikasi pengingat", color = TextWhite)
            Text(if (enabled) "Aktif" else "Mati", color = TextGray, fontSize = 12.sp)
        }
        Switch(checked = enabled, onCheckedChange = { onChange(it, hour, minute) })
    }
    if (enabled) TimeField("Jam notifikasi", hour, minute) { h, m -> onChange(true, h, m) }
}

@Composable
fun rememberStoredImage(path: String?, maxSide: Int): ImageBitmap? {
    var bmp by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bmp = if (path == null) null else withContext(Dispatchers.IO) { ImageStore.load(path, maxSide)?.asImageBitmap() }
    }
    return bmp
}

/** Foto servis dari penyimpanan internal. Tidak menampilkan apa pun bila file hilang. */
@Composable
fun StoredImage(path: String?, modifier: Modifier = Modifier, maxSide: Int = 600, contentScale: ContentScale = ContentScale.Crop) {
    val bmp = rememberStoredImage(path, maxSide)
    if (bmp != null) Image(bitmap = bmp, contentDescription = "Foto servis", modifier = modifier, contentScale = contentScale)
}

@Composable
fun DatePickerDialogX(initialMillis: Long?, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis?.let { toPickerMillis(it) })
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = state.selectedDateMillis != null, onClick = { state.selectedDateMillis?.let { onPicked(fromPickerMillis(it)) } }) { Text("Pilih") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    ) { DatePicker(state = state) }
}

/** Tombol tanggal yang membuka date picker. */
@Composable
fun DateField(label: String, millis: Long?, onPicked: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) { Text(if (millis != null) "$label: ${fmtDate(millis)}" else label) }
    if (open) DatePickerDialogX(millis, { open = false }) { onPicked(it); open = false }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = Color.Red) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

@Composable
fun InfoDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, text = { Text(message) }, confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } })
}

/** Update KM; bila lebih kecil dari KM sekarang, minta konfirmasi dulu. */
@Composable
fun AddKmDialog(currentKm: Int?, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var kmText by remember { mutableStateOf("") }
    var pendingLower by remember { mutableStateOf<Int?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Update Odometer") },
        text = {
            Column {
                if (currentKm != null) { Text("KM sekarang: ${fmtKm(currentKm)}", color = TextGray, fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
                NumberField(kmText, { kmText = it }, "Kilometer")
            }
        },
        confirmButton = {
            TextButton(enabled = posInt(kmText) != null, onClick = {
                posInt(kmText)?.let { v -> if (currentKm != null && v < currentKm) pendingLower = v else onSave(v) }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
    pendingLower?.let { v ->
        AlertDialog(onDismissRequest = { pendingLower = null }, title = { Text("KM lebih kecil?") },
            text = { Text("KM ${fmtKm(v)} lebih kecil dari KM sekarang (${fmtKm(currentKm ?: 0)}). Tetap simpan?") },
            confirmButton = { TextButton(onClick = { onSave(v) }) { Text("Tetap Simpan") } },
            dismissButton = { TextButton(onClick = { pendingLower = null }) { Text("Batal") } })
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFF3A3A3A))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

/** Plat nomor ala plat hitam */
@Composable
fun PlateView(plate: String) {
    Surface(color = Color.Black, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Color.White)) {
        Text(plate, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** Odometer: satu digit per kotak (minimal 6 digit) */
@Composable
fun OdometerDigits(km: Int?) {
    val digits = (km ?: 0).coerceAtLeast(0).toString().padStart(6, '0')
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        digits.forEach { c ->
            Box(Modifier.size(26.dp, 36.dp).background(Color(0xFF0B0B0B), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Text(c.toString(), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun compact(v: Long): String = when { v >= 1_000_000 -> String.format(Locale.US, "%.1fjt", v / 1_000_000.0); v >= 1_000 -> "${v / 1_000}rb"; else -> v.toString() }

/** Grafik batang dari div/Box biasa */
@Composable
fun BarChart(labels: List<String>, values: List<Long>, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (v > 0) Text(compact(v), fontSize = 9.sp, color = TextGray, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                val h = if (v > 0) (90f * v.toFloat() / max.toFloat()).coerceAtLeast(3f) else 1f
                Box(Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(4.dp)).background(if (v > 0) LavenderPrimary else Color(0xFF3A3A3A)))
                Spacer(Modifier.height(4.dp))
                Text(labels.getOrElse(i) { "" }, fontSize = 10.sp, color = TextGray, maxLines = 1)
            }
        }
    }
}

@Composable
fun PercentBar(label: String, value: Long, total: Long) {
    val pct = if (total > 0) (value * 100 / total).toInt() else 0
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = TextWhite, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${rupiah(value)} ($pct%)", color = TextGray, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(if (total > 0) value.toFloat() / total.toFloat() else 0f, LavenderPrimary)
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, fontSize = 12.sp, color = TextGray)
        }
    }
}

/** Pilihan satuan Hari / Bulan / Tahun */
@Composable
fun UnitChips(selected: IntervalUnit, onSelect: (IntervalUnit) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IntervalUnit.values().forEach { u -> FilterChip(selected = selected == u, onClick = { onSelect(u) }, label = { Text(u.label) }) }
    }
}
