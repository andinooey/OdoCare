@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.odocare.app.data.AttachmentStore
import com.odocare.app.data.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.odocare.app.data.LegalDocument
import com.odocare.app.domain.DocStatus
import com.odocare.app.domain.Engines
import com.odocare.app.domain.IntervalUnit
import com.odocare.app.domain.Reminder
import com.odocare.app.domain.Reminders
import com.odocare.app.ui.*

/** Hasil isian form dokumen */
private data class DocForm(
    val type: String, val expiry: Long, val number: String?,
    val lastRenewed: Long?, val intervalValue: Int?, val intervalUnit: IntervalUnit?, val reminders: String,
    val notifyEnabled: Boolean, val notifyHour: Int, val notifyMinute: Int,
    val attachmentPath: String?, val attachmentName: String?
)

private val docFilterLabels = listOf("Semua", "Lewat", "Segera", "Aman")

/** Bagian "Pajak & Dokumen" di tab Servis: dokumen kendaraan aktif + tab status seperti Jadwal & Komponen. */
@Composable
fun DocumentsScreen(vm: MainViewModel, onOpenDocTypes: () -> Unit) {
    val docs by vm.documents.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<LegalDocument?>(null) }
    var renewTarget by remember { mutableStateOf<LegalDocument?>(null) }
    var deleteTarget by remember { mutableStateOf<LegalDocument?>(null) }
    var tab by remember { mutableStateOf(0) }

    // Urut jatuh tempo terdekat (dari database). Jatuh tempo hari ini masuk "Segera" karena belum lewat.
    val views = docs.map { Engines.documentView(it) }
    val late = views.filter { it.status == DocStatus.LATE }
    val soon = views.filter { it.status == DocStatus.SOON || it.status == DocStatus.DUE_TODAY }
    val safe = views.filter { it.status == DocStatus.SAFE }
    val shown = when (tab) { 1 -> late; 2 -> soon; 3 -> safe; else -> views }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        floatingActionButton = { if (vehicle != null) AddFab { showAdd = true } }
    ) { pad ->
        Column(Modifier.padding(pad).statusBarsPadding()) {
            ServiceSectionTitle(vm, vehicle?.name, Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 12.dp)) {
                IconButton(onClick = onOpenDocTypes) { Icon(Icons.Default.Category, "Master Jenis Dokumen", tint = Accent, modifier = Modifier.size(22.dp)) }
            }
            if (vehicle == null) {
                NoVehicleState(vm, Modifier.padding(top = 24.dp))
            } else {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    StatusFilterRow(
                        docFilterLabels, listOf(views.size, late.size, soon.size, safe.size),
                        listOf(PillSelected to TextPrimary, StatusRedBg to StatusRedText, StatusYellowBg to StatusYellowText, StatusGreenBg to StatusGreenText),
                        tab
                    ) { tab = it }
                }
                if (shown.isEmpty()) {
                    when (tab) {
                        1 -> EmptyState(Icons.Default.CheckCircle, "Tidak ada yang lewat", "Semua dokumen masih berlaku.", Modifier.padding(top = 24.dp))
                        2 -> EmptyState(Icons.Default.CheckCircle, "Tidak ada yang segera", "Belum ada dokumen yang mendekati jatuh tempo.", Modifier.padding(top = 24.dp))
                        3 -> EmptyState(Icons.Default.Description, "Belum ada yang aman", "Dokumen yang masih lama jatuh temponya tampil di sini.", Modifier.padding(top = 24.dp))
                        else -> EmptyState(Icons.Default.Description, "Belum ada dokumen", "Simpan pajak, STNK, SIM, atau asuransi agar jatuh temponya diingatkan.", Modifier.padding(top = 24.dp), actionLabel = "Tambah Dokumen", onAction = { showAdd = true })
                    }
                } else {
                    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)) {
                        items(shown, key = { it.doc.id }) { view ->
                            val d = view.doc
                            val colors = docColor(view.status)
                            val unit = IntervalUnit.parse(d.intervalUnit)
                            StatusStripeCard(colors.second) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(docTypeLabel(d.type), Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    DocStatusChip(view.status)
                                    ItemMenu(onEdit = { editTarget = d }, onDelete = { deleteTarget = d })
                                }
                                Text("Jatuh tempo ${fmtDate(d.expiryDate)} · ${docDaysText(view.daysRemaining)}", fontSize = 12.sp, color = colors.second)
                                val meta = mutableListOf<String>()
                                if (!d.number.isNullOrBlank()) meta += "No. ${d.number}"
                                d.lastRenewedDate?.let { meta += "Diperpanjang ${fmtDate(it)}" }
                                if (d.intervalValue != null && unit != null) meta += "Berlaku ${d.intervalValue} ${unit.label.lowercase()}"
                                if (meta.isNotEmpty()) Text(meta.joinToString(" · "), fontSize = 12.sp, color = TextMuted)
                                if (d.attachmentPath != null) {
                                    val ctx = LocalContext.current
                                    var openFailed by remember { mutableStateOf(false) }
                                    Row(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).clickable { openFailed = !AttachmentStore.open(ctx, d.attachmentPath) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (AttachmentStore.isImage(d.attachmentPath)) Icons.Default.Image else Icons.Default.AttachFile, null, tint = Accent, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (openFailed) "File tidak bisa dibuka" else d.attachmentName ?: "Lampiran", fontSize = 12.sp, color = if (openFailed) DangerText else Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (d.notifyEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (d.notifyEnabled) "${Reminders.summary(d.reminders)} · ${fmtTime(d.notifyHour, d.notifyMinute)}" else "Notifikasi mati", fontSize = 12.sp, color = TextMuted, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { renewTarget = d }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(30.dp)) {
                                        Icon(Icons.Default.Autorenew, null, tint = Accent, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Perpanjang", color = Accent, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) DocumentFormDialog("Tambah Dokumen", null, vehicle?.type, vehicle?.name, { showAdd = false }) { f ->
        vm.addDocument(f.type, f.expiry, f.number, f.lastRenewed, f.intervalValue, f.intervalUnit?.name, f.reminders, f.notifyEnabled, f.notifyHour, f.notifyMinute, f.attachmentPath, f.attachmentName); showAdd = false
    }
    editTarget?.let { d ->
        DocumentFormDialog("Edit Dokumen", d, vehicle?.type, vehicle?.name, { editTarget = null }) { f ->
            vm.updateDocument(d.copy(type = f.type, expiryDate = f.expiry, number = f.number, lastRenewedDate = f.lastRenewed, intervalValue = f.intervalValue, intervalUnit = f.intervalUnit?.name, reminders = f.reminders, notifyEnabled = f.notifyEnabled, notifyHour = f.notifyHour, notifyMinute = f.notifyMinute, attachmentPath = f.attachmentPath, attachmentName = f.attachmentName))
            if (d.attachmentPath != null && d.attachmentPath != f.attachmentPath) ImageStore.delete(d.attachmentPath)   // lampiran lama diganti / dihapus
            editTarget = null
        }
    }
    renewTarget?.let { d -> RenewDialog(d, { renewTarget = null }) { renewed, newExpiry -> vm.renewDocument(d, renewed, newExpiry); renewTarget = null } }
    deleteTarget?.let { d -> ConfirmDialog("Hapus dokumen?", "${docTypeLabel(d.type)} akan dihapus beserta riwayat perpanjangannya.", "Hapus", { vm.deleteDocument(d.id); deleteTarget = null }, { deleteTarget = null }) }
}

private fun approxDays(r: Reminder): Int = r.value * when (r.unit) { IntervalUnit.DAY -> 1; IntervalUnit.MONTH -> 30; IntervalUnit.YEAR -> 365 }

/** Kotak info jatuh tempo hasil hitung otomatis. */
@Composable
private fun ExpiryInfo(text: String) {
    Surface(color = StatusBlueBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EventAvailable, null, tint = StatusBlueText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, fontSize = 13.sp, color = StatusBlueText, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DocumentFormDialog(title: String, initial: LegalDocument?, vehicleType: String?, vehicleName: String?, onDismiss: () -> Unit, onSave: (DocForm) -> Unit) {
    // Pilihan jenis mengikuti master jenis dokumen yang cocok dengan jenis kendaraan aktif
    val typeOptions = DocTypes.forVehicle(vehicleType, keep = initial?.type)
    val firstDefault = if (initial == null) typeOptions.firstOrNull() else null
    var type by remember { mutableStateOf(initial?.type ?: typeOptions.firstOrNull()?.code ?: "") }
    var number by remember { mutableStateOf(initial?.number ?: "") }
    var lastRenewed by remember { mutableStateOf(initial?.lastRenewedDate) }
    var intervalText by remember { mutableStateOf(initial?.intervalValue?.toString() ?: firstDefault?.defaultValue?.takeIf { it > 0 }?.toString() ?: "") }
    var intervalUnit by remember { mutableStateOf(IntervalUnit.parse(initial?.intervalUnit) ?: IntervalUnit.parse(firstDefault?.defaultUnit) ?: IntervalUnit.YEAR) }
    var manualExpiry by remember { mutableStateOf(initial?.expiryDate) }
    var reminders by remember { mutableStateOf(Reminders.parse(initial?.reminders ?: Reminders.DEFAULT)) }
    var newRemText by remember { mutableStateOf("") }
    var newRemUnit by remember { mutableStateOf(IntervalUnit.DAY) }
    var notifyOn by remember { mutableStateOf(initial?.notifyEnabled ?: true) }
    var notifyHour by remember { mutableStateOf(initial?.notifyHour ?: 9) }
    var notifyMinute by remember { mutableStateOf(initial?.notifyMinute ?: 0) }
    var attachPath by remember { mutableStateOf(initial?.attachmentPath) }
    var attachName by remember { mutableStateOf(initial?.attachmentName) }
    var newFiles by remember { mutableStateOf(listOf<String>()) }   // file baru yang dibuat selama form terbuka
    var attachError by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val saved = withContext(Dispatchers.IO) { AttachmentStore.save(ctx, uri) }
            if (saved != null) { attachPath = saved.first; attachName = saved.second; newFiles = newFiles + saved.first; attachError = false } else attachError = true
        }
    }
    // Bersihkan file baru yang tidak jadi dipakai (dibatalkan / diganti / dihapus)
    fun cleanup(keep: String?) { newFiles.filter { it != keep }.forEach { ImageStore.delete(it) } }

    // Jatuh tempo otomatis = tanggal terakhir perpanjang + interval (mis. 1/1/2026 + 1 tahun = 1/1/2027)
    val lr = lastRenewed
    // 0 diperbolehkan: berlaku 0 hari = jatuh tempo di tanggal perpanjang itu juga (notifikasi muncul hari itu)
    val iv = intervalText.toIntOrNull()?.takeIf { it >= 0 }
    val computed: Long? = if (lr != null && iv != null) localDateToMillis(intervalUnit.plus(msToLocalDate(lr), iv)) else null
    val expiry: Long? = computed ?: manualExpiry

    FormDialog(
        title = title, subtitle = vehicleName, icon = Icons.Default.Description, onDismiss = { cleanup(null); onDismiss() }, confirmEnabled = expiry != null && type.isNotBlank(),
        onConfirm = { expiry?.let { e -> cleanup(attachPath); onSave(DocForm(type, e, number.ifBlank { null }, lastRenewed, iv, if (iv != null) intervalUnit else null, Reminders.encode(reminders), notifyOn, notifyHour, notifyMinute, attachPath, attachName)) } }
    ) {
        DropdownField("Jenis dokumen", if (type.isBlank()) null else docTypeLabel(type), Icons.Default.Description, typeOptions.map { it.code to it.name }, placeholder = "Pilih jenis dokumen") { code ->
            type = code
            // Tambah baru: masa berlaku mengikuti bawaan master (kosong bila master tidak punya bawaan)
            if (initial == null) {
                val m = DocTypes.find(code)
                intervalText = m?.defaultValue?.takeIf { it > 0 }?.toString() ?: ""
                IntervalUnit.parse(m?.defaultUnit)?.let { intervalUnit = it }
            }
        }
        if (typeOptions.isEmpty()) FormNote("Belum ada jenis dokumen untuk kendaraan ini. Tambahkan di Master Jenis Dokumen (ikon kanan atas).", DangerText)
        AppTextField(number, { number = it }, "Nomor dokumen (opsional)", Icons.Default.Pin)

        FormGroupLabel("Foto / dokumen")
        val path = attachPath
        if (path == null) {
            PickerField("Lampiran (opsional)", null, Icons.Default.AttachFile, placeholder = "Pilih foto atau file (PDF, dll)") { picker.launch("*/*") }
        } else {
            if (AttachmentStore.isImage(path)) StoredImage(path, Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)), maxSide = 800)
            else Surface(color = CardBg.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldBorder), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InsertDriveFile, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(attachName ?: "Lampiran", fontSize = 13.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { AttachmentStore.open(ctx, path) }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(34.dp), border = BorderStroke(1.dp, FieldBorder)) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(8.dp)); Text("Buka", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { picker.launch("*/*") }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(34.dp), border = BorderStroke(1.dp, FieldBorder)) {
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(8.dp)); Text("Ganti", fontSize = 12.sp)
                }
                TextButton(onClick = { attachPath = null; attachName = null }, modifier = Modifier.height(34.dp)) { Text("Hapus", color = DangerText, fontSize = 12.sp) }
            }
        }
        if (attachError) FormNote("Gagal membaca file (maks. 20 MB).", DangerText)

        FormGroupLabel("Masa berlaku")
        DateField("Terakhir diperpanjang", lastRenewed) { lastRenewed = it }
        NumberField(intervalText, { intervalText = it }, "Berlaku selama", icon = Icons.Default.Update, allowZero = true, hint = "Isi 0 bila jatuh tempo di tanggal perpanjang itu juga")
        UnitChips(intervalUnit) { intervalUnit = it }
        if (computed != null) {
            ExpiryInfo("Jatuh tempo ${fmtDate(computed)} (otomatis)")
        } else {
            DateField("Tanggal jatuh tempo", manualExpiry) { manualExpiry = it }
            FormNote("Isi \"Terakhir diperpanjang\" dan \"Berlaku selama\" agar jatuh tempo dihitung otomatis.")
        }

        FormGroupLabel("Notifikasi")
        NotifyOptions(notifyOn, notifyHour, notifyMinute) { e, h, m -> notifyOn = e; notifyHour = h; notifyMinute = m }

        if (notifyOn) {
        FormGroupLabel("Ingatkan sebelum jatuh tempo")
        if (reminders.isEmpty()) FormNote("Hanya di hari jatuh tempo.")
        reminders.forEach { r ->
            Surface(color = CardBg.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldBorder), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(r.label(), Modifier.weight(1f), fontSize = 13.sp, color = TextPrimary)
                    IconButton(onClick = { reminders = reminders - r }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Hapus pengingat", tint = TextMuted, modifier = Modifier.size(16.dp)) }
                }
            }
        }
        NumberField(newRemText, { newRemText = it }, "Ingatkan sebelum", icon = Icons.Default.AddAlert)
        UnitChips(newRemUnit) { newRemUnit = it }
        OutlinedButton(
            enabled = posInt(newRemText) != null, modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldBorder),
            onClick = {
                posInt(newRemText)?.let { v -> reminders = (reminders + Reminder(v, newRemUnit)).distinct().sortedByDescending { approxDays(it) } }
                newRemText = ""
            }
        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Tambah pengingat", fontSize = 13.sp) }
        }
    }
}

/** Perpanjang: pilih tanggal perpanjang; jatuh tempo baru = tanggal itu + interval dokumen (atau pilih manual bila tanpa interval). */
@Composable
private fun RenewDialog(doc: LegalDocument, onDismiss: () -> Unit, onConfirm: (renewedDate: Long, newExpiry: Long) -> Unit) {
    var renewed by remember { mutableStateOf(todayMillis()) }
    var manual by remember { mutableStateOf<Long?>(localDateToMillis(msToLocalDate(doc.expiryDate).plusYears(1))) }
    val iv = doc.intervalValue
    val unit = IntervalUnit.parse(doc.intervalUnit)
    val computed: Long? = if (iv != null && iv >= 0 && unit != null) localDateToMillis(unit.plus(msToLocalDate(renewed), iv)) else null
    val unitLabel = unit?.label?.lowercase() ?: ""
    val newExpiry: Long? = computed ?: manual

    FormDialog(
        title = "Perpanjang", subtitle = docTypeLabel(doc.type), icon = Icons.Default.Autorenew, onDismiss = onDismiss, confirmEnabled = newExpiry != null,
        onConfirm = { newExpiry?.let { onConfirm(renewed, it) } }
    ) {
        DateField("Tanggal diperpanjang", renewed) { renewed = it }
        if (computed != null) ExpiryInfo("Jatuh tempo baru ${fmtDate(computed)} (berlaku $iv $unitLabel)")
        else DateField("Tanggal jatuh tempo baru", manual) { manual = it }
    }
}
