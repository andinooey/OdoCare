package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.LegalDocument
import com.odocare.app.data.MaintenanceSchedule
import com.odocare.app.data.OdometerLog
import com.odocare.app.data.Vehicle
import com.odocare.app.domain.Catalog
import com.odocare.app.domain.CatalogItem
import com.odocare.app.domain.Engines
import com.odocare.app.domain.ServiceStatus
import com.odocare.app.ui.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private fun fmtDate(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)
private fun toPickerMillis(sys: Long): Long = Instant.ofEpochMilli(sys).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun fromPickerMillis(pic: Long): Long = Instant.ofEpochMilli(pic).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private val docTypes: List<Pair<String, String>> = listOf("PAJAK_TAHUNAN" to "Pajak Tahunan", "STNK_5_TAHUNAN" to "STNK 5 Tahunan", "SIM_A" to "SIM A", "SIM_C" to "SIM C", "ASURANSI" to "Asuransi", "GARANSI" to "Garansi", "LAINNYA" to "Lainnya")
private fun docTypeLabel(k: String): String = docTypes.firstOrNull { it.first == k }?.second ?: k

@Composable
fun HomeScreen(vm: MainViewModel, onNavigateToSchedule: () -> Unit) {
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    var showAddKm by remember { mutableStateOf(false) }
    var showRecordService by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TwoWheeler, null, tint = LavenderPrimary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Text("MyBike", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Description, null, tint = Color.White)
        }
        Spacer(Modifier.height(24.dp))

        if (vehicle != null) {
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(StatusBlueBg), contentAlignment = Alignment.Center) { Icon(Icons.Default.TwoWheeler, null, tint = LavenderPrimary, modifier = Modifier.size(40.dp)) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(vehicle!!.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("2026", fontSize = 16.sp, color = TextGray)
                        Text("\"${vehicle!!.type.lowercase()}\"", fontSize = 14.sp, color = TextGray)
                        Spacer(Modifier.height(4.dp))
                        Surface(color = Color.Black, shape = RoundedCornerShape(4.dp)) { Text("B 3868 CTY", color = TextWhite, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                    }
                    Surface(color = LavenderPrimary, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Odometer", color = Color.DarkGray, fontSize = 12.sp)
                            Text("${km ?: 0} km", color = Color.DarkGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(Icons.Default.Speed, "Update\nKM", Modifier.weight(1f), false) { showAddKm = true }
            ActionButton(Icons.Default.Add, "Catat\nServis", Modifier.weight(1f), true) { showRecordService = true }
            ActionButton(Icons.Default.History, "Riwayat", Modifier.weight(1f), false) { onNavigateToSchedule() }
        }
        Spacer(Modifier.height(24.dp))

        Text("Ringkasan Perawatan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(Modifier.height(12.dp))
        val scheduleViews = schedules.map { Engines.scheduleView(it, km) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.LATE || it.status == ServiceStatus.DUE }}", "Due / Overdue", StatusRedBg, StatusRedText, Modifier.weight(1f))
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.SOON }}", "Due Soon", StatusGrayBg, StatusGrayText, Modifier.weight(1f))
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.NORMAL || it.status == ServiceStatus.NO_ODOMETER }}", "Good", StatusBlueBg, StatusBlueText, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterVertically) {
            Text("Dokumen & Pajak", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            Text("Kelola", color = LavenderPrimary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(StatusBlueBg), contentAlignment = Alignment.Center) { Icon(Icons.Default.AssignmentInd, null, tint = LavenderPrimary) }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Pantau Masa Berlaku STNK & SIM", fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("Catat tanggal perpanjangan pajak tahunan, ganti plat, atau SIM agar dapat pengingat tepat waktu.", fontSize = 12.sp, color = TextGray)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterVertically) {
            Text("Status Perawatan Komponen", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            Text("Lihat Semua", color = LavenderPrimary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        schedules.take(3).forEach { s ->
            val view = Engines.scheduleView(s, km)
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, color = TextWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    val colors = serviceColor(view.status)
                    Surface(color = colors.first, shape = RoundedCornerShape(50)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = colors.second, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(view.status.label, color = colors.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }

    if (showAddKm) {
        var kmText by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddKm = false }, title = { Text("Update Odometer") }, text = {
            OutlinedTextField(value = kmText, onValueChange = { kmText = it.filter { c -> c.isDigit() } }, label = { Text("Kilometer") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }, confirmButton = { TextButton(onClick = { kmText.toIntOrNull()?.let { vm.addOdometer(it) }; showAddKm = false }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { showAddKm = false }) { Text("Batal") } })
    }
    if (showRecordService) {
        RecordServiceDialog(schedule = null, onDismiss = { showRecordService = false }) { kmV, c, n ->
            vm.addOdometer(kmV)
            showRecordService = false
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, isPrimary: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(72.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isPrimary) LavenderPrimary else Color.Transparent, contentColor = if (isPrimary) Color(0xFF0D1B3E) else TextWhite), border = if (!isPrimary) androidx.compose.foundation.BorderStroke(1.dp, Color.Gray) else null, contentPadding = PaddingValues(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun SummaryCard(value: String, label: String, bgColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(color = bgColor, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(label, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun OdometerScreen(vm: MainViewModel) {
    val logs by vm.odometerLogs.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<OdometerLog?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Riwayat Odometer", color = TextWhite) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }, floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") } }) { pad ->
        if (logs.isEmpty()) {
            Column(Modifier.padding(pad).padding(16.dp)) { Text("Belum ada catatan.", color = TextGray) }
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
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
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Catat Odometer") }, text = {
            OutlinedTextField(value = kmText, onValueChange = { kmText = it.filter { c -> c.isDigit() } }, label = { Text("Kilometer") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }, confirmButton = { TextButton(onClick = { kmText.toIntOrNull()?.let { vm.addOdometer(it) }; showAdd = false }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } })
    }
    editTarget?.let { log ->
        var kmText by remember { mutableStateOf(log.km.toString()) }
        AlertDialog(onDismissRequest = { editTarget = null }, title = { Text("Edit Odometer") }, text = {
            OutlinedTextField(value = kmText, onValueChange = { kmText = it.filter(Char::isDigit) }, label = { Text("Kilometer") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }, confirmButton = { TextButton(enabled = kmText.toIntOrNull()?.let { it > 0 } == true, onClick = { kmText.toIntOrNull()?.let { vm.updateOdometer(log.copy(km = it)) }; editTarget = null }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Batal") } })
    }
}

@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var recordTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var editTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Jadwal Servis", color = TextWhite) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }, floatingActionButton = {
        Column(horizontalAlignment = Alignment.End) {
            OutlinedButton(onClick = { showCatalog = true }, border = androidx.compose.foundation.BorderStroke(1.dp, LavenderPrimary)) { Text("Katalog", color = LavenderPrimary) }
            Spacer(Modifier.height(8.dp))
            FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") }
        }
    }) { pad ->
        if (schedules.isEmpty()) { Column(Modifier.padding(pad).padding(16.dp)) { Text("Belum ada jadwal.", color = TextGray) } }
        else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp, bottom = 120.dp)) {
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
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(onClick = { recordTarget = s }, colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = Color.Black), shape = RoundedCornerShape(8.dp)) { Text("Catat Servis") }
                                IconButton(onClick = { editTarget = s }) { Icon(Icons.Default.Edit, "Edit", tint = TextGray) }
                                IconButton(onClick = { vm.deleteSchedule(s.id) }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }; var kmT by remember { mutableStateOf("") }; var moT by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Tambah Jadwal") }, text = {
            Column { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }); Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = kmT, onValueChange = { kmT = it.filter(Char::isDigit) }, label = { Text("Interval km") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = moT, onValueChange = { moT = it.filter(Char::isDigit) }, label = { Text("Interval bulan") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        }, confirmButton = { TextButton(onClick = { vm.addSchedule(name, kmT.toIntOrNull(), moT.toIntOrNull()); showAdd = false }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } })
    }
    if (showCatalog) {
        val items = Catalog.itemsFor(vm.activeVehicle.value?.type ?: "MOBIL")
        val existing = schedules.map { it.name }.toSet()
        val selected = remember { mutableStateOf(setOf<String>()) }
        AlertDialog(onDismissRequest = { showCatalog = false }, title = { Text("Rekomendasi") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                items.forEach { item -> val alr = item.name in existing; Row(verticalAlignment = Alignment.CenterVertically) { FilterChip(selected = item.name in selected.value, enabled = !alr, onClick = { selected.value = if (item.name in selected.value) selected.value - item.name else selected.value + item.name }, label = { Text(if (alr) "${item.name} (ada)" else item.name) }) } }
            }
        }, confirmButton = { TextButton(enabled = selected.value.isNotEmpty(), onClick = { vm.addSchedulesFromCatalog(items.filter { it.name in selected.value }); showCatalog = false }) { Text("Tambah") } }, dismissButton = { TextButton(onClick = { showCatalog = false }) { Text("Batal") } })
    }
    recordTarget?.let { s -> RecordServiceDialog(schedule = s, onDismiss = { recordTarget = null }) { kmV, c, n -> vm.recordService(s, kmV, System.currentTimeMillis(), c, n); recordTarget = null } }
    editTarget?.let { s ->
        var name by remember { mutableStateOf(s.name) }; var kmT by remember { mutableStateOf(s.intervalKm?.toString() ?: "") }; var moT by remember { mutableStateOf(s.intervalMonths?.toString() ?: "") }; var lKm by remember { mutableStateOf(s.lastServiceKm?.toString() ?: "") }
        AlertDialog(onDismissRequest = { editTarget = null }, title = { Text("Edit Jadwal") }, text = {
            Column { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }); Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = kmT, onValueChange = { kmT = it.filter(Char::isDigit) }, label = { Text("Interval km") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = moT, onValueChange = { moT = it.filter(Char::isDigit) }, label = { Text("Interval bulan") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        }, confirmButton = { TextButton(onClick = { vm.updateSchedule(s.copy(name = name, intervalKm = kmT.toIntOrNull(), intervalMonths = moT.toIntOrNull())); editTarget = null }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Batal") } })
    }
}

@Composable
fun RecordServiceDialog(schedule: MaintenanceSchedule?, onDismiss: () -> Unit, onConfirm: (Int, Long, String?) -> Unit) {
    var kmText by remember { mutableStateOf("") }; var costText by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Catat Servis") }, text = {
        Column { OutlinedTextField(value = kmText, onValueChange = { kmText = it.filter(Char::isDigit) }, label = { Text("Kilometer") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = costText, onValueChange = { costText = it.filter(Char::isDigit) }, label = { Text("Biaya (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") }) }
    }, confirmButton = { TextButton(onClick = { onConfirm(kmText.toIntOrNull() ?: 0, costText.toLongOrNull() ?: 0L, notes.ifBlank { null }) }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

@Composable
fun DocumentsScreen(vm: MainViewModel) {
    val docs by vm.documents.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Dokumen", color = TextWhite) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }, floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") } }) { pad ->
        if (docs.isEmpty()) { Column(Modifier.padding(pad).padding(16.dp)) { Text("Belum ada dokumen.", color = TextGray) } }
        else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(docs, key = { it.id }) { d ->
                    val view = Engines.documentView(d)
                    val colors = docColor(view.status)
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(docTypeLabel(d.type), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = TextWhite); StatusBadge(view.status.label, colors) }
                            Text("Jatuh tempo: ${fmtDate(d.expiryDate)}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { /* Renew Logic */ }, border = androidx.compose.foundation.BorderStroke(1.dp, LavenderPrimary)) { Text("Perpanjang", color = LavenderPrimary) }
                                IconButton(onClick = { vm.deleteDocument(d.id) }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val active by vm.activeVehicle.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Kendaraan", color = TextWhite) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard("Kendaraan") {
                vehicles.forEach { v ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${v.name} (${v.type})", Modifier.weight(1f), color = TextWhite, fontWeight = if (v.id == active?.id) FontWeight.Bold else FontWeight.Normal)
                        if (v.id != active?.id) TextButton(onClick = { vm.setActiveVehicle(v.id) }) { Text("Aktifkan", color = LavenderPrimary) }
                        IconButton(onClick = { vm.deleteVehicle(v.id) }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = Color.Black)) { Text("Tambah Kendaraan") }
            }
            SectionCard("Notifikasi") {
                OutlinedButton(onClick = { vm.testNotification() }, border = androidx.compose.foundation.BorderStroke(1.dp, LavenderPrimary)) { Text("Kirim Uji Notifikasi", color = LavenderPrimary) }
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }; var type by remember { mutableStateOf("MOTOR") }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Tambah Kendaraan") }, text = {
            Column { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }); Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("MOBIL", "MOTOR", "LAINNYA").forEach { FilterChip(selected = type == it, onClick = { type = it }, label = { Text(it) }) } } }
        }, confirmButton = { TextButton(onClick = { vm.addVehicle(name, type); showAdd = false }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } })
    }
}
