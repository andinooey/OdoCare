@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.MasterCategory
import com.odocare.app.ui.*

/** Master kategori: dipakai untuk kendaraan baru dan dropdown tambah komponen. Tidak mengubah kendaraan yang sudah ada. */
@Composable
fun MasterScreen(vm: MainViewModel, onBack: () -> Unit) {
    val masters by vm.masters.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<MasterCategory?>(null) }
    var deleteTarget by remember { mutableStateOf<MasterCategory?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Master Kategori", onBack) },
        floatingActionButton = { AddFab { showAdd = true } }
    ) { pad ->
        if (masters.isEmpty()) {
            EmptyState(Icons.Default.Category, "Belum ada master kategori", "Master dipakai sebagai daftar komponen bawaan saat menambah jadwal servis.", Modifier.padding(pad).padding(top = 48.dp), actionLabel = "Tambah Master", onAction = { showAdd = true })
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)) {
                item { Text("Perubahan di sini tidak mengubah jadwal kendaraan yang sudah ada.", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp)) }
                items(masters, key = { it.id }) { m ->
                    Surface(color = SurfaceVariantDark, shape = CardShape) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) {
                                Icon(when (m.vehicleType) { "MOBIL" -> Icons.Default.DirectionsCar; "MOTOR" -> Icons.Default.TwoWheeler; else -> Icons.Default.Build }, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                val parts = mutableListOf(vehicleTypeLabel(m.vehicleType))
                                if (m.defaultKm > 0) parts += "${fmtKm(m.defaultKm)} km"
                                if (m.defaultMonths > 0) parts += "${m.defaultMonths} bulan"
                                if (m.defaultKm > 0 || m.defaultMonths > 0) parts += if (m.notifyEnabled) fmtTime(m.notifyHour, m.notifyMinute) else "Notif mati"
                                Text(parts.joinToString(" · "), fontSize = 11.sp, color = TextGray)
                            }
                            ItemMenu(onEdit = { editTarget = m }, onDelete = { deleteTarget = m })
                        }
                    }
                }
            }
        }
    }
    if (showAdd) MasterFormDialog("Tambah Master", null, { showAdd = false }) { n, t, k, mo, ne, nh, nm, wk, wd -> vm.addMaster(n, t, k, mo, ne, nh, nm, wk, wd); showAdd = false }
    editTarget?.let { m -> MasterFormDialog("Edit Master", m, { editTarget = null }) { n, t, k, mo, ne, nh, nm, wk, wd -> vm.updateMaster(m.copy(name = n, vehicleType = t, defaultKm = k, defaultMonths = mo, notifyEnabled = ne, notifyHour = nh, notifyMinute = nm, warnKm = wk, warnDays = wd)); editTarget = null } }
    deleteTarget?.let { m -> ConfirmDialog("Hapus master?", "\"${m.name}\" dihapus dari master. Kendaraan yang sudah ada tidak terpengaruh.", "Hapus", { vm.deleteMaster(m.id); deleteTarget = null }, { deleteTarget = null }) }
}

@Composable
private fun MasterFormDialog(title: String, initial: MasterCategory?, onDismiss: () -> Unit, onSave: (name: String, type: String, km: Int, months: Int, notifyEnabled: Boolean, notifyHour: Int, notifyMinute: Int, warnKm: Int, warnDays: Int) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.vehicleType ?: "ALL") }
    var kmT by remember { mutableStateOf(initial?.defaultKm?.takeIf { it > 0 }?.toString() ?: "") }
    var moT by remember { mutableStateOf(initial?.defaultMonths?.takeIf { it > 0 }?.toString() ?: "") }
    var notifyOn by remember { mutableStateOf(initial?.notifyEnabled ?: true) }
    var notifyHour by remember { mutableStateOf(initial?.notifyHour ?: 9) }
    var notifyMinute by remember { mutableStateOf(initial?.notifyMinute ?: 0) }
    var warnKmT by remember { mutableStateOf(initial?.warnKm?.takeIf { it > 0 }?.toString() ?: "") }
    var warnDaysT by remember { mutableStateOf(initial?.warnDays?.takeIf { it > 0 }?.toString() ?: "") }
    val hasInterval = posInt(kmT) != null || posInt(moT) != null
    FormDialog(
        title = title, icon = Icons.Default.Category, onDismiss = onDismiss, confirmEnabled = name.isNotBlank() && hasInterval,
        onConfirm = { onSave(name.trim(), type, kmT.toIntOrNull() ?: 0, moT.toIntOrNull() ?: 0, notifyOn, notifyHour, notifyMinute, if (posInt(kmT) != null) warnKmT.toIntOrNull() ?: 0 else 0, if (posInt(moT) != null) warnDaysT.toIntOrNull() ?: 0 else 0) }
    ) {
        AppTextField(name, { name = it }, "Nama kategori", Icons.Default.Edit)
        FormGroupLabel("Berlaku untuk")
        SegmentedChoice(listOf("ALL", "MOTOR", "MOBIL"), type, { vehicleTypeLabel(it) }, { type = it })
        FormGroupLabel("Interval bawaan")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { NumberField(kmT, { kmT = it }, "Tiap (km)", icon = Icons.Default.Speed) }
            Box(Modifier.weight(1f)) { NumberField(moT, { moT = it }, "Tiap (bulan)", icon = Icons.Default.Event) }
        }
        FormNote("Isi salah satu atau keduanya.")
        if (hasInterval) {
            FormGroupLabel("Peringatan \"Segera\"")
            posInt(kmT)?.let { NumberField(warnKmT, { warnKmT = it }, "Segera saat sisa (km)", icon = Icons.Default.Warning, hint = "Kosong = otomatis ${fmtKm((it * 0.2).toInt())} km (20%)") }
            if (posInt(moT) != null) NumberField(warnDaysT, { warnDaysT = it }, "Segera saat sisa (hari)", icon = Icons.Default.Warning, hint = "Kosong = otomatis 30 hari")
            NotifyOptions(notifyOn, notifyHour, notifyMinute) { e, h, m -> notifyOn = e; notifyHour = h; notifyMinute = m }
        }
    }
}
