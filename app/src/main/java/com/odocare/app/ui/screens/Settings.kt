@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
fun SettingsScreen(vm: MainViewModel, onOpenMaster: () -> Unit) {
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
    var kmValueText by remember { mutableStateOf(Notif.kmReminderValue(ctx).takeIf { it > 0 }?.toString() ?: "") }
    var kmUnit by remember { mutableStateOf(Notif.kmReminderUnit(ctx)) }
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

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Kendaraan", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)

        SectionCard("Kendaraan Saya") {
            if (vehicles.isEmpty()) Text("Belum ada kendaraan.", color = TextGray)
            vehicles.forEach { v ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${v.name} (${vehicleTypeLabel(v.type)})", color = TextWhite, fontWeight = if (v.id == active?.id) FontWeight.Bold else FontWeight.Normal)
                        if (v.plate.isNotBlank()) Text(v.plate, color = TextGray, fontSize = 12.sp)
                    }
                    if (v.id != active?.id) TextButton(onClick = { vm.setActiveVehicle(v.id) }) { Text("Aktifkan", color = LavenderPrimary) }
                    IconButton(onClick = { editTarget = v }) { Icon(Icons.Default.Edit, "Edit", tint = TextGray) }
                    IconButton(onClick = { deleteTarget = v }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = Color.Black)) { Text("Tambah Kendaraan") }
        }

        SectionCard("Master Kategori") {
            Text("Atur kategori service default untuk motor dan mobil.", color = TextGray, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenMaster, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Kelola Master", color = LavenderPrimary) }
        }

        SectionCard("Pengingat Update KM") {
            Text("Ingatkan saya memperbarui odometer jika KM belum dicatat selama:", color = TextGray, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            NumberField(kmValueText, { kmValueText = it }, "Setiap (angka, kosong = nonaktif)")
            Spacer(Modifier.height(8.dp))
            UnitChips(kmUnit) { kmUnit = it }
            Spacer(Modifier.height(8.dp))
            Button(colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = Color.Black), onClick = {
                val v = kmValueText.toIntOrNull() ?: 0
                Notif.setKmReminder(ctx, v, kmUnit)
                Notif.scheduleDaily(ctx)
                message = if (v > 0) "Pengingat update KM: setiap $v ${kmUnit.label.lowercase()} (diulang selama KM belum diperbarui)." else "Pengingat update KM dinonaktifkan."
            }) { Text("Simpan") }
        }

        SectionCard("Notifikasi & Kalender") {
            Text("Pengingat pajak dan dokumen diatur per dokumen, saat menambah atau mengedit di menu Pajak & Dokumen. Notifikasi dikirim sekitar pukul 09.00.", color = TextGray, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else message = "Notifikasi sudah aktif."
                }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Aktifkan", color = LavenderPrimary) }
                OutlinedButton(onClick = { vm.testNotification() }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Kirim Tes", color = LavenderPrimary) }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(enabled = allDocs.isNotEmpty(), onClick = {
                try {
                    val ics = Exports.icsText(vehicles, allDocs, ::docTypeLabel)
                    Exports.shareFile(ctx, "odocare-pajak.ics", "text/calendar", ics.toByteArray(Charsets.UTF_8))
                } catch (e: Exception) { message = "Gagal ekspor: ${e.message}" }
            }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Ekspor ke Kalender (.ics)", color = LavenderPrimary) }
        }

        SectionCard("Backup & Data") {
            Text(if (lastBackup == 0L) "Belum pernah backup." else "Backup terakhir: ${fmtDateTime(lastBackup)}", color = TextWhite, fontSize = 13.sp)
            Text("$changes perubahan sejak backup terakhir", color = if (changes > 0) StatusYellowText else TextGray, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = Color.Black), onClick = {
                    vm.exportBackup { json ->
                        try {
                            val name = "odocare-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".json"
                            Exports.shareFile(ctx, name, "application/json", json.toByteArray(Charsets.UTF_8))
                            vm.markBackedUp()
                        } catch (e: Exception) { message = "Gagal menyimpan backup: ${e.message}" }
                    }
                }) { Text("Simpan File Backup") }
                OutlinedButton(onClick = { openLauncher.launch(arrayOf("*/*")) }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Buka File Backup", color = LavenderPrimary) }
                OutlinedButton(onClick = { vm.exportBackup { json -> clipboard.setText(AnnotatedString(json)); vm.markBackedUp(); message = "Teks backup disalin ke clipboard." } }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Salin Teks Backup", color = LavenderPrimary) }
                OutlinedButton(onClick = { showPaste = true }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Tempel Teks untuk Restore", color = LavenderPrimary) }
                OutlinedButton(onClick = { showSnapshots = true }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Snapshot Otomatis", color = LavenderPrimary) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAdd) VehicleFormDialog("Tambah Kendaraan", null, { showAdd = false }) { n, p, t, k -> vm.addVehicle(n, p, t, k); showAdd = false }
    editTarget?.let { v -> VehicleFormDialog("Ubah Kendaraan", v, { editTarget = null }) { n, p, _, k -> vm.updateVehicle(v.copy(name = n.trim(), plate = p.trim().uppercase(), initialKm = k)); editTarget = null } }
    deleteTarget?.let { v -> ConfirmDialog("Hapus kendaraan?", "\"${v.name}\" beserta seluruh odometer, jadwal servis, riwayat, dan dokumennya akan dihapus permanen.", "Hapus", { vm.deleteVehicle(v.id); deleteTarget = null }, { deleteTarget = null }) }

    if (showPaste) {
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showPaste = false }, title = { Text("Tempel Teks Backup") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Teks backup (JSON)") }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp)) },
            confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { pendingRestore = text; showPaste = false }) { Text("Lanjut") } },
            dismissButton = { TextButton(onClick = { showPaste = false }) { Text("Batal") } })
    }
    if (showSnapshots) {
        val snaps = remember { Snapshots.list(ctx) }
        AlertDialog(onDismissRequest = { showSnapshots = false }, title = { Text("Snapshot Otomatis") },
            text = {
                if (snaps.isEmpty()) Text("Belum ada snapshot. Snapshot dibuat otomatis saat data berubah (maks. satu per 10 menit, 30 terakhir).")
                else Column(Modifier.verticalScroll(rememberScrollState())) {
                    snaps.forEach { (time, file) ->
                        Text(fmtDateTime(time), color = TextWhite, modifier = Modifier.fillMaxWidth().clickable {
                            try { pendingRestore = file.readText(); showSnapshots = false } catch (e: Exception) { message = "Gagal membaca snapshot." }
                        }.padding(vertical = 12.dp))
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

@Composable
private fun VehicleFormDialog(title: String, initial: Vehicle?, onDismiss: () -> Unit, onSave: (name: String, plate: String, type: String, initialKm: Int) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var plate by remember { mutableStateOf(initial?.plate ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "MOTOR") }
    var kmT by remember { mutableStateOf(initial?.initialKm?.takeIf { it > 0 }?.toString() ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kendaraan") }, singleLine = true)
                OutlinedTextField(value = plate, onValueChange = { plate = it.uppercase() }, label = { Text("Plat nomor") }, singleLine = true)
                if (initial == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MOTOR", "MOBIL").forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(vehicleTypeLabel(t)) }) }
                }
                NumberField(kmT, { kmT = it }, "KM awal")
                if (initial == null) Text("Kategori service dan dokumen default akan dibuat otomatis sesuai jenis kendaraan. Atur tanggal jatuh tempo dokumen di menu Dokumen.", color = TextGray, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, plate, type, kmT.toIntOrNull() ?: 0) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
