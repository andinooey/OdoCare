package com.odocare.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odocare.app.ui.DateRangePickerDialogX
import com.odocare.app.ui.fmtDate
import com.odocare.app.ui.Spacing
import com.odocare.app.ui.msToLocalDate
import java.time.LocalDate
import java.time.ZoneId

/** Pilihan periode untuk filter tanggal di Histori dan Laporan. */
enum class Period(val label: String) { MONTH("Bulan ini"), M3("3 bulan"), YEAR("Tahun ini"), M12("12 bulan"), ALL("Semua"), CUSTOM("Pilih tanggal") }

fun Period.start(today: LocalDate): LocalDate? = when (this) {
    Period.MONTH -> today.withDayOfMonth(1)
    Period.M3 -> today.minusMonths(3)
    Period.YEAR -> today.withDayOfYear(1)
    Period.M12 -> today.minusMonths(12)
    Period.ALL -> null
    Period.CUSTOM -> null   // rentang kustom disimpan di PeriodFilter
}

fun fmtLocal(d: LocalDate): String = fmtDate(d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

/** Status filter tanggal: periode terpilih + rentang kustom. Tidak disimpan setelah layar ditutup. */
@Stable
class PeriodFilter(initial: Period) {
    var period by mutableStateOf(initial)
    var customStart by mutableStateOf<LocalDate?>(null)
    var customEnd by mutableStateOf<LocalDate?>(null)
    val isCustom: Boolean get() = period == Period.CUSTOM
    /** Batas awal (inklusif); null = tanpa batas. */
    val start: LocalDate? get() = if (isCustom) customStart else period.start(LocalDate.now())
    /** Batas akhir (inklusif), hanya untuk rentang kustom; periode lain berakhir "sekarang" tanpa batas. */
    val endBound: LocalDate? get() = if (isCustom) customEnd else null
    fun accepts(ms: Long): Boolean {
        val d = msToLocalDate(ms); val s = start; val e = endBound
        return (s == null || !d.isBefore(s)) && (e == null || !d.isAfter(e))
    }
}

/** Baris chip periode + dialog rentang tanggal. Dipakai bersama oleh Histori dan Laporan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodFilterRow(f: PeriodFilter, customFirst: Boolean = false) {
    var showPicker by remember { mutableStateOf(false) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
        // customFirst: "Pilih tanggal" jadi pilihan pertama
        val order = if (customFirst) listOf(Period.CUSTOM) + Period.values().filter { it != Period.CUSTOM } else Period.values().toList()
        order.forEach { p ->
            if (p == Period.CUSTOM) {
                val s = f.customStart; val e = f.customEnd
                FilterChip(
                    selected = f.isCustom, onClick = { showPicker = true },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)) },
                    label = { Text(if (f.isCustom && s != null && e != null) "${fmtLocal(s)} – ${fmtLocal(e)}" else p.label, fontSize = 12.sp) }
                )
            } else FilterChip(selected = f.period == p, onClick = { f.period = p }, label = { Text(p.label, fontSize = 12.sp) })
        }
    }
    if (showPicker) DateRangePickerDialogX(f.customStart, f.customEnd, { showPicker = false }) { s, e ->
        f.customStart = s; f.customEnd = e; f.period = Period.CUSTOM; showPicker = false
    }
}
