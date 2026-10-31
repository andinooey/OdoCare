@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.ServiceRecord
import com.odocare.app.ui.*

/** Riwayat service per kendaraan: tanggal, KM, biaya, catatan. Bisa diubah dan dihapus. */
@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit, onOpenOdometer: () -> Unit) {
    val records by vm.records.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<ServiceRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<ServiceRecord?>(null) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Riwayat Servis${vehicle?.let { " · ${it.name}" } ?: ""}", onBack) { IconButton(onClick = onOpenOdometer) { Icon(Icons.Default.Speed, "Odometer", tint = TextWhite) } } }
    ) { pad ->
        if (records.isEmpty()) {
            Column(Modifier.padding(pad).padding(16.dp)) { Text("Belum ada riwayat. Tap \"Sudah service\" di tab Servis untuk mencatat.", color = TextGray) }
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
                items(records, key = { it.id }) { r ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, color = TextWhite, fontWeight = FontWeight.Bold)
                                Text("${fmtDate(r.date)} · ${fmtKm(r.km)} km", style = MaterialTheme.typography.bodySmall, color = TextGray)
                                if (r.cost > 0) Text(rupiah(r.cost), style = MaterialTheme.typography.bodyMedium, color = LavenderPrimary)
                                if (!r.notes.isNullOrBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall, color = TextGray)
                            }
                            IconButton(onClick = { editTarget = r }) { Icon(Icons.Default.Edit, "Edit", tint = TextGray) }
                            IconButton(onClick = { deleteTarget = r }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                        }
                    }
                }
            }
        }
    }
    editTarget?.let { r ->
        ServiceFormDialog("Edit Riwayat: ${r.title}", r.km, r.date, r.cost, r.notes, { editTarget = null }) { km, date, cost, notes ->
            vm.updateRecord(r.copy(km = km, date = date, cost = cost, notes = notes)); editTarget = null
        }
    }
    deleteTarget?.let { r -> ConfirmDialog("Hapus riwayat?", "${r.title} (${fmtDate(r.date)}) akan dihapus. Jadwal servis tidak berubah.", "Hapus", { vm.deleteRecord(r.id); deleteTarget = null }, { deleteTarget = null }) }
}
