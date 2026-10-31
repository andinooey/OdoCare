@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var viewImage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Riwayat Servis${vehicle?.let { " · ${it.name}" } ?: ""}", onBack) { IconButton(onClick = onOpenOdometer) { Icon(Icons.Default.Speed, "Odometer", tint = TextWhite, modifier = Modifier.size(22.dp)) } } }
    ) { pad ->
        if (records.isEmpty()) {
            EmptyState(Icons.Default.History, "Belum ada riwayat servis", "Tap \"Catat servis\" pada komponen di tab Servis. Riwayatnya muncul di sini.", Modifier.padding(pad).padding(top = 48.dp))
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                items(records, key = { it.id }) { r ->
                    Surface(color = SurfaceVariantDark, shape = CardShape) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.padding(top = 2.dp).size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) { Icon(Icons.Default.Build, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(r.title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    if (r.cost > 0) Text(rupiah(r.cost), fontSize = 12.sp, color = LavenderPrimary, fontWeight = FontWeight.Medium)
                                }
                                Text("${fmtDate(r.date)} · ${fmtKm(r.km)} km", fontSize = 11.sp, color = TextGray)
                                if (!r.notes.isNullOrBlank()) Text(r.notes, fontSize = 11.sp, color = TextGray)
                                if (r.imagePath != null) {
                                    Spacer(Modifier.height(6.dp))
                                    StoredImage(r.imagePath, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).clickable { viewImage = r.imagePath })
                                }
                            }
                            ItemMenu(onEdit = { editTarget = r }, onDelete = { deleteTarget = r })
                        }
                    }
                }
            }
        }
    }
    editTarget?.let { r ->
        ServiceFormDialog("Edit Riwayat", r.km, r.date, r.cost, r.notes, r.imagePath, { editTarget = null }, { km, date, cost, notes, img ->
            vm.updateRecord(r.copy(km = km, date = date, cost = cost, notes = notes, imagePath = img)); editTarget = null
        }, subtitle = r.title)
    }
    viewImage?.let { path ->
        AlertDialog(onDismissRequest = { viewImage = null }, containerColor = SurfaceDark, shape = RoundedCornerShape(20.dp), text = { StoredImage(path, Modifier.fillMaxWidth(), maxSide = 1600, contentScale = ContentScale.Fit) },
            confirmButton = { TextButton(onClick = { viewImage = null }) { Text("Tutup") } })
    }
    deleteTarget?.let { r -> ConfirmDialog("Hapus riwayat?", "${r.title} (${fmtDate(r.date)}) akan dihapus. Jadwal servis tidak berubah.", "Hapus", { vm.deleteRecord(r.id); deleteTarget = null }, { deleteTarget = null }) }
}
