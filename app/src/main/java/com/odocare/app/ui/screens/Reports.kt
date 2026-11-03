package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

/** Perbandingan biaya servis per bulan: tahun ini (sampai bulan berjalan) dengan seluruh bulan tahun lalu. BBM tidak ikut. */
private data class YearCompare(val current: List<Long>, val previous: List<Long>, val previousToDate: Long, val hasData: Boolean)

private val WhitespaceRun = Regex("\\s+")
private fun tidyTitle(t: String) = t.trim().replace(WhitespaceRun, " ")

@Composable
fun ReportScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val allRecords by vm.allRecords.collectAsStateWithLifecycle()
    val allFuel by vm.allFuel.collectAsStateWithLifecycle()
    var vehicleFilter by remember { mutableStateOf<String?>(null) }
    val pf = remember { PeriodFilter(Period.YEAR) }
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
    val last = records.maxOfOrNull { it.date }
    // Kategori digabung tanpa beda huruf besar-kecil dan spasi; yang ditampilkan ejaan yang paling sering dipakai
    val perCategory = records.groupBy { tidyTitle(it.title).lowercase() }.map { (_, l) ->
        val label = l.groupingBy { tidyTitle(it.title) }.eachCount().maxByOrNull { it.value }?.key ?: l.first().title
        label to l.sumOf { it.cost }
    }.sortedByDescending { it.second }
    val perVehicle = records.groupBy { it.vehicleId }.mapValues { (_, l) -> l.sumOf { it.cost } }.toList().sortedByDescending { it.second }
    val costedCount = records.count { it.cost > 0 }
    val avgCost = if (costedCount > 0) total / costedCount else null
    val priciest = records.maxByOrNull { it.cost }?.takeIf { it.cost > 0 }
    // Periode pembanding yang sepadan: bulan ini vs tanggal yang sama bulan lalu, tahun ini vs tanggal yang sama tahun lalu,
    // 3 bulan / 12 bulan / rentang kustom vs rentang sama panjang tepat sebelumnya. "Semua" tidak punya pembanding.
    val prevRange: Pair<LocalDate, LocalDate>? = run {
        val st = start
        when (pf.period) {
            Period.MONTH -> st?.let { it.minusMonths(1) to today.minusMonths(1) }
            Period.YEAR -> st?.let { it.minusYears(1) to today.minusYears(1) }
            Period.M3 -> st?.let { it.minusMonths(3) to it.minusDays(1) }
            Period.M12 -> st?.let { it.minusMonths(12) to it.minusDays(1) }
            Period.CUSTOM -> {
                val cs = pf.customStart; val ce = pf.customEnd
                if (cs != null && ce != null) { val len = ChronoUnit.DAYS.between(cs, ce) + 1; cs.minusDays(len) to cs.minusDays(1) } else null
            }
            Period.ALL -> null
        }
    }
    val prevLabel = when (pf.period) {
        Period.MONTH -> "periode sama bulan lalu"; Period.YEAR -> "periode sama tahun lalu"
        Period.M3 -> "3 bulan sebelumnya"; Period.M12 -> "12 bulan sebelumnya"; else -> "periode sebelumnya"
    }
    val prevTotal = prevRange?.let { r -> byVehicle.filter { val d = msToLocalDate(it.date); !d.isBefore(r.first) && !d.isAfter(r.second) }.sumOf { it.cost } } ?: 0L
    val delta = if (prevRange != null && prevTotal > 0) ((total - prevTotal) * 100.0 / prevTotal).roundToInt() else null

    // Biaya servis tahun ini vs tahun lalu: tidak mengikuti filter periode, hanya filter kendaraan
    val thisYear = today.year; val lastYear = thisYear - 1
    val yearCompare = remember(allRecords, activeFilter, today) {
        val cur = LongArray(12); val prev = LongArray(12); var prevToDate = 0L
        fun add(ms: Long, cost: Long, vid: String) {
            if (activeFilter != null && vid != activeFilter) return
            val d = msToLocalDate(ms)
            if (d.year == thisYear) cur[d.monthValue - 1] += cost
            else if (d.year == lastYear) {
                prev[d.monthValue - 1] += cost
                if (d.monthValue < today.monthValue || (d.monthValue == today.monthValue && d.dayOfMonth <= today.dayOfMonth)) prevToDate += cost
            }
        }
        allRecords.forEach { add(it.date, it.cost, it.vehicleId) }
        YearCompare(cur.take(today.monthValue), prev.toList(), prevToDate, cur.any { it > 0 } || prev.any { it > 0 })
    }
    val yearMonthLabels = remember { (1..12).map { monthShort(java.time.Month.of(it)) } }
    // Grafik mengikuti rentang yang dipilih ("Semua" mulai dari catatan tertua). Ketiga grafik memakai rentang yang sama agar sumbunya sejajar.
    val fuelTotal = fuelInRange.sumOf { it.totalPrice }
    val chartStart = start ?: (records.map { it.date } + fuelInRange.map { it.date }).minOrNull()?.let { msToLocalDate(it) } ?: today
    val chartEnd = endBound ?: today
    val servicePoints = records.map { it.date to it.cost }
    val fuelPoints = fuelInRange.map { it.date to it.totalPrice }
    val serviceSeries = if (records.isEmpty()) null else costSeries(servicePoints, chartStart, chartEnd)
    val fuelSeries = if (fuelInRange.isEmpty()) null else costSeries(fuelPoints, chartStart, chartEnd)
    val combinedSeries = if (records.isEmpty() || fuelInRange.isEmpty()) null else costSeries(servicePoints + fuelPoints, chartStart, chartEnd)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.L)) {
        Spacer(Modifier.height(Spacing.L))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextWhite, modifier = Modifier.size(22.dp)) }
            ScreenTitle("Laporan", "Ringkasan biaya servis & BBM", Modifier.weight(1f))
            IconButton(enabled = records.isNotEmpty(), onClick = {
                try { Exports.shareFile(ctx, "odocare-laporan.csv", "text/csv", Exports.csvBytes(records, vehicles)) } catch (e: Exception) { info = "Gagal mengekspor: ${e.message}" }
            }) { Icon(Icons.Default.FileDownload, "Ekspor CSV", tint = if (records.isNotEmpty()) LavenderPrimary else TextGray, modifier = Modifier.size(22.dp)) }
        }
        Spacer(Modifier.height(Spacing.M))

        if (vehicles.isEmpty()) {
            NoVehicleState(vm, Modifier.padding(top = Spacing.XL), "Tambahkan kendaraan dan catat servis untuk melihat laporan biaya.")
        } else {
            PeriodFilterRow(pf, customFirst = true)
            Spacer(Modifier.height(Spacing.XS))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                FilterChip(selected = activeFilter == null, onClick = { vehicleFilter = null }, label = { Text("Semua", fontSize = 12.sp) })
                vehicles.forEach { v -> FilterChip(selected = activeFilter == v.id, onClick = { vehicleFilter = v.id }, label = { Text(v.name, fontSize = 12.sp) }) }
            }

            if (records.isEmpty() && fuelInRange.isEmpty()) {
                EmptyState(Icons.Default.BarChart, "Belum ada data", "Tidak ada servis atau BBM pada periode ini. Catat servis dari tab Servis, BBM dari tombol Catat BBM di Home, atau pilih periode lain.", Modifier.padding(top = Spacing.XL))
            } else {
                // ===== Kelompok SERVIS =====
                if (records.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.L))
                    GroupHeader("Servis", Icons.Default.Build)
                    Spacer(Modifier.height(Spacing.S))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                        StatCard(rupiah(total), "Total biaya servis", Modifier.weight(1f), Icons.Default.Payments)
                        StatCard("$count", "Jumlah servis", Modifier.weight(1f), Icons.Default.Build)
                    }
                    Spacer(Modifier.height(Spacing.S))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                        StatCard(avgCost?.let { rupiah(it) } ?: "-", "Rata-rata per servis", Modifier.weight(1f), Icons.Default.BarChart)
                        StatCard(last?.let { fmtDate(it) } ?: "-", "Servis terakhir", Modifier.weight(1f), Icons.Default.Event)
                    }
                    // Perbandingan dengan periode sebelumnya dan servis termahal
                    if (delta != null) {
                        Spacer(Modifier.height(Spacing.XS))
                        Text(
                            when {
                                delta > 0 -> "Naik $delta% dibanding $prevLabel (${rupiah(prevTotal)})"
                                delta < 0 -> "Turun ${-delta}% dibanding $prevLabel (${rupiah(prevTotal)})"
                                else -> "Sama dengan $prevLabel (${rupiah(prevTotal)})"
                            },
                            color = when { delta > 0 -> StatusRedText; delta < 0 -> StatusGreenText; else -> TextGray },
                            fontSize = 12.sp
                        )
                    }
                    if (priciest != null) {
                        Spacer(Modifier.height(Spacing.XS))
                        Text("Servis termahal: ${priciest.title} · ${rupiah(priciest.cost)} · ${fmtDate(priciest.date)}", color = TextGray, fontSize = 12.sp)
                    }

                    // Suku cadang vs Jasa: hanya dari servis yang dirinci
                    val dirinci = records.filter { it.hasBreakdown }
                    if (dirinci.isNotEmpty()) {
                        val parts = dirinci.sumOf { it.partsCost }
                        val labor = dirinci.sumOf { it.laborCost }
                        val tanpaRincian = records.filter { !it.hasBreakdown && it.cost > 0 }
                        Spacer(Modifier.height(Spacing.L))
                        SectionHeader("Suku cadang vs Jasa")
                        Spacer(Modifier.height(Spacing.XS))
                        Surface(color = SurfaceVariantDark, shape = CardShape, border = CardBorder, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.M)) {
                                PercentBar("Suku cadang", parts, parts + labor)
                                PercentBar("Jasa", labor, parts + labor)
                                Spacer(Modifier.height(Spacing.XS))
                                Text(
                                    "Dari ${dirinci.size} servis yang dirinci." + if (tanpaRincian.isNotEmpty()) " ${tanpaRincian.size} servis (${rupiah(tanpaRincian.sumOf { it.cost })}) tanpa rincian tidak dihitung." else "",
                                    color = TextGray, fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.L))
                    SectionHeader("Biaya per Kategori")
                    Spacer(Modifier.height(Spacing.XS))
                    perCategory.forEach { (name, v) -> PercentBar(name, v, total) }

                    if (perVehicle.size > 1) {
                        Spacer(Modifier.height(Spacing.M))
                        SectionHeader("Biaya per Kendaraan")
                        Spacer(Modifier.height(Spacing.XS))
                        perVehicle.forEach { (vid, v) -> PercentBar(names[vid] ?: "-", v, total) }
                    }

                    if (serviceSeries != null) {
                        Spacer(Modifier.height(Spacing.M))
                        SectionHeader("Biaya Servis " + serviceSeries.title.removePrefix("Biaya "))
                        Spacer(Modifier.height(Spacing.S))
                        ChartCard { LineChart(serviceSeries.labels, serviceSeries.values, LavenderPrimary) }
                    }

                }

                // ===== Kelompok BBM =====
                if (fuelInRange.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.XL))
                    GroupHeader("BBM", Icons.Default.LocalGasStation)
                    Spacer(Modifier.height(Spacing.S))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                        StatCard(rupiah(fuelStats.totalCost), "Total biaya BBM", Modifier.weight(1f), Icons.Default.LocalGasStation)
                        StatCard(fuelStats.kmPerLiter?.let { "${fmtKmPerL(it)} km/L" } ?: "-", "Rata-rata km / liter", Modifier.weight(1f), Icons.Default.Speed)
                    }
                    Spacer(Modifier.height(Spacing.S))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                        StatCard(fuelStats.costPerKm?.let { "${rupiah(Math.round(it))} / km" } ?: "-", "Biaya per km", Modifier.weight(1f), Icons.Default.Payments)
                        StatCard("${fuelStats.count}x · ${fmtLiter(fuelStats.totalLiters)} L", "Pengisian · total bensin", Modifier.weight(1f), Icons.Default.Event)
                    }
                    Spacer(Modifier.height(Spacing.XS))
                    Text(
                        if (fuelStats.kmPerLiter == null) "Km per liter dan biaya per km butuh minimal 2 pengisian \"tangki penuh\" berurutan pada periode ini."
                        else "Dihitung dari ${fmtKm(fuelStats.measuredKm)} km di antara pengisian tangki penuh.",
                        color = TextGray, fontSize = 12.sp
                    )
                    if (fuelSeries != null) {
                        Spacer(Modifier.height(Spacing.M))
                        SectionHeader("Biaya BBM " + fuelSeries.title.removePrefix("Biaya "))
                        Spacer(Modifier.height(Spacing.S))
                        ChartCard { LineChart(fuelSeries.labels, fuelSeries.values, StatusBlueText) }
                    }
                }

                // ===== Kelompok GABUNGAN (hanya bila servis dan BBM sama-sama ada) =====
                if (combinedSeries != null) {
                    Spacer(Modifier.height(Spacing.XL))
                    GroupHeader("Servis + BBM", Icons.Default.Payments)
                    Spacer(Modifier.height(Spacing.S))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                        StatCard(rupiah(total + fuelTotal), "Total biaya gabungan", Modifier.weight(1f), Icons.Default.Payments)
                    }
                    Spacer(Modifier.height(Spacing.XS))
                    Text("Servis ${rupiah(total)} + BBM ${rupiah(fuelTotal)}", color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(Spacing.M))
                    SectionHeader("Biaya Gabungan " + combinedSeries.title.removePrefix("Biaya "))
                    Spacer(Modifier.height(Spacing.S))
                    ChartCard { LineChart(combinedSeries.labels, combinedSeries.values, StatusGreenText) }
                }
            }

            // ===== Kelompok TAHUN INI vs TAHUN LALU (tidak mengikuti filter periode) =====
            if (yearCompare.hasData) {
                Spacer(Modifier.height(Spacing.XL))
                GroupHeader("Biaya servis: tahun ini vs tahun lalu", Icons.Default.Build)
                Spacer(Modifier.height(Spacing.S))
                ChartCard {
                    Column {
                        CompareLineChart(yearMonthLabels, yearCompare.current, yearCompare.previous, StatusGreenText, TextGray)
                        Spacer(Modifier.height(Spacing.S))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M), verticalAlignment = Alignment.CenterVertically) {
                            LegendDot(StatusGreenText, "$thisYear")
                            LegendDot(TextGray, "$lastYear")
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.XS))
                val curSum = yearCompare.current.sum()
                val prevSum = yearCompare.previousToDate
                val untilMonth = monthShort(today.month)
                if (prevSum > 0) {
                    val yd = ((curSum - prevSum) * 100.0 / prevSum).roundToInt()
                    Text("Jan–$untilMonth: ${rupiah(curSum)} vs ${rupiah(prevSum)} di periode sama tahun lalu", color = TextGray, fontSize = 12.sp)
                    Text(
                        when { yd > 0 -> "Naik $yd%"; yd < 0 -> "Turun ${-yd}%"; else -> "Sama" },
                        color = when { yd > 0 -> StatusRedText; yd < 0 -> StatusGreenText; else -> TextGray }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text("Jan–$untilMonth: ${rupiah(curSum)}. Belum ada data tahun lalu pada periode yang sama.", color = TextGray, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(Spacing.XL))
    }
    info?.let { InfoDialog(it) { info = null } }
}

/** Judul kelompok laporan (Servis / BBM / gabungan): ikon, judul tebal, dan garis pemisah di bawahnya. */
@Composable
private fun GroupHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.S))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
        Spacer(Modifier.height(Spacing.S))
        Box(Modifier.fillMaxWidth().height(1.dp).background(TrackColor))
    }
}

@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    Surface(color = SurfaceVariantDark, shape = CardShape, border = CardBorder, modifier = Modifier.fillMaxWidth()) { Box(Modifier.padding(Spacing.M)) { content() } }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(Spacing.XS))
        Text(label, fontSize = 12.sp, color = TextGray)
    }
}
