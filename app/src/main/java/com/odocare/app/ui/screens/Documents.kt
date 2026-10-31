@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.LegalDocument
import com.odocare.app.domain.Engines
import com.odocare.app.ui.*
import java.time.LocalDate
import java.time.ZoneId

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
                    StatusStripeCard(colors.second) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(docTypeLabel(d.type), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = TextWhite)
                            StatusBadge(view.status.label, colors)
                        }
                        if (!d.number.isNullOrBlank()) Text("No. ${d.number}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Text("Jatuh tempo: ${fmtDate(d.expiryDate)} (${docDaysText(view.daysRemaining)})", style = MaterialTheme.typography.bodySmall, color = colors.second)
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

    if (showAdd) DocumentFormDialog("Tambah Dokumen", null, { showAdd = false }) { t, e, n -> vm.addDocument(t, e, n); showAdd = false }
    editTarget?.let { d -> DocumentFormDialog("Edit Dokumen", d, { editTarget = null }) { t, e, n -> vm.updateDocument(d.copy(type = t, expiryDate = e, number = n)); editTarget = null } }
    renewTarget?.let { d ->
        val base = maxOf(msToLocalDate(d.expiryDate), LocalDate.now())
        val suggestion = base.plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        DatePickerDialogX(suggestion, { renewTarget = null }) { vm.renewDocument(d, it); renewTarget = null }
    }
    deleteTarget?.let { d -> ConfirmDialog("Hapus dokumen?", "${docTypeLabel(d.type)} akan dihapus beserta riwayat perpanjangannya.", "Hapus", { vm.deleteDocument(d.id); deleteTarget = null }, { deleteTarget = null }) }
}

@Composable
private fun DocumentFormDialog(title: String, initial: LegalDocument?, onDismiss: () -> Unit, onSave: (type: String, expiry: Long, number: String?) -> Unit) {
    var type by remember { mutableStateOf(initial?.type ?: docTypes.first().first) }
    var number by remember { mutableStateOf(initial?.number ?: "") }
    var expiry by remember { mutableStateOf(initial?.expiryDate) }
    var menu by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { menu = true }) { Text(docTypeLabel(type)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        docTypes.forEach { (k, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { type = k; menu = false }) }
                    }
                }
                DateField("Pilih tanggal jatuh tempo", expiry) { expiry = it }
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Nomor (opsional)") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = expiry != null, onClick = { expiry?.let { onSave(type, it, number.ifBlank { null }) } }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
