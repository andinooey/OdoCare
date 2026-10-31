@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.MasterCategory
import com.odocare.app.ui.*

/** Master kategori: dipakai untuk kendaraan baru dan dropdown tambah kategori. Tidak mengubah kendaraan yang sudah ada. */
@Composable
fun MasterScreen(vm: MainViewModel, onBack: () -> Unit) {
    val masters by vm.masters.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<MasterCategory?>(null) }
    var deleteTarget by remember { mutableStateOf<MasterCategory?>(null) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Master Kategori", onBack) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }, containerColor = LavenderPrimary, contentColor = Color.Black) { Icon(Icons.Default.Add, "Tambah") } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)) {
            item { Text("Perubahan master tidak mengubah kendaraan yang sudah ada.", color = TextGray, fontSize = 12.sp) }
            items(masters, key = { it.id }) { m ->
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.name, color = TextWhite, fontWeight = FontWeight.Bold)
                            val parts = mutableListOf(vehicleTypeLabel(m.vehicleType))
                            if (m.defaultKm > 0) parts += "${fmtKm(m.defaultKm)} km"
                            if (m.defaultMonths > 0) parts += "${m.defaultMonths} bulan"
                            Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                        IconButton(onClick = { editTarget = m }) { Icon(Icons.Default.Edit, "Edit", tint = TextGray) }
                        IconButton(onClick = { deleteTarget = m }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                    }
                }
            }
        }
    }
    if (showAdd) MasterFormDialog("Tambah Master", null, { showAdd = false }) { n, t, k, mo -> vm.addMaster(n, t, k, mo); showAdd = false }
    editTarget?.let { m -> MasterFormDialog("Edit Master", m, { editTarget = null }) { n, t, k, mo -> vm.updateMaster(m.copy(name = n, vehicleType = t, defaultKm = k, defaultMonths = mo)); editTarget = null } }
    deleteTarget?.let { m -> ConfirmDialog("Hapus master?", "\"${m.name}\" dihapus dari master. Kendaraan yang sudah ada tidak terpengaruh.", "Hapus", { vm.deleteMaster(m.id); deleteTarget = null }, { deleteTarget = null }) }
}

@Composable
private fun MasterFormDialog(title: String, initial: MasterCategory?, onDismiss: () -> Unit, onSave: (name: String, type: String, km: Int, months: Int) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.vehicleType ?: "ALL") }
    var kmT by remember { mutableStateOf(initial?.defaultKm?.takeIf { it > 0 }?.toString() ?: "") }
    var moT by remember { mutableStateOf(initial?.defaultMonths?.takeIf { it > 0 }?.toString() ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "MOTOR", "MOBIL").forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(vehicleTypeLabel(t)) }) }
                }
                NumberField(kmT, { kmT = it }, "Interval km default")
                NumberField(moT, { moT = it }, "Interval bulan default (kosong = tidak dipakai)")
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && (posInt(kmT) != null || posInt(moT) != null), onClick = { onSave(name.trim(), type, kmT.toIntOrNull() ?: 0, moT.toIntOrNull() ?: 0) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
