@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.Snapshots
import com.odocare.app.data.Vehicle
import com.odocare.app.domain.IntervalUnit
import com.odocare.app.notif.Notif
import com.odocare.app.ui.*
import com.odocare.app.util.Exports
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(vm: MainViewModel, onOpenMaster: () -> Unit, onOpenVehicleTypes: () -> Unit, onOpenDocTypes: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val active by vm.activeVehicle.collectAsStateWithLifecycle()
    val allDocs by vm.allDocuments.collectAsStateWithLifecycle()
    val lastBackup by vm.lastBackup.collectAsStateWithLifecycle()
    val changes by vm.changesSinceBackup.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Vehicle?>(null) }
    var deleteTarget by remember { mutableStateOf<Vehicle?>(null) }
    // 0 = "hari ini saja"; kosong hanya bila pengingat belum pernah diatur
    var kmValueText by remember { mutableStateOf(if (Notif.kmReminderEnabled(ctx) || Notif.kmReminderValue(ctx) > 0) Notif.kmReminderValue(ctx).toString() else "") }
    var kmUnit by remember { mutableStateOf(Notif.kmReminderUnit(ctx)) }
    var kmEnabled by remember { mutableStateOf(Notif.kmReminderEnabled(ctx)) }
    var kmHour by remember { mutableStateOf(Notif.kmReminderHour(ctx)) }
    var kmMinute by remember { mutableStateOf(Notif.kmReminderMinute(ctx)) }
    var exactAlarmOk by remember { mutableStateOf(Notif.canExactAlarm(ctx)) }
    // Kembali dari layar izin "Alarm & pengingat": segarkan status + pasang ulang alarm
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { exactAlarmOk = Notif.canExactAlarm(ctx) } }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    var showPaste by remember { mutableStateOf(false) }
    var showSnapshots by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        message = if (granted) "Notifikasi aktif." else "Izin notifikasi ditolak. Aktifkan lewat Pengaturan HP > Aplikasi > OdoCare."
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { pendingRestore = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
            catch (e: Exception) { message = "Gagal membuka file: ${e.message}" }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(6.dp))
        ScreenTitle("Kendaraan", "Kelola kendaraan dan pengaturan")

        SectionCard("Kendaraan Saya", Icons.Default.DirectionsCar) {
            if (vehicles.isEmpty()) {
                EmptyState(Icons.Default.DirectionsCar, "Belum ada kendaraan", "Tambahkan motor atau mobilmu.", compact = true, actionLabel = "Tambah", onAction = { showAdd = true })
            } else {
                vehicles.forEach { v ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(if (v.id == active?.id) PillSelected else SurfaceDark), contentAlignment = Alignment.Center) {
                            Icon(VehicleTypes.icon(v.type), null, tint = if (v.id == active?.id) LavenderPrimary else TextGray, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(listOf(vehicleTypeLabel(v.type), v.plate).filter { it.isNotBlank() }.joinToString(" · "), color = TextGray, fontSize = 11.sp)
                        }
                        if (v.id == active?.id) StatusBadge("Aktif", StatusGreenBg to StatusGreenText)
                        else TextButton(onClick = { vm.setActiveVehicle(v.id) }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(30.dp)) { Text("Aktifkan", color = LavenderPrimary, fontSize = 12.sp) }
                        ItemMenu(onEdit = { editTarget = v }, onDelete = { deleteTarget = v })
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldBorder)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = LavenderPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah Kendaraan", fontSize = 13.sp, color = LavenderPrimary)
                }
            }
        }

        SectionCard("Master Data", Icons.Default.Category) {
            ActionRow(Icons.Default.DirectionsCar, "Master jenis kendaraan", subtitle = "Motor, mobil, dan jenis lain yang kamu tambahkan", onClick = onOpenVehicleTypes)
            ActionRow(Icons.Default.Tune, "Master kategori", subtitle = "Daftar komponen servis bawaan per jenis kendaraan", onClick = onOpenMaster)
            ActionRow(Icons.Default.Description, "Master jenis dokumen", subtitle = "Pajak, STNK, SIM, asuransi, dan jenis lain", onClick = onOpenDocTypes)
        }

        val kmSaved = Notif.kmReminderValue(ctx)
        val kmSummary = when {
            !Notif.kmReminderEnabled(ctx) -> "Mati"
            kmSaved == 0 -> "Hari ini saja · ${fmtTime(Notif.kmReminderHour(ctx), Notif.kmReminderMinute(ctx))}"
            else -> "Tiap $kmSaved ${Notif.kmReminderUnit(ctx).label.lowercase()} · ${fmtTime(Notif.kmReminderHour(ctx), Notif.kmReminderMinute(ctx))}"
        }
        CollapsibleSectionCard("Pengingat Update KM", Icons.Default.Speed, kmSummary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ingatkan jika KM belum dicatat selama:", Modifier.weight(1f), color = TextGray, fontSize = 12.sp)
                Switch(checked = kmEnabled, onCheckedChange = { kmEnabled = it }, modifier = Modifier.scale(0.8f))
            }
            if (kmEnabled) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(kmValueText, { kmValueText = it }, "Lama (angka)", icon = Icons.Default.Timer, allowZero = true, hint = "Isi 0 untuk mengingatkan hari ini saja")
                    if (kmValueText == "0") FormNote("Pengingat dikirim sekali hari ini pada jam yang dipilih, lalu mati sendiri.")
                    else UnitChips(kmUnit) { kmUnit = it }
                    TimeField("Jam notifikasi", kmHour, kmMinute) { h, m -> kmHour = h; kmMinute = m }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(12.dp), onClick = {
                val v = kmValueText.toIntOrNull()
                if (kmEnabled && v == null) {
                    message = "Isi angkanya dulu (0 = hari ini saja), atau matikan pengingat."
                } else {
                    val n = v ?: 0
                    Notif.setKmReminder(ctx, kmEnabled, n, kmUnit, kmHour, kmMinute)
                    Notif.schedulePolling(ctx)
                    Notif.onDataChanged(ctx)   // jam baru langsung berlaku (alarm dipasang ulang)
                    val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                    message = when {
                        !kmEnabled -> "Pengingat update KM dimatikan."
                        n == 0 && nowMin >= kmHour * 60 + kmMinute -> "Jam ${fmtTime(kmHour, kmMinute)} hari ini sudah lewat, jadi pengingat dikirim sekarang. Setelah itu pengingat mati sendiri."
                        n == 0 -> "Pengingat update KM hari ini pukul ${fmtTime(kmHour, kmMinute)}, sekali saja."
                        else -> "Pengingat update KM tiap $n ${kmUnit.label.lowercase()} pukul ${fmtTime(kmHour, kmMinute)}, diulang sampai KM diperbarui."
                    }
                }
            }) { Text("Simpan", fontSize = 13.sp) }
        }

        CollapsibleSectionCard(
            "Notifikasi & Kalender", Icons.Default.Notifications,
            summary = if (Build.VERSION.SDK_INT >= 31 && !exactAlarmOk) "Izin alarm tepat waktu belum aktif" else "Siap",
            summaryColor = if (Build.VERSION.SDK_INT >= 31 && !exactAlarmOk) StatusYellowText else TextGray
        ) {
            FormNote("Notifikasi muncul tepat di jam yang diatur. Jam & on/off pajak/dokumen diatur di form dokumen, komponen servis di form komponen.")
            Spacer(Modifier.height(4.dp))
            if (Build.VERSION.SDK_INT >= 31 && !exactAlarmOk) {
                ActionRow(Icons.Default.AlarmOn, "Izinkan alarm tepat waktu", subtitle = "Tanpa izin ini notifikasi bisa telat beberapa menit") {
                    try { ctx.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))) }
                    catch (e: Exception) { message = "Buka Pengaturan HP > Aplikasi > OdoCare > Alarm & pengingat, lalu izinkan." }
                }
            }
            ActionRow(Icons.Default.NotificationsActive, "Aktifkan notifikasi") {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else message = "Notifikasi sudah aktif."
            }
            ActionRow(Icons.Default.NotificationAdd, "Kirim notifikasi tes") { vm.testNotification() }
            ActionRow(Icons.Default.CalendarMonth, "Ekspor ke kalender (.ics)", enabled = allDocs.isNotEmpty(), subtitle = if (allDocs.isEmpty()) "Belum ada dokumen untuk diekspor" else null) {
                try {
                    val ics = Exports.icsText(vehicles, allDocs, ::docTypeLabel)
                    Exports.shareFile(ctx, "odocare-pajak.ics", "text/calendar", ics.toByteArray(Charsets.UTF_8))
                } catch (e: Exception) { message = "Gagal ekspor: ${e.message}" }
            }
        }

        CollapsibleSectionCard(
            "Backup & Data", Icons.Default.Backup,
            summary = if (lastBackup == 0L) "Belum pernah backup" else "Backup terakhir ${fmtDate(lastBackup)}",
            summaryColor = if (lastBackup == 0L) StatusYellowText else TextGray
        ) {
            Text(if (lastBackup == 0L) "Belum pernah backup." else "Backup terakhir: ${fmtDateTime(lastBackup)}", color = TextWhite, fontSize = 12.sp)
            Text("$changes perubahan sejak backup terakhir", color = if (changes > 0) StatusYellowText else TextGray, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            ActionRow(Icons.Default.Save, "Simpan file backup") {
                vm.exportBackup { json ->
                    try {
                        val name = "odocare-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".json"
                        Exports.shareFile(ctx, name, "application/json", json.toByteArray(Charsets.UTF_8))
                        vm.markBackedUp()
                    } catch (e: Exception) { message = "Gagal menyimpan backup: ${e.message}" }
                }
            }
            ActionRow(Icons.Default.FolderOpen, "Buka file backup") { openLauncher.launch(arrayOf("*/*")) }
            ActionRow(Icons.Default.ContentCopy, "Salin teks backup") { vm.exportBackup { json -> clipboard.setText(AnnotatedString(json)); vm.markBackedUp(); message = "Teks backup disalin ke clipboard." } }
            ActionRow(Icons.Default.ContentPaste, "Tempel teks untuk restore") { showPaste = true }
            ActionRow(Icons.Default.Restore, "Snapshot otomatis") { showSnapshots = true }
        }

        AboutCard()
        Spacer(Modifier.height(20.dp))
    }

    if (showAdd) VehicleFormDialog("Tambah Kendaraan", null, { showAdd = false }) { n, p, t, k, a, d -> vm.addVehicle(n, p, t, k, a, d); showAdd = false }
    editTarget?.let { v -> VehicleFormDialog("Ubah Kendaraan", v, { editTarget = null }) { n, p, _, k, _, _ -> vm.updateVehicle(v.copy(name = n.trim(), plate = p.trim().uppercase(), initialKm = k)); editTarget = null } }
    deleteTarget?.let { v -> ConfirmDialog("Hapus kendaraan?", "\"${v.name}\" beserta seluruh odometer, jadwal servis, riwayat, dan dokumennya akan dihapus permanen.", "Hapus", { vm.deleteVehicle(v.id); deleteTarget = null }, { deleteTarget = null }) }

    if (showPaste) {
        var text by remember { mutableStateOf("") }
        FormDialog("Tempel Teks Backup", Icons.Default.ContentPaste, { showPaste = false }, onConfirm = { pendingRestore = text; showPaste = false }, confirmLabel = "Lanjut", confirmEnabled = text.isNotBlank()) {
            AppTextField(text, { text = it }, "Teks backup (JSON)", Icons.Default.Code, singleLine = false, minLines = 5, modifier = Modifier.fillMaxWidth())
        }
    }
    if (showSnapshots) {
        val snaps = remember { Snapshots.list(ctx) }
        AlertDialog(onDismissRequest = { showSnapshots = false }, shape = RoundedCornerShape(20.dp), containerColor = SurfaceDark, title = { Text("Snapshot Otomatis", fontSize = 16.sp, color = TextWhite) },
            text = {
                if (snaps.isEmpty()) EmptyState(Icons.Default.Restore, "Belum ada snapshot", "Dibuat otomatis saat data berubah (maks. satu per 10 menit, 30 terakhir).", compact = true)
                else Column(Modifier.verticalScroll(rememberScrollState())) {
                    snaps.forEach { (time, file) ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            try { pendingRestore = file.readText(); showSnapshots = false } catch (e: Exception) { message = "Gagal membaca snapshot." }
                        }.padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restore, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(fmtDateTime(time), color = TextWhite, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSnapshots = false }) { Text("Tutup") } })
    }
    pendingRestore?.let { json ->
        ConfirmDialog("Pulihkan data?", "Semua data saat ini akan DIGANTI dengan isi backup. Simpan backup dulu kalau perlu.", "Pulihkan", {
            pendingRestore = null
            vm.restore(json) { err -> message = err?.let { "Gagal memulihkan: $it" } ?: "Data berhasil dipulihkan." }
        }, { pendingRestore = null })
    }
    message?.let { InfoDialog(it) { message = null } }
}

/** Tentang aplikasi: penjelasan singkat + pembuat. Versi dibaca dari paket aplikasi. */
@Composable
private fun AboutCard() {
    val ctx = LocalContext.current
    val version = remember { try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" } }
    CollapsibleSectionCard("Tentang OdoCare", Icons.Default.Info, summary = "Kolaborasi Andino & Muh. Nur Ali") {
        Text("OdoCare membantu merawat kendaraanmu: catat KM odometer, pantau jadwal servis tiap komponen, dan dapat pengingat sebelum pajak atau dokumen jatuh tempo.", color = TextWhite, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(10.dp))
        listOf(
            Icons.Default.Speed to "Catat KM dan lihat riwayatnya",
            Icons.Default.Build to "Status servis: Aman, Segera, Lewat",
            Icons.Default.Description to "Pajak, STNK, SIM, dan asuransi diingatkan",
            Icons.Default.Backup to "Data tersimpan di HP, bisa di-backup"
        ).forEach { (icon, text) ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(text, color = TextGray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Kolaborasi Andino & Muh. Nur Ali" + if (version.isNotBlank()) " · Versi $version" else "", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
