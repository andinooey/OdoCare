@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.odocare.app.domain.Engines
import com.odocare.app.domain.IntervalUnit
import com.odocare.app.domain.Reminder
import com.odocare.app.domain.Reminders
import com.odocare.app.ui.*

/** Hasil isian form dokumen */
private data class DocForm(
    val type: String, val expiry: Long, val number: String?,
    val lastRenewed: Long?, val intervalValue: Int?, val intervalUnit: IntervalUnit?, val reminders: String
)

@Composable
fun DocumentsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val docs by vm.documents.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<LegalDocument?>(null) }
    var renewTarget by remember { mutableStateOf<LegalDocument?>(null) }
    var deleteTarget by remember { mutableStateOf<LegalDocument?>(null) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Pajak & Dokumen", onBack) },
        floatingActionButton = { if (vehicle != null) FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") } }
    ) { pad ->
        if (docs.isEmpty()) {
            Column(Modifier.padding(pad).padding(16.dp)) { Text(if (vehicle == null) "Tambahkan kendaraan dulu di tab Kendaraan." else "Belum ada dokumen.", color = TextGray) }
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)) {
                items(docs, key = { it.id }) { d ->
                    val view = Engines.documentView(d)
                    val colors = docColor(view.status)
                    val unit = IntervalUnit.parse(d.intervalUnit)
                    StatusStripeCard(colors.second) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(docTypeLabel(d.type), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = TextWhite)
                            StatusBadge(view.status.label, colors)
                        }
                        if (!d.number.isNullOrBlank()) Text("No. ${d.number}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Text("Jatuh tempo: ${fmtDate(d.expiryDate)} (${docDaysText(view.daysRemaining)})", style = MaterialTheme.typography.bodySmall, color = colors.second)
                        d.lastRenewedDate?.let { Text("Terakhir perpanjang: ${fmtDate(it)}", style = MaterialTheme.typography.bodySmall, color = TextGray) }
                        if (d.intervalValue != null && unit != null) Text("Interval: tiap ${d.intervalValue} ${unit.label.lowercase()}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Text("Pengingat: ${Reminders.summary(d.reminders)}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { renewTarget = d }, border = BorderStroke(1.dp, LavenderPrimary)) { Text("Perpanjang", color = LavenderPrimary) }
                            IconButton(onClick = { editTarget = d }) { Icon(Icons.Default.Edit, "Edit", tint = TextGray) }
                            IconButton(onClick = { deleteTarget = d }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) DocumentFormDialog("Tambah Dokumen", null, { showAdd = false }) { f ->
        vm.addDocument(f.type, f.expiry, f.number, f.lastRenewed, f.intervalValue, f.intervalUnit?.name, f.reminders); showAdd = false
    }
    editTarget?.let { d ->
        DocumentFormDialog("Edit Dokumen", d, { editTarget = null }) { f ->
            vm.updateDocument(d.copy(type = f.type, expiryDate = f.expiry, number = f.number, lastRenewedDate = f.lastRenewed, intervalValue = f.intervalValue, intervalUnit = f.intervalUnit?.name, reminders = f.reminders))
            editTarget = null
        }
    }
    renewTarget?.let { d -> RenewDialog(d, { renewTarget = null }) { renewed, newExpiry -> vm.renewDocument(d, renewed, newExpiry); renewTarget = null } }
    deleteTarget?.let { d -> ConfirmDialog("Hapus dokumen?", "${docTypeLabel(d.type)} akan dihapus beserta riwayat perpanjangannya.", "Hapus", { vm.deleteDocument(d.id); deleteTarget = null }, { deleteTarget = null }) }
}

private fun approxDays(r: Reminder): Int = r.value * when (r.unit) { IntervalUnit.DAY -> 1; IntervalUnit.MONTH -> 30; IntervalUnit.YEAR -> 365 }

@Composable
private fun DocumentFormDialog(title: String, initial: LegalDocument?, onDismiss: () -> Unit, onSave: (DocForm) -> Unit) {
    var type by remember { mutableStateOf(initial?.type ?: docTypes.first().first) }
    var number by remember { mutableStateOf(initial?.number ?: "") }
    var lastRenewed by remember { mutableStateOf(initial?.lastRenewedDate) }
    var intervalText by remember { mutableStateOf(initial?.intervalValue?.toString() ?: "") }
    var intervalUnit by remember { mutableStateOf(IntervalUnit.parse(initial?.intervalUnit) ?: IntervalUnit.YEAR) }
    var manualExpiry by remember { mutableStateOf(initial?.expiryDate) }
    var reminders by remember { mutableStateOf(Reminders.parse(initial?.reminders ?: Reminders.DEFAULT)) }
    var newRemText by remember { mutableStateOf("") }
    var newRemUnit by remember { mutableStateOf(IntervalUnit.DAY) }
    var menu by remember { mutableStateOf(false) }

    // Jatuh tempo otomatis = tanggal terakhir perpanjang + interval (mis. 1/1/2026 + 1 tahun = 1/1/2027)
    val lr = lastRenewed
    val iv = posInt(intervalText)
    val computed: Long? = if (lr != null && iv != null) localDateToMillis(intervalUnit.plus(msToLocalDate(lr), iv)) else null
    val expiry: Long? = computed ?: manualExpiry

    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { menu = true }) { Text(docTypeLabel(type)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        docTypes.forEach { (k, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { type = k; menu = false }) }
                    }
                }
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Nomor (opsional)") }, singleLine = true)

                Text("Perpanjang & interval", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                DateField("Tanggal terakhir perpanjang", lastRenewed) { lastRenewed = it }
                NumberField(intervalText, { intervalText = it }, "Interval (angka)")
                UnitChips(intervalUnit) { intervalUnit = it }

                if (computed != null) {
                    Text("Jatuh tempo: ${fmtDate(computed)} (otomatis)", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                } else {
                    DateField("Pilih tanggal jatuh tempo", manualExpiry) { manualExpiry = it }
                    Text("Isi tanggal terakhir perpanjang dan interval agar jatuh tempo dihitung otomatis.", color = TextGray, fontSize = 11.sp)
                }

                Text("Pengingat notifikasi", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (reminders.isEmpty()) Text("Tidak ada, hanya di hari jatuh tempo.", color = TextGray, fontSize = 12.sp)
                reminders.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.label(), Modifier.weight(1f), color = TextWhite)
                        IconButton(onClick = { reminders = reminders - r }) { Icon(Icons.Default.Close, "Hapus pengingat", tint = TextGray) }
                    }
                }
                NumberField(newRemText, { newRemText = it }, "Ingatkan berapa lama sebelum")
                UnitChips(newRemUnit) { newRemUnit = it }
                TextButton(enabled = posInt(newRemText) != null, onClick = {
                    posInt(newRemText)?.let { v -> reminders = (reminders + Reminder(v, newRemUnit)).distinct().sortedByDescending { approxDays(it) } }
                    newRemText = ""
                }) { Text("Tambah pengingat") }
            }
        },
        confirmButton = {
            TextButton(enabled = expiry != null, onClick = {
                expiry?.let { e -> onSave(DocForm(type, e, number.ifBlank { null }, lastRenewed, iv, if (iv != null) intervalUnit else null, Reminders.encode(reminders))) }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

/** Perpanjang: pilih tanggal perpanjang; jatuh tempo baru = tanggal itu + interval dokumen (atau pilih manual bila tanpa interval). */
@Composable
private fun RenewDialog(doc: LegalDocument, onDismiss: () -> Unit, onConfirm: (renewedDate: Long, newExpiry: Long) -> Unit) {
    var renewed by remember { mutableStateOf(todayMillis()) }
    var manual by remember { mutableStateOf<Long?>(localDateToMillis(msToLocalDate(doc.expiryDate).plusYears(1))) }
    val iv = doc.intervalValue
    val unit = IntervalUnit.parse(doc.intervalUnit)
    val computed: Long? = if (iv != null && iv > 0 && unit != null) localDateToMillis(unit.plus(msToLocalDate(renewed), iv)) else null
    val unitLabel = unit?.label?.lowercase() ?: ""
    val newExpiry: Long? = computed ?: manual

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Perpanjang ${docTypeLabel(doc.type)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Tanggal perpanjang", renewed) { renewed = it }
                if (computed != null) Text("Jatuh tempo baru: ${fmtDate(computed)} (tiap $iv $unitLabel)", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                else DateField("Jatuh tempo baru", manual) { manual = it }
            }
        },
        confirmButton = { TextButton(enabled = newExpiry != null, onClick = { newExpiry?.let { onConfirm(renewed, it) } }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
