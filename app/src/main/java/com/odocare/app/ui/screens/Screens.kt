@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
import com.odocare.app.data.ImageStore
import com.odocare.app.domain.ScheduleView
import com.odocare.app.domain.ServiceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            Text("Status Perawatan Komponen", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            Text("Lihat Semua", color = LavenderPrimary, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onNavigateToSchedule))
        }
        Spacer(Modifier.height(12.dp))
        if (schedules.isEmpty()) Text("Belum ada jadwal perawatan. Tambahkan di tab Servis.", color = TextGray, fontSize = 14.sp)
        scheduleViews.sortedByDescending { it.progress }.take(3).forEach { view ->
            val colors = serviceColor(view.status)
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(view.schedule.name, color = TextWhite, fontWeight = FontWeight.Bold)
                            val interval = intervalLabel(view.schedule.intervalKm, view.schedule.intervalMonths)
                            if (interval.isNotEmpty()) Text(interval, color = TextGray, fontSize = 12.sp)
                        }
                        StatusBadge(view.status.label, colors)
                    }
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(view.progress, colors.second)
                    Spacer(Modifier.height(6.dp))
                    Text(scheduleDetail(view), color = colors.second, fontSize = 12.sp)
                }
            }
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
            documents.take(5).forEach { d ->
                val dv = Engines.documentView(d)
                Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onNavigateToDocuments)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(docTypeLabel(d.type), color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("Jatuh tempo: ${fmtDate(d.expiryDate)}", color = TextWhite, fontSize = 13.sp)
                            Text(docDaysText(dv.daysRemaining), color = TextGray, fontSize = 12.sp)
                        }
                        StatusBadge(dv.status.label, docColor(dv.status))
                    }
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
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, date, cost, notes, img ->
            vm.recordService(s, kmV, date, cost, notes, img); recordTarget = null
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
        floatingActionButton = { AddFab { showAdd = true } }
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
                            ItemMenu(onEdit = { editTarget = log }, onDelete = { vm.deleteOdometer(log.id) })
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
            text = { NumberField(kmText, { kmText = it }, "KM odometer") },
            confirmButton = { TextButton(enabled = posInt(kmText) != null, onClick = { posInt(kmText)?.let { vm.updateOdometer(log.copy(km = it)) }; editTarget = null }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Batal") } })
    }
}

// ---------- Jadwal / kategori servis ----------

@Composable
fun ScheduleScreen(vm: MainViewModel, onOpenMaster: () -> Unit) {
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val masters by vm.masters.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var recordTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var editTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var deleteTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var tab by remember { mutableStateOf(0) }
    // Paling mendesak di atas
    val views = schedules.map { Engines.scheduleView(it, km) }.sortedByDescending { it.progress }
    val needService = views.filter { it.status == ServiceStatus.SOON || it.status == ServiceStatus.DUE || it.status == ServiceStatus.LATE }
    val good = views.filter { it.status == ServiceStatus.NORMAL || it.status == ServiceStatus.NO_ODOMETER }
    val shown = when (tab) { 1 -> needService; 2 -> good; else -> views }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        floatingActionButton = { if (vehicle != null) AddFab { showAdd = true } }
    ) { pad ->
        Column(Modifier.padding(pad).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Jadwal & Komponen", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    vehicle?.let { Text(it.name, fontSize = 18.sp, color = TextGray) }
                }
                IconButton(onClick = onOpenMaster) { Icon(Icons.Default.Category, "Master Kategori", tint = LavenderPrimary, modifier = Modifier.size(32.dp)) }
            }
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = LavenderPrimary) {
                listOf("Semua (${views.size})", "Perlu Servis (${needService.size})", "Baik (${good.size})").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, selectedContentColor = LavenderPrimary, unselectedContentColor = LavenderPrimary,
                        text = { Text(label, textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.Medium) })
                }
            }
            if (shown.isEmpty()) {
                Text(
                    when {
                        vehicle == null -> "Tambahkan kendaraan dulu di tab Kendaraan."
                        tab == 1 -> "Tidak ada komponen yang perlu servis."
                        tab == 2 -> "Belum ada komponen berstatus baik."
                        else -> "Belum ada kategori. Tap + untuk menambah."
                    }, color = TextGray, modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
                    items(shown, key = { it.schedule.id }) { view ->
                        ScheduleCard(view, onRecord = { recordTarget = view.schedule }, onEdit = { editTarget = view.schedule }, onDelete = { deleteTarget = view.schedule })
                    }
                }
            }
        }
    }

    if (showAdd) {
        val options = masters.filter { it.vehicleType == "ALL" || it.vehicleType == vehicle?.type }
        ScheduleFormDialog("Tambah Kategori", options, null, km, { showAdd = false }) { n, ik, im, lk, ld, ne, nh, nm, wk, wd -> vm.addSchedule(n, ik, im, lk, ld, ne, nh, nm, wk, wd); showAdd = false }
    }
    editTarget?.let { s ->
        ScheduleFormDialog("Edit Kategori", null, s, km, { editTarget = null }) { n, ik, im, lk, ld, ne, nh, nm, wk, wd ->
            vm.updateSchedule(s.copy(name = n, intervalKm = ik, intervalMonths = im, lastServiceKm = lk, lastServiceDate = ld, notifyEnabled = ne, notifyHour = nh, notifyMinute = nm, warnKm = wk, warnDays = wd)); editTarget = null
        }
    }
    recordTarget?.let { s ->
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, date, cost, notes, img -> vm.recordService(s, kmV, date, cost, notes, img); recordTarget = null }
    }
    deleteTarget?.let { s -> ConfirmDialog("Hapus kategori?", "\"${s.name}\" akan dihapus.", "Hapus", { vm.deleteSchedule(s.id); deleteTarget = null }, { deleteTarget = null }) }
}

/** Kartu jadwal: nama + interval, status, KM terakhir / berikutnya / kondisi, tombol catat servis. Edit & hapus di titik tiga. */
@Composable
private fun ScheduleCard(view: ScheduleView, onRecord: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val s = view.schedule
    val ik = s.intervalKm?.takeIf { it > 0 }
    val im = s.intervalMonths?.takeIf { it > 0 }
    val lk = s.lastServiceKm
    val ld = s.lastServiceDate
    val nextKm: Int? = if (ik != null && lk != null) lk + ik else null
    val nextDate: Long? = if (im != null && ld != null) localDateToMillis(msToLocalDate(ld).plusMonths(im.toLong())) else null
    val conditionColor = when (view.status) { ServiceStatus.NORMAL -> LavenderPrimary; else -> serviceColor(view.status).second }

    Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 6.dp)) {
                    Text(s.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    val interval = intervalLabel(ik, im)
                    if (interval.isNotEmpty()) Text(interval.replaceFirstChar { it.uppercase() }, fontSize = 15.sp, color = TextGray)
                }
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(view.status)
                    ItemMenu(onEdit, onDelete, editLabel = "Edit Jadwal", deleteLabel = "Hapus Jadwal")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(end = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Terakhir", fontSize = 14.sp, color = TextGray)
                    Text(lk?.let { "${fmtKm(it)} km" } ?: "-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    ld?.let { Text(fmtDate(it), fontSize = 14.sp, color = TextGray) }
                }
                Column(Modifier.weight(1f)) {
                    Text("Berikutnya", fontSize = 14.sp, color = TextGray)
                    Text(nextKm?.let { "${fmtKm(it)} km" } ?: nextDate?.let { fmtDate(it) } ?: "-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    if (nextKm != null && nextDate != null) Text(fmtDate(nextDate), fontSize = 14.sp, color = TextGray)
                }
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                    Text("Kondisi", fontSize = 14.sp, color = TextGray)
                    Text(conditionText(view), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = conditionColor, textAlign = TextAlign.End)
                }
            }
            // Info batas peringatan + notifikasi
            val warn = mutableListOf<String>()
            if (ik != null) warn += "${fmtKm(Engines.warnKm(s))} km"
            if (im != null) warn += "${Engines.warnDays(s)} hari"
            val infoParts = mutableListOf<String>()
            if (warn.isNotEmpty()) infoParts += "Segera saat sisa " + warn.joinToString(" / ")
            infoParts += if (s.notifyEnabled) "Notifikasi ${fmtTime(s.notifyHour, s.notifyMinute)}" else "Notifikasi mati"
            Spacer(Modifier.height(10.dp))
            Text(infoParts.joinToString(" · "), fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(end = 12.dp))
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onRecord, modifier = Modifier.fillMaxWidth().padding(end = 12.dp).height(56.dp), shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonSlate, contentColor = TextWhite)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Catat Servis Sekarang", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private const val CUSTOM = "__custom__"

/** Mode tambah: dropdown master (sesuai jenis kendaraan) + "Lainnya". Mode edit (masters == null): nama bebas. */
@Composable
private fun ScheduleFormDialog(
    title: String, masters: List<MasterCategory>?, initial: MaintenanceSchedule?, currentKm: Int?,
    onDismiss: () -> Unit,
    onSave: (name: String, intervalKm: Int?, intervalMonths: Int?, lastKm: Int?, lastDate: Long, notifyEnabled: Boolean, notifyHour: Int, notifyMinute: Int, warnKm: Int, warnDays: Int) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kmT by remember { mutableStateOf(initial?.intervalKm?.toString() ?: "") }
    var moT by remember { mutableStateOf(initial?.intervalMonths?.toString() ?: "") }
    var lastKm by remember { mutableStateOf((initial?.lastServiceKm ?: currentKm)?.toString() ?: "") }
    var lastDate by remember { mutableStateOf(initial?.lastServiceDate ?: todayMillis()) }
    var notifyOn by remember { mutableStateOf(initial?.notifyEnabled ?: true) }
    var notifyHour by remember { mutableStateOf(initial?.notifyHour ?: 9) }
    var notifyMinute by remember { mutableStateOf(initial?.notifyMinute ?: 0) }
    var warnKmT by remember { mutableStateOf(initial?.warnKm?.takeIf { it > 0 }?.toString() ?: "") }
    var warnDaysT by remember { mutableStateOf(initial?.warnDays?.takeIf { it > 0 }?.toString() ?: "") }
    var selected by remember { mutableStateOf<String?>(if (masters == null) CUSTOM else null) }
    var menu by remember { mutableStateOf(false) }
    val showName = selected == CUSTOM
    val canSave = name.isNotBlank() && (posInt(kmT) != null || posInt(moT) != null)

    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (masters != null) {
                    Box {
                        OutlinedButton(onClick = { menu = true }) { Text(when (selected) { null -> "Pilih komponen"; CUSTOM -> "Lainnya (ketik sendiri)"; else -> name }) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            masters.forEach { m ->
                                DropdownMenuItem(text = { Text(m.name) }, onClick = {
                                    selected = m.id; name = m.name
                                    notifyOn = m.notifyEnabled; notifyHour = m.notifyHour; notifyMinute = m.notifyMinute
                                    warnKmT = m.warnKm.takeIf { it > 0 }?.toString() ?: ""; warnDaysT = m.warnDays.takeIf { it > 0 }?.toString() ?: ""
                                    kmT = m.defaultKm.takeIf { it > 0 }?.toString() ?: ""; moT = m.defaultMonths.takeIf { it > 0 }?.toString() ?: ""
                                    menu = false
                                })
                            }
                            DropdownMenuItem(text = { Text("Lainnya (ketik sendiri)") }, onClick = { selected = CUSTOM; name = ""; kmT = ""; moT = ""; warnKmT = ""; warnDaysT = ""; menu = false })
                        }
                    }
                }
                if (showName) OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama komponen") }, singleLine = true)
                if (selected != null) {
                    NumberField(kmT, { kmT = it }, "Ganti tiap (km)", hint = "Kosongkan jika tidak dipakai")
                    NumberField(moT, { moT = it }, "Ganti tiap (bulan)", hint = "Kosongkan jika tidak dipakai")
                    NumberField(lastKm, { lastKm = it }, "KM servis terakhir")
                    DateField("Tanggal servis terakhir", lastDate) { lastDate = it }
                    posInt(kmT)?.let { NumberField(warnKmT, { warnKmT = it }, "Segera saat sisa (km)", hint = "Kosong = otomatis ${fmtKm((it * 0.2).toInt())} km (20%)") }
                    if (posInt(moT) != null) NumberField(warnDaysT, { warnDaysT = it }, "Segera saat sisa (hari)", hint = "Kosong = otomatis 30 hari")
                    if (canSave) NotifyOptions(notifyOn, notifyHour, notifyMinute) { e, h, m -> notifyOn = e; notifyHour = h; notifyMinute = m }
                }
            }
        },
        confirmButton = { TextButton(enabled = selected != null && canSave, onClick = { onSave(name.trim(), posInt(kmT), posInt(moT), lastKm.toIntOrNull(), lastDate, notifyOn, notifyHour, notifyMinute, if (posInt(kmT) != null) warnKmT.toIntOrNull() ?: 0 else 0, if (posInt(moT) != null) warnDaysT.toIntOrNull() ?: 0 else 0) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

/** Form tanggal, KM, biaya, catatan, foto. Dipakai untuk "Sudah service" dan edit riwayat. */
@Composable
fun ServiceFormDialog(
    title: String, initialKm: Int?, initialDate: Long, initialCost: Long, initialNotes: String?, initialImage: String? = null,
    onDismiss: () -> Unit, onConfirm: (km: Int, date: Long, cost: Long, notes: String?, imagePath: String?) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var kmText by remember { mutableStateOf(initialKm?.toString() ?: "") }
    var date by remember { mutableStateOf(initialDate) }
    var costText by remember { mutableStateOf(if (initialCost > 0) initialCost.toString() else "") }
    var notes by remember { mutableStateOf(initialNotes ?: "") }
    var imagePath by remember { mutableStateOf(initialImage) }
    var newImages by remember { mutableStateOf(listOf<String>()) }   // foto baru yang dibuat selama dialog terbuka
    var imageError by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val saved = withContext(Dispatchers.IO) { ImageStore.save(ctx, uri) }
            if (saved != null) { imagePath = saved; newImages = newImages + saved; imageError = false } else imageError = true
        }
    }
    // Bersihkan file foto baru yang tidak jadi dipakai (dibatalkan / diganti)
    fun cleanup(keep: String?) { newImages.filter { it != keep }.forEach { ImageStore.delete(it) } }
    fun dismiss() { cleanup(null); onDismiss() }

    AlertDialog(onDismissRequest = { dismiss() }, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Tanggal servis", date) { date = it }
                NumberField(kmText, { kmText = it }, "KM saat servis")
                NumberField(costText, { costText = it }, "Biaya servis", currency = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan (opsional)") })

                Text("Foto nota / bukti (opsional)", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (imagePath != null) {
                    StoredImage(imagePath, Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)), maxSide = 800)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch("image/*") }) { Text(if (imagePath == null) "Pilih Foto" else "Ganti Foto") }
                    if (imagePath != null) TextButton(onClick = { imagePath = null }) { Text("Hapus Foto", color = Color.Red) }
                }
                if (imageError) Text("Gagal membaca gambar.", color = Color.Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = posInt(kmText) != null, onClick = {
                posInt(kmText)?.let {
                    cleanup(imagePath)
                    onConfirm(it, date, costText.toLongOrNull() ?: 0L, notes.ifBlank { null }, imagePath)
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = { dismiss() }) { Text("Batal") } })
}

@Composable
fun RecordServiceDialog(schedule: MaintenanceSchedule?, initialKm: Int?, onDismiss: () -> Unit, onConfirm: (km: Int, date: Long, cost: Long, notes: String?, imagePath: String?) -> Unit) {
    ServiceFormDialog(if (schedule != null) "Sudah service: ${schedule.name}" else "Catat Servis", initialKm, todayMillis(), 0L, null, null, onDismiss, onConfirm)
}
