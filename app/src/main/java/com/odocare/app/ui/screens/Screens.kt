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
import com.odocare.app.data.FuelLog
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
    onNavigateToDocuments: () -> Unit, onNavigateToOdometer: () -> Unit,
    onNavigateToNotifications: () -> Unit, onNavigateToSettings: () -> Unit
) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val documents by vm.documents.collectAsStateWithLifecycle()
    val unreadNotifications by vm.unreadNotifications.collectAsStateWithLifecycle()
    val lastBackup by vm.lastBackup.collectAsStateWithLifecycle()
    var showAddKm by remember { mutableStateOf(false) }
    var showAddFuel by remember { mutableStateOf(false) }
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
                IconButton(onClick = onNavigateToNotifications) {
                    BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text(if (unreadNotifications > 99) "99+" else "$unreadNotifications", fontSize = 9.sp) } }) {
                        Icon(if (unreadNotifications > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications, "Notifikasi", tint = TextWhite, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Pindah kendaraan aktif langsung dari Home (hanya tampil bila punya lebih dari satu)
            if (vehicles.size > 1) {
                VehicleChips(vm)
                Spacer(Modifier.height(12.dp))
            }

            // Pengingat backup: belum pernah backup (setelah 7 hari dipakai) atau backup terakhir lebih dari 30 hari. "Nanti" menyembunyikannya 7 hari.
            val nowMs = System.currentTimeMillis()
            val dayMs = 86_400_000L
            val firstUse = vehicles.minOfOrNull { it.createdAt } ?: nowMs
            val backupDue = if (lastBackup == 0L) nowMs - firstUse >= 7 * dayMs else nowMs - lastBackup >= 30 * dayMs
            if (backupDue && nowMs >= vm.backupSnoozeUntil) {
                Surface(color = StatusYellowBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, null, tint = StatusYellowText, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Amankan data kamu", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusYellowText)
                            Text(if (lastBackup == 0L) "Belum pernah backup." else "Backup terakhir ${((nowMs - lastBackup) / dayMs)} hari lalu.", fontSize = 12.sp, color = StatusYellowText)
                        }
                        TextButton(onClick = { vm.expandBackupSection = true; onNavigateToSettings() }) { Text("Backup", color = StatusYellowText, fontWeight = FontWeight.Bold) }
                        IconButton(onClick = { vm.snoozeBackupReminder() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Ingatkan lagi nanti", tint = StatusYellowText, modifier = Modifier.size(18.dp)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Surface(onClick = { showAddKm = true }, color = SurfaceVariantDark, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(StatusBlueBg), contentAlignment = Alignment.Center) {
                            Icon(VehicleTypes.icon(v.type), null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
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
                        Text("Ketuk untuk update", color = TextGray, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionButton(Icons.Default.Speed, "Update KM", Modifier.weight(1f), false) { showAddKm = true }
                ActionButton(Icons.Default.LocalGasStation, "Catat BBM", Modifier.weight(1f), false) { showAddFuel = true }
                ActionButton(Icons.Default.Add, "Catat Servis", Modifier.weight(1f), true) { showPicker = true }
                ActionButton(Icons.Default.History, "Histori", Modifier.weight(1f), false) { onNavigateToHistory() }
            }
            Spacer(Modifier.height(16.dp))

            SectionHeader("Ringkasan Perawatan")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("${scheduleViews.count { it.status == ServiceStatus.LATE }}", "Lewat", Icons.Default.Error, StatusRedBg, StatusRedText, Modifier.weight(1f)) { vm.pendingScheduleTab = 1; onNavigateToSchedule() }
                SummaryCard("${scheduleViews.count { it.status == ServiceStatus.SOON || it.status == ServiceStatus.DUE }}", "Segera", Icons.Default.Warning, StatusYellowBg, StatusYellowText, Modifier.weight(1f)) { vm.pendingScheduleTab = 2; onNavigateToSchedule() }
                SummaryCard("${scheduleViews.count { it.status == ServiceStatus.NORMAL || it.status == ServiceStatus.NO_ODOMETER }}", "Aman", Icons.Default.CheckCircle, StatusGreenBg, StatusGreenText, Modifier.weight(1f)) { vm.pendingScheduleTab = 3; onNavigateToSchedule() }
            }
            Spacer(Modifier.height(16.dp))

            SectionHeader("Status Komponen", "Lihat semua", onNavigateToSchedule)
            Spacer(Modifier.height(8.dp))
            if (schedules.isEmpty()) {
                EmptyState(Icons.Default.Build, "Belum ada jadwal", "Tambahkan komponen servis di tab Servis.", compact = true, actionLabel = "Tambah", onAction = onNavigateToSchedule)
            }
            scheduleViews.sortedByDescending { it.progress }.take(5).forEach { view -> HomeComponentCard(view) }
            Spacer(Modifier.height(12.dp))

            SectionHeader("Dokumen & Pajak", "Kelola", onNavigateToDocuments)
            Spacer(Modifier.height(8.dp))
            if (documents.isEmpty()) {
                EmptyState(Icons.Default.Description, "Belum ada dokumen", "Pajak, SIM, atau asuransi tampil di sini.", compact = true, actionLabel = "Tambah", onAction = onNavigateToDocuments)
            } else {
                documents.take(4).forEach { d ->
                    val dv = Engines.documentView(d)
                    val colors = docColor(dv.status)
                    Surface(color = SurfaceVariantDark, shape = CardShape, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(CardShape).clickable(onClick = onNavigateToDocuments)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(docTypeLabel(d.type), color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Jatuh tempo ${fmtDate(d.expiryDate)}", color = TextGray, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                DocStatusChip(dv.status)
                                Spacer(Modifier.height(4.dp))
                                Text(docDaysText(dv.daysRemaining), color = colors.second, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddVehicle) VehicleFormDialog("Tambah Kendaraan", null, { showAddVehicle = false }) { n, p, t, k, a, d, f -> vm.addVehicle(n, p, t, k, a, d, f); showAddVehicle = false }
    if (showAddKm) AddKmDialog(km, { showAddKm = false }) { vm.addOdometer(it); showAddKm = false }
    if (showAddFuel) FuelFormDialog("Catat BBM", null, km, vehicle?.name, vehicle?.fuelPricePerLiter ?: 0L, { showAddFuel = false }) { date, kmV, liters, total, full -> vm.addFuel(date, kmV, liters, total, full); showAddFuel = false }
    if (showPicker) {
        AlertDialog(onDismissRequest = { showPicker = false }, shape = RoundedCornerShape(20.dp), containerColor = SurfaceDark,
            title = { Text("Servis apa yang dicatat?", fontSize = 16.sp, color = TextWhite) },
            text = {
                if (schedules.isEmpty()) EmptyState(Icons.Default.Build, "Belum ada jadwal", "Tambahkan komponen dulu di tab Servis.", compact = true)
                else Column(Modifier.verticalScroll(rememberScrollState())) {
                    schedules.forEach { s ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { showPicker = false; recordTarget = s }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(s.name, fontSize = 14.sp, color = TextWhite)
                        }
                    }
                }
            },
            confirmButton = { if (schedules.isEmpty()) TextButton(onClick = { showPicker = false; onNavigateToSchedule() }) { Text("Buka Jadwal") } },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal", color = TextGray) } })
    }
    recordTarget?.let { s ->
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, date, cost, parts, labor, notes, img ->
            vm.recordService(s, kmV, date, cost, parts, labor, notes, img); recordTarget = null
        }
    }
}

/**
 * Kartu Status Komponen di Home: nama + status, progress, lalu tiga kolom Terakhir (KM + tanggal servis),
 * Berikutnya (target), dan Kondisi (sisa). Susunannya sama dengan kartu di tab Servis.
 */
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
    val conditionColor = if (view.status == ServiceStatus.NORMAL) LavenderPrimary else statusColor

    Surface(color = SurfaceVariantDark, shape = CardShape, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, Modifier.weight(1f), color = TextWhite, style = AppType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                StatusChip(view.status)
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(view.progress, statusColor)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                InfoCol("Terakhir", lk?.let { "${fmtKm(it)} km" } ?: "-", ld?.let { fmtDate(it) }, Modifier.weight(1f))
                InfoCol("Berikutnya", nextKm?.let { "${fmtKm(it)} km" } ?: nextDate?.let { fmtDate(it) } ?: "-", if (nextKm != null && nextDate != null) fmtDate(nextDate) else null, Modifier.weight(1f))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Kondisi", style = AppType.Caption, color = TextGray)
                    conditionLines(view).forEach { Text(it, style = AppType.Value, color = conditionColor, textAlign = TextAlign.End) }
                }
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
        Spacer(Modifier.height(24.dp))
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
        Spacer(Modifier.height(24.dp))
        Text("Selamat datang di OdoCare", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Belum ada kendaraan. Tambahkan kendaraan pertamamu untuk mulai memantau servis dan pajak.", fontSize = 13.sp, color = TextGray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(46.dp)) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tambah Kendaraan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
        Surface(color = SurfaceVariantDark, shape = CardShape, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            Text(desc, fontSize = 12.sp, color = TextGray)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, isPrimary: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (isPrimary) LavenderPrimary else Color.Transparent, contentColor = if (isPrimary) Color(0xFF0D1B3E) else TextWhite),
        border = if (!isPrimary) BorderStroke(1.dp, Color(0xFF3A3B47)) else null, contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun SummaryCard(value: String, label: String, icon: ImageVector, bgColor: Color, textColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, color = bgColor, shape = CardShape, modifier = modifier) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, lineHeight = 20.sp)
                Text(label, fontSize = 12.sp, color = textColor)
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
            EmptyState(Icons.Default.Speed, "Belum ada catatan odometer", "Catat KM secara berkala agar status servis akurat.", Modifier.padding(pad).padding(top = 24.dp), actionLabel = "Catat KM", onAction = { showAdd = true })
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)) {
                items(logs, key = { it.id }) { log ->
                    Surface(color = SurfaceVariantDark, shape = CardShape) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) { Icon(Icons.Default.Speed, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${fmtKm(log.km)} km", fontSize = 14.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                                Text(fmtDate(log.recordedAt), fontSize = 12.sp, color = TextGray)
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
            ServiceSectionTitle(vm, vehicle?.name, Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 12.dp)) {
                IconButton(onClick = onOpenMaster) { Icon(Icons.Default.Category, "Master Kategori", tint = LavenderPrimary, modifier = Modifier.size(22.dp)) }
            }
            if (vehicle == null) {
                NoVehicleState(vm, Modifier.padding(top = 24.dp))
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
                        1 -> EmptyState(Icons.Default.CheckCircle, "Tidak ada yang lewat", "Semua komponen masih dalam batas servis.", Modifier.padding(top = 24.dp))
                        2 -> EmptyState(Icons.Default.CheckCircle, "Tidak ada yang segera", "Belum ada komponen yang mendekati jadwal servis.", Modifier.padding(top = 24.dp))
                        3 -> EmptyState(Icons.Default.Build, "Belum ada yang aman", "Komponen berstatus aman akan tampil di sini.", Modifier.padding(top = 24.dp))
                        else -> EmptyState(Icons.Default.Build, "Belum ada komponen", "Tambahkan oli, rem, ban, atau komponen lain yang ingin dipantau.", Modifier.padding(top = 24.dp), actionLabel = "Tambah Komponen", onAction = { showAdd = true })
                    }
                } else {
                    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)) {
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
        RecordServiceDialog(schedule = s, initialKm = km, onDismiss = { recordTarget = null }) { kmV, date, cost, parts, labor, notes, img -> vm.recordService(s, kmV, date, cost, parts, labor, notes, img); recordTarget = null }
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
        Text(label, style = AppType.Caption, color = TextGray)
        Text(value, style = AppType.Value, color = color, textAlign = if (end) TextAlign.End else TextAlign.Start)
        if (sub != null) Text(sub, style = AppType.Caption, color = TextGray, textAlign = if (end) TextAlign.End else TextAlign.Start)
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
        Column(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = AppType.CardTitle, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val interval = intervalLabel(ik, im)
                        if (interval.isNotEmpty()) Text(interval.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = TextGray)
                        Spacer(Modifier.width(8.dp))
                        Icon(if (s.notifyEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, null, tint = TextGray.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                    }
                }
                StatusChip(view.status)
                ItemMenu(onEdit, onDelete, editLabel = "Edit Jadwal", deleteLabel = "Hapus Jadwal")
            }
            Column(Modifier.padding(end = 12.dp)) {
                Spacer(Modifier.height(8.dp))
                ProgressBar(view.progress, statusColor)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    InfoCol("Terakhir", lk?.let { "${fmtKm(it)} km" } ?: "-", ld?.let { fmtDate(it) }, Modifier.weight(1f))
                    InfoCol("Berikutnya", nextKm?.let { "${fmtKm(it)} km" } ?: nextDate?.let { fmtDate(it) } ?: "-", if (nextKm != null && nextDate != null) fmtDate(nextDate) else null, Modifier.weight(1f))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Kondisi", style = AppType.Caption, color = TextGray)
                        lines.forEach { Text(it, style = AppType.Value, color = conditionColor, textAlign = TextAlign.End) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onRecord, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = ButtonSlate, contentColor = TextWhite)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
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

/** Form tanggal, KM, biaya (bisa dirinci), catatan, foto. Dipakai untuk "Sudah service" dan edit riwayat. */
@Composable
fun ServiceFormDialog(
    title: String, initialKm: Int?, initialDate: Long, initialCost: Long, initialNotes: String?, initialImage: String? = null,
    onDismiss: () -> Unit, onConfirm: (km: Int, date: Long, cost: Long, partsCost: Long, laborCost: Long, notes: String?, imagePath: String?) -> Unit,
    subtitle: String? = null,
    /** Batas bawah KM (mis. KM odometer saat ini). null = tanpa batas, dipakai saat edit riwayat lama. */
    minKm: Int? = null,
    initialParts: Long = 0L, initialLabor: Long = 0L
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var kmText by remember { mutableStateOf(initialKm?.toString() ?: "") }
    var date by remember { mutableStateOf(initialDate) }
    var costText by remember { mutableStateOf(if (initialCost > 0) initialCost.toString() else "") }
    // Rincian biaya: tersembunyi sampai "Rincikan" diketuk (otomatis terbuka bila riwayat ini sudah punya rincian)
    var breakdown by remember { mutableStateOf(initialParts > 0 || initialLabor > 0) }
    var partsText by remember { mutableStateOf(if (initialParts > 0) initialParts.toString() else "") }
    var laborText by remember { mutableStateOf(if (initialLabor > 0) initialLabor.toString() else "") }
    var notes by remember { mutableStateOf(initialNotes ?: "") }
    var imagePath by remember { mutableStateOf(initialImage) }
    var newImages by remember { mutableStateOf(listOf<String>()) }   // foto baru yang dibuat selama form terbuka
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

    val partsValue = partsText.toLongOrNull() ?: 0L
    val laborValue = laborText.toLongOrNull() ?: 0L
    val totalText = if (breakdown) (partsValue + laborValue).takeIf { it > 0 }?.toString() ?: "" else costText

    val kmValue = posInt(kmText)
    val kmTooLow = minKm != null && kmValue != null && kmValue < minKm
    FormDialog(
        title = title, subtitle = subtitle, icon = Icons.Default.Build, onDismiss = { dismiss() }, confirmEnabled = kmValue != null && !kmTooLow,
        onConfirm = {
            posInt(kmText)?.takeIf { minKm == null || it >= minKm }?.let {
                cleanup(imagePath)
                val parts = if (breakdown) partsValue else 0L
                val labor = if (breakdown) laborValue else 0L
                val cost = if (breakdown) parts + labor else costText.toLongOrNull() ?: 0L
                onConfirm(it, date, cost, parts, labor, notes.ifBlank { null }, imagePath)
            }
        }
    ) {
        DateField("Tanggal servis", date) { date = it }
        NumberField(
            kmText, { kmText = it }, "KM saat servis", icon = Icons.Default.Speed,
            hint = minKm?.let { "Minimal ${fmtKm(it)} km (KM odometer saat ini)" },
            error = if (kmTooLow) "KM servis tidak boleh lebih kecil dari KM saat ini (${fmtKm(minKm!!)} km)" else null
        )
        // Total biaya. Saat dirinci, kolom ini terisi otomatis dari suku cadang + jasa dan tidak bisa diketik.
        NumberField(
            totalText, { costText = it }, if (breakdown) "Total biaya (otomatis)" else "Biaya servis", currency = true, icon = Icons.Default.Payments,
            readOnly = breakdown
        )
        if (!breakdown) {
            TextButton(
                onClick = { breakdown = true }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Tune, null, modifier = Modifier.size(15.dp), tint = LavenderPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Rincikan", fontSize = 12.sp, color = LavenderPrimary)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { NumberField(partsText, { partsText = it }, "Suku cadang", currency = true, icon = Icons.Default.Settings) }
                Box(Modifier.weight(1f)) { NumberField(laborText, { laborText = it }, "Jasa", currency = true, icon = Icons.Default.Handyman) }
            }
            TextButton(
                onClick = {
                    // Tutup rincian: total yang sudah terhitung dipertahankan sebagai biaya servis
                    if (partsValue + laborValue > 0) costText = (partsValue + laborValue).toString()
                    breakdown = false; partsText = ""; laborText = ""
                }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp), tint = TextGray)
                Spacer(Modifier.width(8.dp))
                Text("Tutup rincian", fontSize = 12.sp, color = TextGray)
            }
        }
        AppTextField(notes, { notes = it }, "Catatan (opsional)", Icons.Default.EditNote, singleLine = false, minLines = 2)
        if (imagePath == null) {
            PickerField("Foto nota / bukti (opsional)", null, Icons.Default.AddAPhoto, placeholder = "Tambah foto") { picker.launch("image/*") }
        } else {
            StoredImage(imagePath, Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)), maxSide = 800)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { picker.launch("image/*") }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(34.dp), border = BorderStroke(1.dp, FieldBorder)) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(8.dp)); Text("Ganti", fontSize = 12.sp)
                }
                TextButton(onClick = { imagePath = null }, modifier = Modifier.height(34.dp)) { Text("Hapus foto", color = DangerText, fontSize = 12.sp) }
            }
        }
        if (imageError) FormNote("Gagal membaca gambar.", DangerText)
    }
}

@Composable
fun RecordServiceDialog(schedule: MaintenanceSchedule?, initialKm: Int?, onDismiss: () -> Unit, onConfirm: (km: Int, date: Long, cost: Long, partsCost: Long, laborCost: Long, notes: String?, imagePath: String?) -> Unit) {
    ServiceFormDialog("Catat Servis", initialKm, todayMillis(), 0L, null, null, onDismiss, onConfirm, subtitle = schedule?.name, minKm = initialKm)
}

/**
 * Form catat / edit BBM: tanggal, KM, total harga, centang tangki penuh.
 * Bila harga BBM per liter kendaraan sudah diatur ([pricePerLiter] > 0), liter dihitung otomatis dari total harga,
 * jadi cukup mengisi total harga. Bila belum diatur, jumlah liter diisi manual seperti biasa.
 * KM tidak boleh lebih kecil dari KM odometer saat ini ([minKm]); saat edit catatan lama, [minKm] = null.
 */
@Composable
fun FuelFormDialog(
    title: String, initial: FuelLog?, currentKm: Int?, vehicleName: String?, pricePerLiter: Long, onDismiss: () -> Unit,
    onSave: (date: Long, km: Int, liters: Double, totalPrice: Long, fullTank: Boolean) -> Unit
) {
    val minKm = if (initial == null) currentKm else null
    var date by remember { mutableStateOf(initial?.date ?: todayMillis()) }
    var kmText by remember { mutableStateOf((initial?.km ?: currentKm)?.toString() ?: "") }
    var litersText by remember { mutableStateOf(initial?.let { literInput(it.liters) } ?: "") }
    var totalText by remember { mutableStateOf(initial?.totalPrice?.toString() ?: "") }
    var fullTank by remember { mutableStateOf(initial?.fullTank ?: true) }

    val kmValue = posInt(kmText)
    val kmTooLow = minKm != null && kmValue != null && kmValue < minKm
    val total = totalText.toLongOrNull()?.takeIf { it > 0 }
    val auto = pricePerLiter > 0
    // Mode otomatis: liter = total / harga per liter (2 desimal). Saat edit dan total tidak diubah, liter lama dipertahankan.
    val liters: Double? = if (auto) {
        when {
            total == null -> null
            initial != null && total == initial.totalPrice -> initial.liters
            else -> Math.round(total.toDouble() / pricePerLiter * 100.0) / 100.0
        }.takeIf { it != null && it > 0 }
    } else posDouble(litersText)
    val perLiter = if (liters != null && total != null) Math.round(total / liters) else null

    FormDialog(
        title = title, subtitle = vehicleName, icon = Icons.Default.LocalGasStation, onDismiss = onDismiss,
        confirmEnabled = kmValue != null && !kmTooLow && liters != null && total != null,
        onConfirm = { if (kmValue != null && !kmTooLow && liters != null && total != null) onSave(date, kmValue, liters, total, fullTank) }
    ) {
        DateField("Tanggal isi", date) { date = it }
        NumberField(
            kmText, { kmText = it }, "KM saat isi", icon = Icons.Default.Speed,
            hint = minKm?.let { "Minimal ${fmtKm(it)} km (KM odometer saat ini). Odometer ikut diperbarui." },
            error = if (kmTooLow) "KM tidak boleh lebih kecil dari KM saat ini (${fmtKm(minKm!!)} km)" else null
        )
        if (!auto) DecimalField(litersText, { litersText = it }, "Jumlah bensin", suffix = "liter", icon = Icons.Default.LocalGasStation)
        NumberField(totalText, { totalText = it }, "Total harga", currency = true, icon = Icons.Default.Payments)
        if (auto) {
            FormNote(
                if (liters != null) "Jumlah bensin: ${fmtLiter(liters)} liter (harga ${rupiah(pricePerLiter)} per liter)"
                else "Jumlah bensin dihitung otomatis dari harga ${rupiah(pricePerLiter)} per liter."
            )
        } else {
            if (perLiter != null) FormNote("Harga per liter: ${rupiah(perLiter)}")
            FormNote("Atur harga BBM per liter di tab Kendaraan (menu Ubah) agar jumlah liter dihitung otomatis.")
        }
        CheckRow(Icons.Default.BatteryChargingFull, "Tangki penuh", "Centang bila diisi sampai penuh. Dipakai untuk menghitung km per liter.", fullTank) { fullTank = it }
    }
}
