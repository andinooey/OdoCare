@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.LegalDocument
import com.odocare.app.data.MaintenanceSchedule
import com.odocare.app.data.OdometerLog
import com.odocare.app.domain.Catalog
import com.odocare.app.domain.Engines
import com.odocare.app.domain.ServiceStatus
import com.odocare.app.ui.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// ---------- Helpers ----------

private val NoInsets = WindowInsets(0, 0, 0, 0)
private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private fun fmtDate(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)

// DatePicker bekerja dalam UTC, data aplikasi dalam zona waktu perangkat
private fun toPickerMillis(sys: Long): Long = Instant.ofEpochMilli(sys).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun fromPickerMillis(pic: Long): Long = Instant.ofEpochMilli(pic).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun posInt(s: String): Int? = s.toIntOrNull()?.takeIf { it > 0 }

private val docTypes: List<Pair<String, String>> = listOf("PAJAK_TAHUNAN" to "Pajak Tahunan", "STNK_5_TAHUNAN" to "STNK 5 Tahunan", "SIM_A" to "SIM A", "SIM_C" to "SIM C", "ASURANSI" to "Asuransi", "GARANSI" to "Garansi", "LAINNYA" to "Lainnya")
private fun docTypeLabel(k: String): String = docTypes.firstOrNull { it.first == k }?.second ?: k
private fun vehicleTypeLabel(t: String): String = when (t) { "MOBIL" -> "Mobil"; "MOTOR" -> "Motor"; else -> "Lainnya" }
private fun docDaysText(days: Long): String = when { days < 0 -> "Terlambat ${-days} hari"; days == 0L -> "Hari ini"; else -> "$days hari lagi" }

private fun urgency(s: ServiceStatus): Int = when (s) { ServiceStatus.LATE -> 3; ServiceStatus.DUE -> 2; ServiceStatus.SOON -> 1; else -> 0 }

@Composable
private fun appBarColors() = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = value, onValueChange = { onChange(it.filter(Char::isDigit)) }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

@Composable
private fun DatePickerDialogX(initialMillis: Long?, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis?.let { toPickerMillis(it) })
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = state.selectedDateMillis != null, onClick = { state.selectedDateMillis?.let { onPicked(fromPickerMillis(it)) } }) { Text("Pilih") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    ) { DatePicker(state = state) }
}

// ---------- Home ----------

@Composable
fun HomeScreen(vm: MainViewModel, onNavigateToSchedule: () -> Unit, onNavigateToHistory: () -> Unit, onNavigateToDocuments: () -> Unit) {
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val documents by vm.documents.collectAsStateWithLifecycle()
    var showAddKm by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var recordTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    val scheduleViews = schedules.map { Engines.scheduleView(it, km) }
    val hasVehicle = vehicle != null

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, null, tint = LavenderPrimary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Text("OdoCare", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNavigateToDocuments) { Icon(Icons.Default.Description, "Dokumen", tint = Color.White) }
        }
        Spacer(Modifier.height(24.dp))

        val v = vehicle
        if (v == null) {
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Belum ada kendaraan", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Buka tab Vehicle untuk menambahkan kendaraan pertamamu.", color = TextGray, fontSize = 14.sp)
                }
            }
        } else {
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(StatusBlueBg), contentAlignment = Alignment.Center) {
                        Icon(if (v.type == "MOBIL") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler, null, tint = LavenderPrimary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(vehicleTypeLabel(v.type), fontSize = 14.sp, color = TextGray)
                    }
                    Surface(color = LavenderPrimary, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Odometer", color = Color.DarkGray, fontSize = 12.sp)
                            Text(if (km != null) "$km km" else "- km", color = Color.DarkGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(Icons.Default.Speed, "Update\nKM", Modifier.weight(1f), false, hasVehicle) { showAddKm = true }
            ActionButton(Icons.Default.Add, "Catat\nServis", Modifier.weight(1f), true, hasVehicle) { showPicker = true }
            ActionButton(Icons.Default.History, "Riwayat", Modifier.weight(1f), false, hasVehicle) { onNavigateToHistory() }
        }
        Spacer(Modifier.height(24.dp))

        Text("Ringkasan Perawatan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.LATE || it.status == ServiceStatus.DUE }}", "Due / Overdue", StatusRedBg, StatusRedText, Modifier.weight(1f))
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.SOON }}", "Due Soon", StatusGrayBg, StatusGrayText, Modifier.weight(1f))
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.NORMAL || it.status == ServiceStatus.NO_ODOMETER }}", "Good", StatusBlueBg, StatusBlueText, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Dokumen & Pajak", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            Text("Kelola", color = LavenderPrimary, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onNavigateToDocuments))
        }
        Spacer(Modifier.height(12.dp))
        if (documents.isEmpty()) {
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToDocuments)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(StatusBlueBg), contentAlignment = Alignment.Center) { Icon(Icons.Default.AssignmentInd, null, tint = LavenderPrimary) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Pantau Masa Berlaku STNK & SIM", fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Catat tanggal perpanjangan pajak tahunan, ganti plat, atau SIM agar dapat pengingat tepat waktu.", fontSize = 12.sp, color = TextGray)
                    }
                }
            }
        } else {
            documents.take(3).forEach { d ->
                val dv = Engines.documentView(d)
                Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onNavigateToDocuments)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(docTypeLabel(d.type), color = TextWhite, fontWeight = FontWeight.Bold)
                            Text(docDaysText(dv.daysRemaining), color = TextGray, fontSize = 12.sp)
                        }
                        StatusBadge(dv.status.label, docColor(dv.status))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Status Perawatan Komponen", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            Text("Lihat Semua", color = LavenderPrimary, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onNavigateToSchedule))
        }
        Spacer(Modifier.height(12.dp))
        if (schedules.isEmpty()) {
            Text("Belum ada jadwal perawatan. Tambahkan di tab Maintenance.", color = TextGray, fontSize = 14.sp)
        }
        scheduleViews.sortedByDescending { urgency(it.status) }.take(3).forEach { view ->
            val colors = serviceColor(view.status)
            val urgent = view.status == ServiceStatus.LATE || view.status == ServiceStatus.DUE
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(view.schedule.name, color = TextWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Surface(color = colors.first, shape = RoundedCornerShape(50)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (urgent) Icons.Default.Warning else Icons.Default.CheckCircle, null, tint = colors.second, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(view.status.label, color = colors.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAddKm) {
        var kmText by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddKm = false }, title = { Text("Update Odometer") },
            text = { NumberField(kmText, { kmText = it }, "Kilometer") },
            confirmButton = { TextButton(enabled = posInt(kmText) != null, onClick = { posInt(kmText)?.let { vm.addOdometer(it) }; showAddKm = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAddKm = false }) { Text("Batal") } })
    }
    if (showPicker) {
        AlertDialog(onDismissRequest = { showPicker = false }, title = { Text("Servis apa yang dicatat?") },
            text = {
                if (schedules.isEmpty()) Text("Belum ada jadwal perawatan. Tambahkan dulu di tab Maintenance.")
                else Column(Modifier.verticalScroll(rememberScrollState())) {
                    schedules.forEach { s -> TextButton(onClick = { showPicker = false; recordTarget = s }) { Text(s.name) } }
                }
            },
            confirmButton = { if (schedules.isEmpty()) TextButton(onClick = { showPicker = false; onNavigateToSchedule() }) { Text("Buka Jadwal") } },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal") } })
    }
    recordTarget?.let { s ->
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, c, n ->
            vm.recordService(s, kmV, System.currentTimeMillis(), c, n); recordTarget = null
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, isPrimary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(72.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (isPrimary) LavenderPrimary else Color.Transparent, contentColor = if (isPrimary) Color(0xFF0D1B3E) else TextWhite),
        border = if (!isPrimary) BorderStroke(1.dp, Color.Gray) else null, contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun SummaryCard(value: String, label: String, bgColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(color = bgColor, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(label, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

// ---------- Odometer ----------

@Composable
fun OdometerScreen(vm: MainViewModel) {
    val logs by vm.odometerLogs.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<OdometerLog?>(null) }
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Riwayat Odometer", color = TextWhite) }, colors = appBarColors()) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") } }
    ) { pad ->
        if (logs.isEmpty()) {
            Column(Modifier.padding(pad).padding(16.dp)) { Text("Belum ada catatan.", color = TextGray) }
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)) {
                items(logs, key = { it.id }) { log ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${log.km} km", style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.Bold)
                                Text(fmtDate(log.recordedAt), style = MaterialTheme.typography.bodySmall, color = TextGray)
                            }
                            IconButton(onClick = { editTarget = log }) { Icon(Icons.Default.Edit, "Edit", tint = TextGray) }
                            IconButton(onClick = { vm.deleteOdometer(log.id) }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        var kmText by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Catat Odometer") },
            text = { NumberField(kmText, { kmText = it }, "Kilometer") },
            confirmButton = { TextButton(enabled = posInt(kmText) != null, onClick = { posInt(kmText)?.let { vm.addOdometer(it) }; showAdd = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } })
    }
    editTarget?.let { log ->
        var kmText by remember { mutableStateOf(log.km.toString()) }
        AlertDialog(onDismissRequest = { editTarget = null }, title = { Text("Edit Odometer") },
            text = { NumberField(kmText, { kmText = it }, "Kilometer") },
            confirmButton = { TextButton(enabled = posInt(kmText) != null, onClick = { posInt(kmText)?.let { vm.updateOdometer(log.copy(km = it)) }; editTarget = null }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Batal") } })
    }
}

// ---------- Schedule ----------

@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var recordTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var editTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var deleteTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Jadwal Servis", color = TextWhite) }, colors = appBarColors()) },
        floatingActionButton = {
            if (vehicle != null) Column(horizontalAlignment = Alignment.End) {
                OutlinedButton(onClick = { showCatalog = true }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Katalog", color = LavenderPrimary) }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") }
            }
        }
    ) { pad ->
        if (schedules.isEmpty()) {
            Column(Modifier.padding(pad).padding(16.dp)) { Text(if (vehicle == null) "Tambahkan kendaraan dulu di tab Vehicle." else "Belum ada jadwal. Tap Katalog untuk rekomendasi.", color = TextGray) }
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 160.dp)) {
                items(schedules, key = { it.id }) { s ->
                    val view = Engines.scheduleView(s, km)
                    val colors = serviceColor(view.status)
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                                StatusBadge(view.status.label, colors)
                            }
                            Spacer(Modifier.height(8.dp))
                            val parts = mutableListOf<String>()
                            s.intervalKm?.let { parts += "tiap $it km" }; s.intervalMonths?.let { parts += "tiap $it bulan" }
                            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextGray)
                            view.kmRemaining?.let { Text(if (it >= 0) "Sisa $it km" else "Lewat ${-it} km", style = MaterialTheme.typography.bodySmall, color = TextGray) }
                            view.daysRemaining?.let { Text(if (it >= 0) "Sisa $it hari" else "Lewat ${-it} hari", style = MaterialTheme.typography.bodySmall, color = TextGray) }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { recordTarget = s }, colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = Color.Black), shape = RoundedCornerShape(8.dp)) { Text("Catat Servis") }
                                IconButton(onClick = { editTarget = s }) { Icon(Icons.Default.Edit, "Edit", tint = TextGray) }
                                IconButton(onClick = { deleteTarget = s }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }; var kmT by remember { mutableStateOf("") }; var moT by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Tambah Jadwal") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, singleLine = true); Spacer(Modifier.height(8.dp))
                    NumberField(kmT, { kmT = it }, "Interval km"); Spacer(Modifier.height(8.dp))
                    NumberField(moT, { moT = it }, "Interval bulan")
                }
            },
            confirmButton = { TextButton(enabled = name.isNotBlank() && (posInt(kmT) != null || posInt(moT) != null), onClick = { vm.addSchedule(name, posInt(kmT), posInt(moT)); showAdd = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } })
    }
    if (showCatalog) {
        val items = Catalog.itemsFor(vehicle?.type ?: "MOBIL")
        val existing = schedules.map { it.name }.toSet()
        var selected by remember { mutableStateOf(setOf<String>()) }
        AlertDialog(onDismissRequest = { showCatalog = false }, title = { Text("Rekomendasi") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEach { item ->
                        val already = item.name in existing
                        FilterChip(selected = item.name in selected, enabled = !already,
                            onClick = { selected = if (item.name in selected) selected - item.name else selected + item.name },
                            label = { Text(if (already) "${item.name} (ada)" else item.name) })
                    }
                }
            },
            confirmButton = { TextButton(enabled = selected.isNotEmpty(), onClick = { vm.addSchedulesFromCatalog(items.filter { it.name in selected }); showCatalog = false }) { Text("Tambah") } },
            dismissButton = { TextButton(onClick = { showCatalog = false }) { Text("Batal") } })
    }
    recordTarget?.let { s ->
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, c, n -> vm.recordService(s, kmV, System.currentTimeMillis(), c, n); recordTarget = null }
    }
    editTarget?.let { s ->
        var name by remember { mutableStateOf(s.name) }; var kmT by remember { mutableStateOf(s.intervalKm?.toString() ?: "") }
        var moT by remember { mutableStateOf(s.intervalMonths?.toString() ?: "") }; var lKm by remember { mutableStateOf(s.lastServiceKm?.toString() ?: "") }
        AlertDialog(onDismissRequest = { editTarget = null }, title = { Text("Edit Jadwal") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, singleLine = true); Spacer(Modifier.height(8.dp))
                    NumberField(kmT, { kmT = it }, "Interval km"); Spacer(Modifier.height(8.dp))
                    NumberField(moT, { moT = it }, "Interval bulan"); Spacer(Modifier.height(8.dp))
                    NumberField(lKm, { lKm = it }, "Servis terakhir (km)")
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && (posInt(kmT) != null || posInt(moT) != null), onClick = {
                    vm.updateSchedule(s.copy(name = name.trim(), intervalKm = posInt(kmT), intervalMonths = posInt(moT), lastServiceKm = lKm.toIntOrNull()))
                    editTarget = null
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Batal") } })
    }
    deleteTarget?.let { s ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Hapus jadwal?") }, text = { Text("\"${s.name}\" akan dihapus.") },
            confirmButton = { TextButton(onClick = { vm.deleteSchedule(s.id); deleteTarget = null }) { Text("Hapus", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } })
    }
}

@Composable
fun RecordServiceDialog(schedule: MaintenanceSchedule?, initialKm: Int?, onDismiss: () -> Unit, onConfirm: (Int, Long, String?) -> Unit) {
    var kmText by remember { mutableStateOf(initialKm?.toString() ?: "") }
    var costText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (schedule != null) "Catat Servis: ${schedule.name}" else "Catat Servis") },
        text = {
            Column {
                NumberField(kmText, { kmText = it }, "Kilometer"); Spacer(Modifier.height(8.dp))
                NumberField(costText, { costText = it }, "Biaya (Rp)"); Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") })
            }
        },
        confirmButton = { TextButton(enabled = posInt(kmText) != null, onClick = { posInt(kmText)?.let { onConfirm(it, costText.toLongOrNull() ?: 0L, notes.ifBlank { null }) } }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

// ---------- Documents ----------

@Composable
fun DocumentsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val docs by vm.documents.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var renewTarget by remember { mutableStateOf<LegalDocument?>(null) }
    var deleteTarget by remember { mutableStateOf<LegalDocument?>(null) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(title = { Text("Dokumen", color = TextWhite) }, colors = appBarColors(),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextWhite) } })
        },
        floatingActionButton = { if (vehicle != null) FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") } }
    ) { pad ->
        if (docs.isEmpty()) {
            Column(Modifier.padding(pad).padding(16.dp)) { Text(if (vehicle == null) "Tambahkan kendaraan dulu di tab Vehicle." else "Belum ada dokumen.", color = TextGray) }
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)) {
                items(docs, key = { it.id }) { d ->
                    val view = Engines.documentView(d)
                    val colors = docColor(view.status)
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(docTypeLabel(d.type), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = TextWhite)
                                StatusBadge(view.status.label, colors)
                            }
                            if (!d.number.isNullOrBlank()) Text("No. ${d.number}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                            Text("Jatuh tempo: ${fmtDate(d.expiryDate)} (${docDaysText(view.daysRemaining)})", style = MaterialTheme.typography.bodySmall, color = TextGray)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { renewTarget = d }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Perpanjang", color = LavenderPrimary) }
                                IconButton(onClick = { deleteTarget = d }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var type by remember { mutableStateOf(docTypes.first().first) }
        var number by remember { mutableStateOf("") }
        var expiry by remember { mutableStateOf<Long?>(null) }
        var menu by remember { mutableStateOf(false) }
        var picking by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Tambah Dokumen") },
            text = {
                Column {
                    Box {
                        OutlinedButton(onClick = { menu = true }) { Text(docTypeLabel(type)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            docTypes.forEach { (k, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { type = k; menu = false }) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { picking = true }) { Text(expiry?.let { "Jatuh tempo: ${fmtDate(it)}" } ?: "Pilih tanggal jatuh tempo") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Nomor (opsional)") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(enabled = expiry != null, onClick = { expiry?.let { vm.addDocument(type, it, number.ifBlank { null }) }; showAdd = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } })
        if (picking) DatePickerDialogX(expiry, { picking = false }) { expiry = it; picking = false }
    }
    renewTarget?.let { d ->
        val base = maxOf(Instant.ofEpochMilli(d.expiryDate).atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now())
        val suggestion = base.plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        DatePickerDialogX(suggestion, { renewTarget = null }) { vm.renewDocument(d, it); renewTarget = null }
    }
    deleteTarget?.let { d ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Hapus dokumen?") }, text = { Text("${docTypeLabel(d.type)} akan dihapus beserta riwayat perpanjangannya.") },
            confirmButton = { TextButton(onClick = { vm.deleteDocument(d.id); deleteTarget = null }) { Text("Hapus", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } })
    }
}

// ---------- Vehicles / Settings ----------

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val active by vm.activeVehicle.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<com.odocare.app.data.Vehicle?>(null) }
    Scaffold(contentWindowInsets = NoInsets, topBar = { TopAppBar(title = { Text("Kendaraan", color = TextWhite) }, colors = appBarColors()) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard("Kendaraan") {
                vehicles.forEach { v ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${v.name} (${vehicleTypeLabel(v.type)})", Modifier.weight(1f), color = TextWhite, fontWeight = if (v.id == active?.id) FontWeight.Bold else FontWeight.Normal)
                        if (v.id != active?.id) TextButton(onClick = { vm.setActiveVehicle(v.id) }) { Text("Aktifkan", color = LavenderPrimary) }
                        IconButton(onClick = { deleteTarget = v }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = Color.Black)) { Text("Tambah Kendaraan") }
            }
            SectionCard("Notifikasi") {
                OutlinedButton(onClick = { vm.testNotification() }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Kirim Uji Notifikasi", color = LavenderPrimary) }
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }; var type by remember { mutableStateOf("MOTOR") }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Tambah Kendaraan") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, singleLine = true); Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("MOBIL", "MOTOR", "LAINNYA").forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(vehicleTypeLabel(t)) }) } }
                }
            },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { vm.addVehicle(name, type); showAdd = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } })
    }
    deleteTarget?.let { v ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Hapus kendaraan?") },
            text = { Text("\"${v.name}\" beserta seluruh odometer, jadwal servis, dan dokumennya akan dihapus permanen.") },
            confirmButton = { TextButton(onClick = { vm.deleteVehicle(v.id); deleteTarget = null }) { Text("Hapus", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } })
    }
}
