package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.ui.*
import com.odocare.app.util.Exports
import java.time.LocalDate

private enum class Period(val label: String) { MONTH("Bulan ini"), M3("3 bulan"), YEAR("Tahun ini"), M12("12 bulan"), ALL("Semua") }

private fun Period.start(today: LocalDate): LocalDate? = when (this) {
    Period.MONTH -> today.withDayOfMonth(1)
    Period.M3 -> today.minusMonths(3)
    Period.YEAR -> today.withDayOfYear(1)
    Period.M12 -> today.minusMonths(12)
    Period.ALL -> null
}

@Composable
fun ReportScreen(vm: MainViewModel, onOpenHistory: () -> Unit) {
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

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        // Ikon riwayat servis di kanan atas
        ScreenTitle("Laporan", "Ringkasan biaya servis") {
            IconButton(enabled = vehicles.isNotEmpty(), onClick = {
                activeFilter?.let { vm.setActiveVehicle(it) }
                onOpenHistory()
            }) { Icon(Icons.Default.History, "Riwayat servis", tint = if (vehicles.isNotEmpty()) LavenderPrimary else TextGray, modifier = Modifier.size(24.dp)) }
        }
        Spacer(Modifier.height(10.dp))

        if (vehicles.isEmpty()) {
            NoVehicleState(vm, Modifier.padding(top = 24.dp), "Tambahkan kendaraan dan catat servis untuk melihat laporan biaya.")
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = activeFilter == null, onClick = { vehicleFilter = null }, label = { Text("Semua", fontSize = 12.sp) })
                vehicles.forEach { v -> FilterChip(selected = activeFilter == v.id, onClick = { vehicleFilter = v.id }, label = { Text(v.name, fontSize = 12.sp) }) }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Period.values().forEach { p -> FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label, fontSize = 12.sp) }) }
            }

            if (records.isEmpty()) {
                EmptyState(Icons.Default.BarChart, "Belum ada data servis", "Tidak ada servis pada periode ini. Catat servis dari tab Servis atau pilih periode lain.", Modifier.padding(top = 24.dp))
            } else {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(rupiah(total), "Total biaya", Modifier.weight(1f), Icons.Default.Payments)
                    StatCard("$count", "Jumlah servis", Modifier.weight(1f), Icons.Default.Build)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(rupiah(average), "Rata-rata", Modifier.weight(1f), Icons.Default.Calculate)
                    StatCard(last?.let { fmtDate(it) } ?: "-", "Servis terakhir", Modifier.weight(1f), Icons.Default.Event)
                }

                Spacer(Modifier.height(16.dp))
                SectionHeader("Biaya per Kategori")
                Spacer(Modifier.height(4.dp))
                perCategory.forEach { (name, v) -> PercentBar(name, v, total) }

                if (perVehicle.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Biaya per Kendaraan")
                    Spacer(Modifier.height(4.dp))
                    perVehicle.forEach { (vid, v) -> PercentBar(names[vid] ?: "-", v, total) }
                }

                Spacer(Modifier.height(12.dp))
                SectionHeader("6 Bulan Terakhir")
                Spacer(Modifier.height(8.dp))
                Surface(color = SurfaceVariantDark, shape = CardShape, modifier = Modifier.fillMaxWidth()) { Box(Modifier.padding(12.dp)) { BarChart(labels, values) } }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Transaksi terbaru", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        try { Exports.shareFile(ctx, "odocare-laporan.csv", "text/csv", Exports.csvBytes(records, vehicles)) } catch (e: Exception) { info = "Gagal mengekspor: ${e.message}" }
                    }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(30.dp)) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp), tint = LavenderPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text("CSV", fontSize = 12.sp, color = LavenderPrimary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                records.sortedByDescending { it.date }.take(50).forEach { r ->
                    Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${fmtDate(r.date)} · ${names[r.vehicleId] ?: "-"} · ${fmtKm(r.km)} km", color = TextGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (r.cost > 0) rupiah(r.cost) else "-", color = LavenderPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    info?.let { InfoDialog(it) { info = null } }
}

