package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.domain.DocStatus
import com.odocare.app.domain.Engines
import com.odocare.app.domain.ServiceStatus
import com.odocare.app.ui.*
import java.time.LocalDate

private data class Attention(val vehicleId: String, val route: String, val title: String, val detail: String, val label: String, val colors: Pair<Color, Color>, val score: Float)

/** onOpenVehicle(vehicleId, route): aktifkan kendaraan lalu buka "schedule" atau "documents". */
@Composable
fun DashboardScreen(vm: MainViewModel, onOpenVehicle: (String, String) -> Unit) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val schedules by vm.allSchedules.collectAsStateWithLifecycle()
    val docs by vm.allDocuments.collectAsStateWithLifecycle()
    val records by vm.allRecords.collectAsStateWithLifecycle()
    val odo by vm.allOdometer.collectAsStateWithLifecycle()

    val names = vehicles.associate { it.id to it.name }
    val kmByVehicle = odo.groupBy { it.vehicleId }.mapValues { (_, l) -> l.maxByOrNull { it.recordedAt }?.km }

    val attention = mutableListOf<Attention>()
    schedules.forEach { s ->
        val v = Engines.scheduleView(s, kmByVehicle[s.vehicleId])
        if (v.status == ServiceStatus.LATE || v.status == ServiceStatus.SOON || v.status == ServiceStatus.DUE)
            attention += Attention(s.vehicleId, "schedule", "${s.name} · ${names[s.vehicleId] ?: ""}", scheduleDetail(v), v.status.label, serviceColor(v.status), v.progress)
    }
    docs.forEach { d ->
        val v = Engines.documentView(d)
        if (v.status != DocStatus.SAFE) {
            val score = if (v.daysRemaining < 0) 1f + minOf(-v.daysRemaining, 365L) / 365f else 1f - v.daysRemaining / 30f
            attention += Attention(d.vehicleId, "documents", "${docTypeLabel(d.type)} · ${names[d.vehicleId] ?: ""}", docDaysText(v.daysRemaining), v.status.label, docColor(v.status), score)
        }
    }
    val sorted = attention.sortedByDescending { it.score }

    val today = LocalDate.now()
    val costMonth = records.filter { msToLocalDate(it.date).let { d -> d.year == today.year && d.monthValue == today.monthValue } }.sumOf { it.cost }
    val costYear = records.filter { msToLocalDate(it.date).year == today.year }.sumOf { it.cost }
    val (labels, values) = sixMonthSeries(records)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Dashboard", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("${vehicles.size}", "Kendaraan", Modifier.weight(1f))
            StatCard("${sorted.size}", "Perlu perhatian", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(rupiah(costMonth), "Biaya bulan ini", Modifier.weight(1f))
            StatCard(rupiah(costYear), "Biaya tahun ini", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Perlu Perhatian", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(12.dp))
        if (sorted.isEmpty()) Text("Semua aman.", color = TextGray, fontSize = 14.sp)
        sorted.take(8).forEach { a ->
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenVehicle(a.vehicleId, a.route) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(a.title, color = TextWhite, fontWeight = FontWeight.Bold)
                        Text(a.detail, color = a.colors.second, fontSize = 12.sp)
                    }
                    StatusBadge(a.label, a.colors)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Jatuh Tempo Terdekat", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(12.dp))
        if (docs.isEmpty()) Text("Belum ada dokumen.", color = TextGray, fontSize = 14.sp)
        docs.sortedBy { it.expiryDate }.take(3).forEach { d ->
            val v = Engines.documentView(d)
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenVehicle(d.vehicleId, "documents") }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${docTypeLabel(d.type)} · ${names[d.vehicleId] ?: ""}", color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("${fmtDate(d.expiryDate)} (${docDaysText(v.daysRemaining)})", color = TextGray, fontSize = 12.sp)
                    }
                    StatusBadge(v.status.label, docColor(v.status))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Biaya Service 6 Bulan Terakhir", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(12.dp))
        Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.padding(16.dp)) { BarChart(labels, values) }
        }
        Spacer(Modifier.height(24.dp))
    }
}
