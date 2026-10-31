package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.ui.*
import com.odocare.app.util.Exports
import java.time.LocalDate

private enum class Period(val label: String) { MONTH("Bulan ini"), M3("3 bulan terakhir"), YEAR("Tahun ini"), M12("12 bulan terakhir"), ALL("Semua") }

private fun Period.start(today: LocalDate): LocalDate? = when (this) {
    Period.MONTH -> today.withDayOfMonth(1)
    Period.M3 -> today.minusMonths(3)
    Period.YEAR -> today.withDayOfYear(1)
    Period.M12 -> today.minusMonths(12)
    Period.ALL -> null
}

@Composable
fun ReportScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val allRecords by vm.allRecords.collectAsStateWithLifecycle()
    var vehicleFilter by remember { mutableStateOf<String?>(null) }
    var period by remember { mutableStateOf(Period.YEAR) }
    var info by remember { mutableStateOf<String?>(null) }

    val activeFilter = vehicleFilter?.takeIf { id -> vehicles.any { it.id == id } }
    val names = vehicles.associate { it.id to it.name }
    val byVehicle = allRecords.filter { activeFilter == null || it.vehicleId == activeFilter }
    val start = period.start(LocalDate.now())
    val records = byVehicle.filter { start == null || !msToLocalDate(it.date).isBefore(start) }

    val total = records.sumOf { it.cost }
    val count = records.size
    val average = if (count > 0) total / count else 0L
    val last = records.maxOfOrNull { it.date }
    val perCategory = records.groupBy { it.title }.mapValues { (_, l) -> l.sumOf { it.cost } }.toList().sortedByDescending { it.second }
    val perVehicle = records.groupBy { it.vehicleId }.mapValues { (_, l) -> l.sumOf { it.cost } }.toList().sortedByDescending { it.second }
    val (labels, values) = sixMonthSeries(byVehicle)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Laporan", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = activeFilter == null, onClick = { vehicleFilter = null }, label = { Text("Semua kendaraan") })
            vehicles.forEach { v -> FilterChip(selected = activeFilter == v.id, onClick = { vehicleFilter = v.id }, label = { Text(v.name) }) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Period.values().forEach { p -> FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) }) }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(rupiah(total), "Total biaya", Modifier.weight(1f))
            StatCard("$count", "Jumlah service", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(rupiah(average), "Rata-rata per service", Modifier.weight(1f))
            StatCard(last?.let { fmtDate(it) } ?: "-", "Service terakhir", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Biaya per Kategori", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(8.dp))
        if (perCategory.isEmpty()) Text("Belum ada data pada periode ini.", color = TextGray, fontSize = 14.sp)
        perCategory.forEach { (name, v) -> PercentBar(name, v, total) }

        Spacer(Modifier.height(16.dp))
        Text("Biaya per Kendaraan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(8.dp))
        perVehicle.forEach { (vid, v) -> PercentBar(names[vid] ?: "-", v, total) }

        Spacer(Modifier.height(16.dp))
        Text("Biaya 6 Bulan Terakhir", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(12.dp))
        Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Box(Modifier.padding(16.dp)) { BarChart(labels, values) } }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Transaksi (50 terbaru)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.weight(1f))
            OutlinedButton(enabled = records.isNotEmpty(), onClick = {
                try { Exports.shareFile(ctx, "odocare-laporan.csv", "text/csv", Exports.csvBytes(records, vehicles)) } catch (e: Exception) { info = "Gagal mengekspor: ${e.message}" }
            }) { Text("Ekspor CSV") }
        }
        Spacer(Modifier.height(8.dp))
        records.sortedByDescending { it.date }.take(50).forEach { r ->
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, color = TextWhite, fontWeight = FontWeight.Medium)
                        Text("${fmtDate(r.date)} · ${names[r.vehicleId] ?: "-"} · ${fmtKm(r.km)} km", color = TextGray, fontSize = 12.sp)
                    }
                    Text(rupiah(r.cost), color = LavenderPrimary, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    info?.let { InfoDialog(it) { info = null } }
}
