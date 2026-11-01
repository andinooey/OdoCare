package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.odocare.app.data.OdometerLog
import com.odocare.app.domain.FuelEngine
import com.odocare.app.ui.*
import com.odocare.app.util.Exports
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private data class KmStats(val avgPerMonth: Int?, val totalKm: Int, val days: Long, val perVehicle: List<Pair<String, Int>>)

/**
 * Rata-rata KM per bulan dari log odometer dalam rentang. Dihitung per kendaraan (jarak KM awal ke KM akhir / jumlah hari x 30),
 * lalu dijumlahkan agar KM kendaraan berbeda tidak tercampur. Kendaraan dengan kurang dari 2 catatan di rentang itu dilewati.
 */
private fun kmStats(logs: List<OdometerLog>, start: LocalDate?, end: LocalDate?): KmStats {
    var total = 0; var sumMonthly = 0.0; var maxDays = 0L
    val per = mutableListOf<Pair<String, Int>>()
    for ((vid, all) in logs.groupBy { it.vehicleId }) {
        val inRange = all.filter { val d = msToLocalDate(it.recordedAt); (start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end)) }.sortedBy { it.recordedAt }
        if (inRange.size < 2) continue
        val first = inRange.first(); val last = inRange.last()
        val dist = (last.km - first.km).coerceAtLeast(0)
        val days = ChronoUnit.DAYS.between(msToLocalDate(first.recordedAt), msToLocalDate(last.recordedAt)).coerceAtLeast(1)
        val monthly = dist * 30.0 / days
        total += dist; maxDays = maxOf(maxDays, days); sumMonthly += monthly
        per += vid to monthly.roundToInt()
    }
    return KmStats(if (per.isEmpty()) null else sumMonthly.roundToInt(), total, maxDays, per)
}

@Composable
fun ReportScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val allRecords by vm.allRecords.collectAsStateWithLifecycle()
    val allFuel by vm.allFuel.collectAsStateWithLifecycle()
    var vehicleFilter by remember { mutableStateOf<String?>(null) }
    val pf = remember { PeriodFilter(Period.YEAR) }
    val allOdometer by vm.allOdometer.collectAsStateWithLifecycle()
    var info by remember { mutableStateOf<String?>(null) }

    val activeFilter = vehicleFilter?.takeIf { id -> vehicles.any { it.id == id } }
    val names = vehicles.associate { it.id to it.name }
    val byVehicle = allRecords.filter { activeFilter == null || it.vehicleId == activeFilter }
    val today = LocalDate.now()
    val start = pf.start
    val endBound = pf.endBound   // batas akhir hanya untuk rentang kustom
    val records = byVehicle.filter { pf.accepts(it.date) }
    // BBM: total dihitung dari catatan di rentang; ruas km/liter dihitung dari SEMUA catatan kendaraan lalu disaring menurut tanggal akhir ruas
    val fuelByVehicle = allFuel.filter { activeFilter == null || it.vehicleId == activeFilter }
    val fuelInRange = fuelByVehicle.filter { pf.accepts(it.date) }
    val fuelStats = FuelEngine.stats(fuelInRange, FuelEngine.segments(fuelByVehicle).filter { pf.accepts(it.endDate) })

    val total = records.sumOf { it.cost }
    val count = records.size
    val km = kmStats(allOdometer.filter { activeFilter == null || it.vehicleId == activeFilter }, start, endBound)
    val last = records.maxOfOrNull { it.date }
    val perCategory = records.groupBy { it.title }.mapValues { (_, l) -> l.sumOf { it.cost } }.toList().sortedByDescending { it.second }
    val perVehicle = records.groupBy { it.vehicleId }.mapValues { (_, l) -> l.sumOf { it.cost } }.toList().sortedByDescending { it.second }
    // Grafik mengikuti rentang yang dipilih ("Semua" mulai dari servis tertua)
    val series = if (records.isEmpty()) null else costSeries(records, start ?: records.minOf { msToLocalDate(it.date) }, endBound ?: today)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextPrimary, modifier = Modifier.size(22.dp)) }
            ScreenTitle("Laporan", "Ringkasan biaya servis & BBM", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        if (vehicles.isEmpty()) {
            NoVehicleState(vm, Modifier.padding(top = 24.dp), "Tambahkan kendaraan dan catat servis untuk melihat laporan biaya.")
        } else {
            PeriodFilterRow(pf, customFirst = true)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = activeFilter == null, onClick = { vehicleFilter = null }, label = { Text("Semua", fontSize = 12.sp) })
                vehicles.forEach { v -> FilterChip(selected = activeFilter == v.id, onClick = { vehicleFilter = v.id }, label = { Text(v.name, fontSize = 12.sp) }) }
            }

            if (records.isEmpty() && fuelInRange.isEmpty()) {
                EmptyState(Icons.Default.BarChart, "Belum ada data", "Tidak ada servis atau BBM pada periode ini. Catat servis dari tab Servis, BBM dari tombol Catat BBM di Home, atau pilih periode lain.", Modifier.padding(top = 24.dp))
            } else {
                Spacer(Modifier.height(12.dp))
                if (records.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(rupiah(total), "Total biaya", Modifier.weight(1f), Icons.Default.Payments)
                    StatCard("$count", "Jumlah servis", Modifier.weight(1f), Icons.Default.Build)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(km.avgPerMonth?.let { "${fmtKm(it)} km" } ?: "-", "Rata-rata KM / bulan", Modifier.weight(1f), Icons.Default.Speed)
                    StatCard(last?.let { fmtDate(it) } ?: "-", "Servis terakhir", Modifier.weight(1f), Icons.Default.Event)
                }
                // Keterangan rata-rata KM: dasar hitungan, atau alasan kenapa belum bisa dihitung
                Spacer(Modifier.height(4.dp))
                if (km.avgPerMonth == null) {
                    Text("Rata-rata KM butuh minimal 2 catatan KM pada periode ini.", color = TextMuted, fontSize = 12.sp)
                } else {
                    val basis = when {
                        km.days < 7 -> "perkiraan, data baru ${km.days} hari"
                        km.perVehicle.size == 1 -> "dalam ${km.days} hari"
                        else -> "dari semua kendaraan"
                    }
                    Text("Dari ${fmtKm(km.totalKm)} km tercatat, $basis.", color = TextMuted, fontSize = 12.sp)
                    if (km.perVehicle.size > 1) Text(km.perVehicle.joinToString(" · ") { (vid, v) -> "${names[vid] ?: "-"} ${fmtKm(v)}" } + " km/bulan", color = TextMuted, fontSize = 12.sp)
                }

                // Suku cadang vs Jasa: hanya dari servis yang dirinci
                val dirinci = records.filter { it.hasBreakdown }
                if (dirinci.isNotEmpty()) {
                    val parts = dirinci.sumOf { it.partsCost }
                    val labor = dirinci.sumOf { it.laborCost }
                    val tanpaRincian = records.filter { !it.hasBreakdown && it.cost > 0 }
                    Spacer(Modifier.height(16.dp))
                    SectionHeader("Suku cadang vs Jasa")
                    Spacer(Modifier.height(4.dp))
                    Surface(color = CardBg, shape = CardShape, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            PercentBar("Suku cadang", parts, parts + labor)
                            PercentBar("Jasa", labor, parts + labor)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Dari ${dirinci.size} servis yang dirinci." + if (tanpaRincian.isNotEmpty()) " ${tanpaRincian.size} servis (${rupiah(tanpaRincian.sumOf { it.cost })}) tanpa rincian tidak dihitung." else "",
                                color = TextMuted, fontSize = 12.sp
                            )
                        }
                    }
                }
                }   // akhir blok servis (kartu statistik)

                if (fuelInRange.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader("BBM")
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(rupiah(fuelStats.totalCost), "Total biaya BBM", Modifier.weight(1f), Icons.Default.LocalGasStation)
                        StatCard(fuelStats.kmPerLiter?.let { "${fmtKmPerL(it)} km/L" } ?: "-", "Rata-rata km / liter", Modifier.weight(1f), Icons.Default.Speed)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(fuelStats.costPerKm?.let { "${rupiah(Math.round(it))} / km" } ?: "-", "Biaya per km", Modifier.weight(1f), Icons.Default.Payments)
                        StatCard("${fuelStats.count}x · ${fmtLiter(fuelStats.totalLiters)} L", "Pengisian · total bensin", Modifier.weight(1f), Icons.Default.Event)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (fuelStats.kmPerLiter == null) "Km per liter dan biaya per km butuh minimal 2 pengisian \"tangki penuh\" berurutan pada periode ini."
                        else "Dihitung dari ${fmtKm(fuelStats.measuredKm)} km di antara pengisian tangki penuh.",
                        color = TextMuted, fontSize = 12.sp
                    )
                }

                if (records.isNotEmpty()) {
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
                if (series != null) {
                    SectionHeader(series.title)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = CardBg, shape = CardShape, modifier = Modifier.fillMaxWidth()) { Box(Modifier.padding(12.dp)) { BarChart(series.labels, series.values) } }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Transaksi terbaru", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        try { Exports.shareFile(ctx, "odocare-laporan.csv", "text/csv", Exports.csvBytes(records, vehicles)) } catch (e: Exception) { info = "Gagal mengekspor: ${e.message}" }
                    }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(30.dp)) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp), tint = Accent)
                        Spacer(Modifier.width(4.dp))
                        Text("CSV", fontSize = 12.sp, color = Accent)
                    }
                }
                Spacer(Modifier.height(4.dp))
                records.sortedByDescending { it.date }.take(50).forEach { r ->
                    Surface(color = CardBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${fmtDate(r.date)} · ${names[r.vehicleId] ?: "-"} · ${fmtKm(r.km)} km", color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (r.cost > 0) rupiah(r.cost) else "-", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                }   // akhir blok servis (kategori, grafik, transaksi)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    info?.let { InfoDialog(it) { info = null } }
}

