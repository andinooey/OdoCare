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
import androidx.compose.foundation.shape.CircleShape
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
    var showAddVehicle by remember { mutableStateOf(false) }
    var recordTarget by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    val scheduleViews = schedules.map { Engines.scheduleView(it, km) }
    val v = vehicle

    if (v == null) {
        WelcomeEmpty(onAdd = { showAddVehicle = true })
    } else {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = LavenderPrimary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("OdoCare", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onNavigateToDocuments) { Icon(Icons.Default.Description, "Dokumen", tint = TextWhite, modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.height(8.dp))

            // Pindah kendaraan aktif langsung dari Home (hanya tampil bila punya lebih dari satu)
            if (vehicles.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    vehicles.forEach { item -> FilterChip(selected = item.id == v.id, onClick = { vm.setActiveVehicle(item.id) }, label = { Text(item.name, fontSize = 12.sp) }) }
                }
                Spacer(Modifier.height(10.dp))
            }

            Surface(onClick = { showAddKm = true }, color = SurfaceVariantDark, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(StatusBlueBg), contentAlignment = Alignment.Center) {
                            Icon(VehicleTypes.icon(v.type), null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(vehicleTypeLabel(v.type), fontSize = 12.sp, color = TextGray)
                        }
                        if (v.plate.isNotBlank()) PlateView(v.plate)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OdometerDigits(km)
                        Spacer(Modifier.width(8.dp))
                        Text("km", color = TextGray, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("Ketuk untuk update", color = TextGray, fontSize = 11.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(Icons.Default.Speed, "Update KM", Modifier.weight(1f), false) { showAddKm = true }
                ActionButton(Icons.Default.Add, "Catat Servis", Modifier.weight(1f), true) { showPicker = true }
                ActionButton(Icons.Default.History, "Riwayat", Modifier.weight(1f), false) { onNavigateToHistory() }
            }
            Spacer(Modifier.height(18.dp))

            SectionHeader("Ringkasan Perawatan")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("${scheduleViews.count { it.status == ServiceStatus.LATE }}", "Lewat", Icons.Default.Error, StatusRedBg, StatusRedText, Modifier.weight(1f)) { vm.pendingScheduleTab = 1; onNavigateToSchedule() }
                SummaryCard("${scheduleViews.count { it.status == ServiceStatus.SOON || it.status == ServiceStatus.DUE }}", "Segera", Icons.Default.Warning, StatusYellowBg, StatusYellowText, Modifier.weight(1f)) { vm.pendingScheduleTab = 2; onNavigateToSchedule() }
                SummaryCard("${scheduleViews.count { it.status == ServiceStatus.NORMAL || it.status == ServiceStatus.NO_ODOMETER }}", "Aman", Icons.Default.CheckCircle, StatusGreenBg, StatusGreenText, Modifier.weight(1f)) { vm.pendingScheduleTab = 3; onNavigateToSchedule() }
            }
            Spacer(Modifier.height(18.dp))

            SectionHeader("Status Komponen", "Lihat semua", onNavigateToSchedule)
            Spacer(Modifier.height(6.dp))
            if (schedules.isEmpty()) {
                EmptyState(Icons.Default.Build, "Belum ada jadwal", "Tambahkan komponen servis di tab Servis.", compact = true, actionLabel = "Tambah", onAction = onNavigateToSchedule)
            }
            scheduleViews.sortedByDescending { it.progress }.take(5).forEach { view -> HomeComponentCard(view) }
            Spacer(Modifier.height(12.dp))

            SectionHeader("Dokumen & Pajak", "Kelola", onNavigateToDocuments)
            Spacer(Modifier.height(6.dp))
            if (documents.isEmpty()) {
                EmptyState(Icons.Default.Description, "Belum ada dokumen", "Pajak, SIM, atau asuransi tampil di sini.", compact = true, actionLabel = "Tambah", onAction = onNavigateToDocuments)
            } else {
                documents.take(4).forEach { d ->
                    val dv = Engines.documentView(d)
                    val colors = docColor(dv.status)
                    Surface(color = SurfaceVariantDark, shape = CardShape, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(CardShape).clickable(onClick = onNavigateToDocuments)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(docTypeLabel(d.type), color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Jatuh tempo ${fmtDate(d.expiryDate)}", color = TextGray, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                DocStatusChip(dv.status)
                                Spacer(Modifier.height(3.dp))
                                Text(docDaysText(dv.daysRemaining), color = colors.second, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddVehicle) VehicleFormDialog("Tambah Kendaraan", null, { showAddVehicle = false }) { n, p, t, k, a, d -> vm.addVehicle(n, p, t, k, a, d); showAddVehicle = false }
    if (showAddKm) AddKmDialog(km, { showAddKm = false }) { vm.addOdometer(it); showAddKm = false }
    if (showPicker) {
        AlertDialog(onDismissRequest = { showPicker = false }, shape = RoundedCornerShape(20.dp), containerColor = SurfaceDark,
            title = { Text("Servis apa yang dicatat?", fontSize = 16.sp, color = TextWhite) },
            text = {
                if (schedules.isEmpty()) EmptyState(Icons.Default.Build, "Belum ada jadwal", "Tambahkan komponen dulu di tab Servis.", compact = true)
                else Column(Modifier.verticalScroll(rememberScrollState())) {
                    schedules.forEach { s ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { showPicker = false; recordTarget = s }.padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(s.name, fontSize = 14.sp, color = TextWhite)
                        }
                    }
                }
            },
            confirmButton = { if (schedules.isEmpty()) TextButton(onClick = { showPicker = false; onNavigateToSchedule() }) { Text("Buka Jadwal") } },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal", color = TextGray) } })
    }
    recordTarget?.let { s ->
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, date, cost, notes, img ->
            vm.recordService(s, kmV, date, cost, notes, img); recordTarget = null
        }
    }
}

/** Kartu Status Komponen di Home, sengaja ringkas: nama + status, progress, lalu target berikutnya dan sisa. */
@Composable
private fun HomeComponentCard(view: ScheduleView) {
    val s = view.schedule
    val ik = s.intervalKm?.takeIf { it > 0 }
    val im = s.intervalMonths?.takeIf { it > 0 }
    val lk = s.lastServiceKm
    val ld = s.lastServiceDate
    val nextKm: Int? = if (ik != null && lk != null) lk + ik else null
    val nextDate: Long? = if (im != null && ld != null) localDateToMillis(msToLocalDate(ld).plusMonths(im.toLong())) else null
    val statusColor = serviceColor(view.status).second
    val next = nextKm?.let { "${fmtKm(it)} km" } ?: nextDate?.let { fmtDate(it) }

    Surface(color = SurfaceVariantDark, shape = CardShape, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, Modifier.weight(1f), color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                StatusChip(view.status)
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(view.progress, statusColor)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (next != null) "Berikutnya $next" else "", Modifier.weight(1f), color = TextGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(conditionText(view), color = if (view.status == ServiceStatus.NORMAL) LavenderPrimary else statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
            }
        }
    }
}

/** Tampilan awal saat belum ada kendaraan sama sekali. */
@Composable
private fun WelcomeEmpty(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        // Ilustrasi: lingkaran bertingkat + ikon melayang
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(210.dp).clip(CircleShape).background(SurfaceVariantDark.copy(alpha = 0.45f)))
            Box(Modifier.size(150.dp).clip(CircleShape).background(SurfaceVariantDark))
            Box(Modifier.size(100.dp).clip(CircleShape).background(PillSelected), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Speed, null, tint = LavenderPrimary, modifier = Modifier.size(48.dp))
            }
            FloatBadge(Icons.Default.DirectionsCar, Modifier.align(Alignment.TopStart).offset(x = 14.dp, y = 30.dp), StatusBlueBg, StatusBlueText)
            FloatBadge(Icons.Default.TwoWheeler, Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = 22.dp), StatusGreenBg, StatusGreenText)
            FloatBadge(Icons.Default.Build, Modifier.align(Alignment.BottomStart).offset(x = 18.dp, y = (-26).dp), StatusYellowBg, StatusYellowText)
            FloatBadge(Icons.Default.Description, Modifier.align(Alignment.BottomEnd).offset(x = (-14).dp, y = (-34).dp), StatusRedBg, StatusRedText)
        }
        Spacer(Modifier.height(20.dp))
        Text("Selamat datang di OdoCare", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Belum ada kendaraan. Tambahkan kendaraan pertamamu untuk mulai memantau servis dan pajak.", fontSize = 13.sp, color = TextGray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(46.dp)) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tambah Kendaraan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
        Surface(color = SurfaceVariantDark, shape = CardShape, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WelcomePoint(Icons.Default.Speed, "Pantau KM odometer", "Catat KM kapan saja, riwayatnya tersimpan.")
                WelcomePoint(Icons.Default.Build, "Jadwal servis otomatis", "Oli, rem, ban, dan lainnya dengan status Aman / Segera / Lewat.")
                WelcomePoint(Icons.Default.Notifications, "Pengingat pajak & dokumen", "Notifikasi sebelum STNK, SIM, atau asuransi jatuh tempo.")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FloatBadge(icon: ImageVector, modifier: Modifier, bg: Color, fg: Color) {
    Box(modifier.size(38.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun WelcomePoint(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(PillSelected), contentAlignment = Alignment.Center) { Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            Text(desc, fontSize = 11.sp, color = TextGray)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, isPrimary: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (isPrimary) LavenderPrimary else Color.Transparent, contentColor = if (isPrimary) Color(0xFF0D1B3E) else TextWhite),
        border = if (!isPrimary) BorderStroke(1.dp, Color(0xFF3A3B47)) else null, contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun SummaryCard(value: String, label: String, icon: ImageVector, bgColor: Color, textColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, color = bgColor, shape = CardShape, modifier = modifier) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, lineHeight = 20.sp)
                Text(label, fontSize = 11.sp, color = textColor)
            }
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Riwayat Odometer", onBack) },
        floatingActionButton = { AddFab { showAdd = true } }
    ) { pad ->
        if (logs.isEmpty()) {
            EmptyState(Icons.Default.Speed, "Belum ada catatan odometer", "Catat KM secara berkala agar status servis akurat.", Modifier.padding(pad).padding(top = 48.dp), actionLabel = "Catat KM", onAction = { showAdd = true })
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)) {
                items(logs, key = { it.id }) { log ->
                    Surface(color = SurfaceVariantDark, shape = CardShape) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) { Icon(Icons.Default.Speed, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${fmtKm(log.km)} km", fontSize = 14.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                                Text(fmtDate(log.recordedAt), fontSize = 11.sp, color = TextGray)
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
        FormDialog("Edit Odometer", Icons.Default.Speed, { editTarget = null },
            onConfirm = { posInt(kmText)?.let { vm.updateOdometer(log.copy(km = it)) }; editTarget = null }, confirmEnabled = posInt(kmText) != null
        ) { NumberField(kmText, { kmText = it }, "KM odometer", icon = Icons.Default.Speed) }
    }
}

// ---------- Jadwal / komponen servis ----------

private val filterLabels = listOf("Semua", "Lewat", "Segera", "Aman")

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
    // Tab awal bisa dipilih dari kotak ringkasan di Home (Lewat / Segera / Aman)
    var tab by remember { mutableStateOf(vm.pendingScheduleTab.also { vm.pendingScheduleTab = 0 }) }
    // Paling mendesak di atas
    val views = schedules.map { Engines.scheduleView(it, km) }.sortedByDescending { it.progress }
    val late = views.filter { it.status == ServiceStatus.LATE }
    val soon = views.filter { it.status == ServiceStatus.SOON || it.status == ServiceStatus.DUE }
    val safe = views.filter { it.status == ServiceStatus.NORMAL || it.status == ServiceStatus.NO_ODOMETER }
    val shown = when (tab) { 1 -> late; 2 -> soon; 3 -> safe; else -> views }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        floatingActionButton = { if (vehicle != null) AddFab { showAdd = true } }
    ) { pad ->
        Column(Modifier.padding(pad).statusBarsPadding()) {
            ScreenTitle("Jadwal & Komponen", vehicle?.name, Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 10.dp)) {
                IconButton(onClick = onOpenMaster) { Icon(Icons.Default.Category, "Master Kategori", tint = LavenderPrimary, modifier = Modifier.size(22.dp)) }
            }
            if (vehicle == null) {
                NoVehicleState(vm, Modifier.padding(top = 32.dp))
            } else {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    StatusFilterRow(
                        filterLabels, listOf(views.size, late.size, soon.size, safe.size),
                        listOf(PillSelected to TextWhite, StatusRedBg to StatusRedText, StatusYellowBg to StatusYellowText, StatusGreenBg to StatusGreenText),
                        tab
                    ) { tab = it }
                }
                if (shown.isEmpty()) {
                    when (tab) {
                        1 -> EmptyState(Icons.Default.CheckCircle, "Tidak ada yang lewat", "Semua komponen masih dalam batas servis.", Modifier.padding(top = 32.dp))
                        2 -> EmptyState(Icons.Default.CheckCircle, "Tidak ada yang segera", "Belum ada komponen yang mendekati jadwal servis.", Modifier.padding(top = 32.dp))
                        3 -> EmptyState(Icons.Default.Build, "Belum ada yang aman", "Komponen berstatus aman akan tampil di sini.", Modifier.padding(top = 32.dp))
                        else -> EmptyState(Icons.Default.Build, "Belum ada komponen", "Tambahkan oli, rem, ban, atau komponen lain yang ingin dipantau.", Modifier.padding(top = 32.dp), actionLabel = "Tambah Komponen", onAction = { showAdd = true })
                    }
                } else {
                    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp)) {
                        items(shown, key = { it.schedule.id }) { view ->
                            ScheduleCard(view, onRecord = { recordTarget = view.schedule }, onEdit = { editTarget = view.schedule }, onDelete = { deleteTarget = view.schedule })
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        val options = masters.filter { it.vehicleType == "ALL" || it.vehicleType == vehicle?.type }
        ScheduleFormDialog("Tambah Komponen", options, null, km, { showAdd = false }) { n, ik, im, lk, ld, ne, nh, nm, wk, wd -> vm.addSchedule(n, ik, im, lk, ld, ne, nh, nm, wk, wd); showAdd = false }
    }
    editTarget?.let { s ->
        ScheduleFormDialog("Edit Komponen", null, s, km, { editTarget = null }) { n, ik, im, lk, ld, ne, nh, nm, wk, wd ->
            vm.updateSchedule(s.copy(name = n, intervalKm = ik, intervalMonths = im, lastServiceKm = lk, lastServiceDate = ld, notifyEnabled = ne, notifyHour = nh, notifyMinute = nm, warnKm = wk, warnDays = wd)); editTarget = null
        }
    }
    recordTarget?.let { s ->
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, date, cost, notes, img -> vm.recordService(s, kmV, date, cost, notes, img); recordTarget = null }
    }
    deleteTarget?.let { s -> ConfirmDialog("Hapus komponen?", "\"${s.name}\" akan dihapus.", "Hapus", { vm.deleteSchedule(s.id); deleteTarget = null }, { deleteTarget = null }) }
}

/** Baris kondisi per bagian: "Sisa 4,000 km" dan "Sisa 119 hari" ditumpuk agar muat. */
private fun conditionLines(v: ScheduleView): List<String> {
    if (v.status == ServiceStatus.NO_ODOMETER) return listOf("KM belum diupdate")
    val lines = mutableListOf<String>()
    v.kmRemaining?.let { lines += if (it < 0) "Lewat ${fmtKm(-it)} km" else "Sisa ${fmtKm(it)} km" }
    v.daysRemaining?.let { lines += if (it < 0) "Lewat ${-it} hari" else "Sisa $it hari" }
    return if (lines.isEmpty()) listOf("-") else lines
}

@Composable
private fun InfoCol(label: String, value: String, sub: String?, modifier: Modifier, color: Color = TextWhite, end: Boolean = false) {
    Column(modifier, horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Text(label, fontSize = 10.sp, color = TextGray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, textAlign = if (end) TextAlign.End else TextAlign.Start)
        if (sub != null) Text(sub, fontSize = 10.sp, color = TextGray, textAlign = if (end) TextAlign.End else TextAlign.Start)
    }
}

/** Kartu jadwal ringkas: nama + status, progress, terakhir / berikutnya / kondisi, tombol catat. Edit & hapus di titik tiga. */
@Composable
private fun ScheduleCard(view: ScheduleView, onRecord: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val s = view.schedule
    val ik = s.intervalKm?.takeIf { it > 0 }
    val im = s.intervalMonths?.takeIf { it > 0 }
    val lk = s.lastServiceKm
    val ld = s.lastServiceDate
    val nextKm: Int? = if (ik != null && lk != null) lk + ik else null
    val nextDate: Long? = if (im != null && ld != null) localDateToMillis(msToLocalDate(ld).plusMonths(im.toLong())) else null
    val statusColor = serviceColor(view.status).second
    val conditionColor = if (view.status == ServiceStatus.NORMAL) LavenderPrimary else statusColor
    val lines = conditionLines(view)

    Surface(color = SurfaceVariantDark, shape = CardShape, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, top = 8.dp, end = 4.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val interval = intervalLabel(ik, im)
                        if (interval.isNotEmpty()) Text(interval.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = TextGray)
                        Spacer(Modifier.width(6.dp))
                        Icon(if (s.notifyEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, null, tint = TextGray.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                    }
                }
                StatusChip(view.status)
                ItemMenu(onEdit, onDelete, editLabel = "Edit Jadwal", deleteLabel = "Hapus Jadwal")
            }
            Column(Modifier.padding(end = 10.dp)) {
                Spacer(Modifier.height(6.dp))
                ProgressBar(view.progress, statusColor)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    InfoCol("Terakhir", lk?.let { "${fmtKm(it)} km" } ?: "-", ld?.let { fmtDate(it) }, Modifier.weight(1f))
                    InfoCol("Berikutnya", nextKm?.let { "${fmtKm(it)} km" } ?: nextDate?.let { fmtDate(it) } ?: "-", if (nextKm != null && nextDate != null) fmtDate(nextDate) else null, Modifier.weight(1f))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Kondisi", fontSize = 10.sp, color = TextGray)
                        lines.forEach { Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = conditionColor, textAlign = TextAlign.End) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onRecord, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = ButtonSlate, contentColor = TextWhite)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Catat servis", fontSize = 13.sp)
                }
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
    val showName = selected == CUSTOM
    val canSave = name.isNotBlank() && (posInt(kmT) != null || posInt(moT) != null)

    FormDialog(
        title = title, icon = Icons.Default.Build, onDismiss = onDismiss, confirmEnabled = selected != null && canSave,
        onConfirm = { onSave(name.trim(), posInt(kmT), posInt(moT), lastKm.toIntOrNull(), lastDate, notifyOn, notifyHour, notifyMinute, if (posInt(kmT) != null) warnKmT.toIntOrNull() ?: 0 else 0, if (posInt(moT) != null) warnDaysT.toIntOrNull() ?: 0 else 0) }
    ) {
        if (masters != null) {
            DropdownField(
                "Komponen", when (selected) { null -> null; CUSTOM -> "Lainnya (ketik sendiri)"; else -> name }, Icons.Default.Build,
                masters.map { it.id to it.name } + (CUSTOM to "Lainnya (ketik sendiri)"), placeholder = "Pilih komponen"
            ) { key ->
                if (key == CUSTOM) { selected = CUSTOM; name = ""; kmT = ""; moT = ""; warnKmT = ""; warnDaysT = "" }
                else masters.firstOrNull { it.id == key }?.let { m ->
                    selected = m.id; name = m.name
                    notifyOn = m.notifyEnabled; notifyHour = m.notifyHour; notifyMinute = m.notifyMinute
                    warnKmT = m.warnKm.takeIf { it > 0 }?.toString() ?: ""; warnDaysT = m.warnDays.takeIf { it > 0 }?.toString() ?: ""
                    kmT = m.defaultKm.takeIf { it > 0 }?.toString() ?: ""; moT = m.defaultMonths.takeIf { it > 0 }?.toString() ?: ""
                }
            }
        }
        if (showName) AppTextField(name, { name = it }, "Nama komponen", Icons.Default.Edit)
        if (selected != null) {
            FormGroupLabel("Interval ganti")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { NumberField(kmT, { kmT = it }, "Tiap (km)", icon = Icons.Default.Speed) }
                Box(Modifier.weight(1f)) { NumberField(moT, { moT = it }, "Tiap (bulan)", icon = Icons.Default.Event) }
            }
            FormNote("Isi salah satu atau keduanya. Kosongkan yang tidak dipakai.")
            FormGroupLabel("Servis terakhir")
            NumberField(lastKm, { lastKm = it }, "KM servis terakhir", icon = Icons.Default.Speed)
            DateField("Tanggal servis terakhir", lastDate) { lastDate = it }
            if (posInt(kmT) != null || posInt(moT) != null) {
                FormGroupLabel("Peringatan \"Segera\"")
                posInt(kmT)?.let { NumberField(warnKmT, { warnKmT = it }, "Segera saat sisa (km)", icon = Icons.Default.Warning, hint = "Kosong = otomatis ${fmtKm((it * 0.2).toInt())} km (20%)") }
                if (posInt(moT) != null) NumberField(warnDaysT, { warnDaysT = it }, "Segera saat sisa (hari)", icon = Icons.Default.Warning, hint = "Kosong = otomatis 30 hari")
            }
            if (canSave) NotifyOptions(notifyOn, notifyHour, notifyMinute) { e, h, m -> notifyOn = e; notifyHour = h; notifyMinute = m }
        }
    }
}

/** Form tanggal, KM, biaya, catatan, foto. Dipakai untuk "Sudah service" dan edit riwayat. */
@Composable
fun ServiceFormDialog(
    title: String, initialKm: Int?, initialDate: Long, initialCost: Long, initialNotes: String?, initialImage: String? = null,
    onDismiss: () -> Unit, onConfirm: (km: Int, date: Long, cost: Long, notes: String?, imagePath: String?) -> Unit,
    subtitle: String? = null
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

    FormDialog(
        title = title, subtitle = subtitle, icon = Icons.Default.Build, onDismiss = { dismiss() }, confirmEnabled = posInt(kmText) != null,
        onConfirm = {
            posInt(kmText)?.let {
                cleanup(imagePath)
                onConfirm(it, date, costText.toLongOrNull() ?: 0L, notes.ifBlank { null }, imagePath)
            }
        }
    ) {
        DateField("Tanggal servis", date) { date = it }
        NumberField(kmText, { kmText = it }, "KM saat servis", icon = Icons.Default.Speed)
        NumberField(costText, { costText = it }, "Biaya servis", currency = true, icon = Icons.Default.Payments)
        AppTextField(notes, { notes = it }, "Catatan (opsional)", Icons.Default.EditNote, singleLine = false, minLines = 2)
        if (imagePath == null) {
            PickerField("Foto nota / bukti (opsional)", null, Icons.Default.AddAPhoto, placeholder = "Tambah foto") { picker.launch("image/*") }
        } else {
            StoredImage(imagePath, Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)), maxSide = 800)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { picker.launch("image/*") }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(34.dp), border = BorderStroke(1.dp, FieldBorder)) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text("Ganti", fontSize = 12.sp)
                }
                TextButton(onClick = { imagePath = null }, modifier = Modifier.height(34.dp)) { Text("Hapus foto", color = DangerText, fontSize = 12.sp) }
            }
        }
        if (imageError) FormNote("Gagal membaca gambar.", DangerText)
    }
}

@Composable
fun RecordServiceDialog(schedule: MaintenanceSchedule?, initialKm: Int?, onDismiss: () -> Unit, onConfirm: (km: Int, date: Long, cost: Long, notes: String?, imagePath: String?) -> Unit) {
    ServiceFormDialog("Catat Servis", initialKm, todayMillis(), 0L, null, null, onDismiss, onConfirm, subtitle = schedule?.name)
}
