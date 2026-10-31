@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.MaintenanceSchedule
import com.odocare.app.data.MasterCategory
import com.odocare.app.data.OdometerLog
import com.odocare.app.domain.Engines
import com.odocare.app.domain.ServiceStatus
import com.odocare.app.ui.*

// ---------- Home ----------

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNavigateToSchedule: () -> Unit, onNavigateToHistory: () -> Unit,
    onNavigateToDocuments: () -> Unit, onNavigateToOdometer: () -> Unit
) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
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
        Spacer(Modifier.height(16.dp))

        // Pindah kendaraan aktif langsung dari Home (hanya tampil bila punya lebih dari satu)
        if (vehicles.size > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vehicles.forEach { item -> FilterChip(selected = item.id == vehicle?.id, onClick = { vm.setActiveVehicle(item.id) }, label = { Text(item.name) }) }
            }
            Spacer(Modifier.height(12.dp))
        }

        val v = vehicle
        Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            if (v == null) {
                Column(Modifier.padding(20.dp)) {
                    Text("Belum ada kendaraan", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Buka tab Kendaraan untuk menambahkan kendaraan pertamamu.", color = TextGray, fontSize = 14.sp)
                }
            } else {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(StatusBlueBg), contentAlignment = Alignment.Center) {
                            Icon(if (v.type == "MOBIL") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler, null, tint = LavenderPrimary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(vehicleTypeLabel(v.type), fontSize = 14.sp, color = TextGray)
                        }
                        if (v.plate.isNotBlank()) PlateView(v.plate)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.clickable(onClick = onNavigateToOdometer), verticalAlignment = Alignment.CenterVertically) {
                        OdometerDigits(km)
                        Spacer(Modifier.width(8.dp))
                        Text("km", color = TextGray, fontSize = 14.sp)
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
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.LATE }}", "Lewat", StatusRedBg, StatusRedText, Modifier.weight(1f))
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.SOON || it.status == ServiceStatus.DUE }}", "Segera", StatusYellowBg, StatusYellowText, Modifier.weight(1f))
            SummaryCard("${scheduleViews.count { it.status == ServiceStatus.NORMAL || it.status == ServiceStatus.NO_ODOMETER }}", "Aman", StatusGreenBg, StatusGreenText, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Dokumen & Pajak", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            Text("Kelola", color = LavenderPrimary, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onNavigateToDocuments))
        }
        Spacer(Modifier.height(12.dp))
        if (documents.isEmpty()) {
            Text("Belum ada dokumen. Tap Kelola untuk menambahkan pajak, SIM, atau asuransi.", color = TextGray, fontSize = 14.sp)
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
        if (schedules.isEmpty()) Text("Belum ada jadwal perawatan. Tambahkan di tab Servis.", color = TextGray, fontSize = 14.sp)
        scheduleViews.sortedByDescending { it.progress }.take(3).forEach { view ->
            val colors = serviceColor(view.status)
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(view.schedule.name, color = TextWhite, fontWeight = FontWeight.Bold)
                        Text(scheduleDetail(view), color = TextGray, fontSize = 12.sp)
                    }
                    StatusBadge(view.status.label, colors)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAddKm) AddKmDialog(km, { showAddKm = false }) { vm.addOdometer(it); showAddKm = false }
    if (showPicker) {
        AlertDialog(onDismissRequest = { showPicker = false }, title = { Text("Servis apa yang dicatat?") },
            text = {
                if (schedules.isEmpty()) Text("Belum ada jadwal perawatan. Tambahkan dulu di tab Servis.")
                else Column(Modifier.verticalScroll(rememberScrollState())) {
                    schedules.forEach { s -> TextButton(onClick = { showPicker = false; recordTarget = s }) { Text(s.name) } }
                }
            },
            confirmButton = { if (schedules.isEmpty()) TextButton(onClick = { showPicker = false; onNavigateToSchedule() }) { Text("Buka Jadwal") } },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal") } })
    }
    recordTarget?.let { s ->
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, date, cost, notes ->
            vm.recordService(s, kmV, date, cost, notes); recordTarget = null
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
fun OdometerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val logs by vm.odometerLogs.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<OdometerLog?>(null) }
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Riwayat Odometer", onBack) },
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
                                Text("${fmtKm(log.km)} km", style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.Bold)
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
    if (showAdd) AddKmDialog(km, { showAdd = false }) { vm.addOdometer(it); showAdd = false }
    editTarget?.let { log ->
        var kmText by remember { mutableStateOf(log.km.toString()) }
        AlertDialog(onDismissRequest = { editTarget = null }, title = { Text("Edit Odometer") },
            text = { NumberField(kmText, { kmText = it }, "Kilometer") },
            confirmButton = { TextButton(enabled = posInt(kmText) != null, onClick = { posInt(kmText)?.let { vm.updateOdometer(log.copy(km = it)) }; editTarget = null }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Batal") } })
    }
}

// ---------- Jadwal / kategori servis ----------

@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val masters by vm.masters.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var recordTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var editTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var deleteTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    // Paling mendesak di atas
    val views = schedules.map { Engines.scheduleView(it, km) }.sortedByDescending { it.progress }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Jadwal Servis", color = TextWhite) }, colors = appBarColors()) },
        floatingActionButton = { if (vehicle != null) FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") } }
    ) { pad ->
        if (views.isEmpty()) {
            Column(Modifier.padding(pad).padding(16.dp)) { Text(if (vehicle == null) "Tambahkan kendaraan dulu di tab Kendaraan." else "Belum ada kategori. Tap + untuk menambah.", color = TextGray) }
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)) {
                items(views, key = { it.schedule.id }) { view ->
                    val s = view.schedule
                    val colors = serviceColor(view.status)
                    StatusStripeCard(colors.second) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                            StatusBadge(view.status.label, colors)
                        }
                        Spacer(Modifier.height(8.dp))
                        ProgressBar(view.progress, colors.second)
                        Spacer(Modifier.height(6.dp))
                        Text(scheduleDetail(view), style = MaterialTheme.typography.bodyMedium, color = colors.second)
                        val parts = mutableListOf<String>()
                        s.intervalKm?.let { parts += "tiap ${fmtKm(it)} km" }; s.intervalMonths?.let { parts += "tiap $it bulan" }
                        if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { recordTarget = s }, colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = Color.Black), shape = RoundedCornerShape(8.dp)) { Text("Sudah service") }
                            IconButton(onClick = { editTarget = s }) { Icon(Icons.Default.Edit, "Edit", tint = TextGray) }
                            IconButton(onClick = { deleteTarget = s }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        val options = masters.filter { it.vehicleType == "ALL" || it.vehicleType == vehicle?.type }
        ScheduleFormDialog("Tambah Kategori", options, null, km, { showAdd = false }) { n, ik, im, lk, ld -> vm.addSchedule(n, ik, im, lk, ld); showAdd = false }
    }
    editTarget?.let { s ->
        ScheduleFormDialog("Edit Kategori", null, s, km, { editTarget = null }) { n, ik, im, lk, ld ->
            vm.updateSchedule(s.copy(name = n, intervalKm = ik, intervalMonths = im, lastServiceKm = lk, lastServiceDate = ld)); editTarget = null
        }
    }
    recordTarget?.let { s ->
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, date, cost, notes -> vm.recordService(s, kmV, date, cost, notes); recordTarget = null }
    }
    deleteTarget?.let { s -> ConfirmDialog("Hapus kategori?", "\"${s.name}\" akan dihapus.", "Hapus", { vm.deleteSchedule(s.id); deleteTarget = null }, { deleteTarget = null }) }
}

private const val CUSTOM = "__custom__"

/** Mode tambah: dropdown master (sesuai jenis kendaraan) + "Lainnya". Mode edit (masters == null): nama bebas. */
@Composable
private fun ScheduleFormDialog(
    title: String, masters: List<MasterCategory>?, initial: MaintenanceSchedule?, currentKm: Int?,
    onDismiss: () -> Unit, onSave: (name: String, intervalKm: Int?, intervalMonths: Int?, lastKm: Int?, lastDate: Long) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kmT by remember { mutableStateOf(initial?.intervalKm?.toString() ?: "") }
    var moT by remember { mutableStateOf(initial?.intervalMonths?.toString() ?: "") }
    var lastKm by remember { mutableStateOf((initial?.lastServiceKm ?: currentKm)?.toString() ?: "") }
    var lastDate by remember { mutableStateOf(initial?.lastServiceDate ?: todayMillis()) }
    var selected by remember { mutableStateOf<String?>(if (masters == null) CUSTOM else null) }
    var menu by remember { mutableStateOf(false) }
    val showName = selected == CUSTOM
    val canSave = name.isNotBlank() && (posInt(kmT) != null || posInt(moT) != null)

    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (masters != null) {
                    Box {
                        OutlinedButton(onClick = { menu = true }) { Text(when (selected) { null -> "Pilih kategori"; CUSTOM -> "Lainnya (ketik sendiri)"; else -> name }) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            masters.forEach { m ->
                                DropdownMenuItem(text = { Text(m.name) }, onClick = {
                                    selected = m.id; name = m.name
                                    kmT = m.defaultKm.takeIf { it > 0 }?.toString() ?: ""; moT = m.defaultMonths.takeIf { it > 0 }?.toString() ?: ""
                                    menu = false
                                })
                            }
                            DropdownMenuItem(text = { Text("Lainnya (ketik sendiri)") }, onClick = { selected = CUSTOM; name = ""; kmT = ""; moT = ""; menu = false })
                        }
                    }
                }
                if (showName) OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kategori") }, singleLine = true)
                if (selected != null) {
                    NumberField(kmT, { kmT = it }, "Interval km (kosong = tidak dipakai)")
                    NumberField(moT, { moT = it }, "Interval bulan (kosong = tidak dipakai)")
                    NumberField(lastKm, { lastKm = it }, "KM terakhir diservis")
                    DateField("Tanggal terakhir diservis", lastDate) { lastDate = it }
                }
            }
        },
        confirmButton = { TextButton(enabled = selected != null && canSave, onClick = { onSave(name.trim(), posInt(kmT), posInt(moT), lastKm.toIntOrNull(), lastDate) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

/** Form tanggal, KM, biaya, catatan. Dipakai untuk "Sudah service" dan edit riwayat. */
@Composable
fun ServiceFormDialog(
    title: String, initialKm: Int?, initialDate: Long, initialCost: Long, initialNotes: String?,
    onDismiss: () -> Unit, onConfirm: (km: Int, date: Long, cost: Long, notes: String?) -> Unit
) {
    var kmText by remember { mutableStateOf(initialKm?.toString() ?: "") }
    var date by remember { mutableStateOf(initialDate) }
    var costText by remember { mutableStateOf(if (initialCost > 0) initialCost.toString() else "") }
    var notes by remember { mutableStateOf(initialNotes ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Tanggal", date) { date = it }
                NumberField(kmText, { kmText = it }, "Kilometer")
                NumberField(costText, { costText = it }, "Biaya (Rp)")
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") })
            }
        },
        confirmButton = { TextButton(enabled = posInt(kmText) != null, onClick = { posInt(kmText)?.let { onConfirm(it, date, costText.toLongOrNull() ?: 0L, notes.ifBlank { null }) } }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

@Composable
fun RecordServiceDialog(schedule: MaintenanceSchedule?, initialKm: Int?, onDismiss: () -> Unit, onConfirm: (km: Int, date: Long, cost: Long, notes: String?) -> Unit) {
    ServiceFormDialog(if (schedule != null) "Sudah service: ${schedule.name}" else "Catat Servis", initialKm, todayMillis(), 0L, null, onDismiss, onConfirm)
}
