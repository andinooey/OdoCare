@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.collectAsState
import com.odocare.app.MainViewModel
import com.odocare.app.data.VehicleType
import com.odocare.app.ui.*

/** Master jenis kendaraan: Motor, Mobil, dan jenis lain yang ditambahkan sendiri (truk, sepeda listrik, dst). */
@Composable
fun VehicleTypeScreen(vm: MainViewModel, onBack: () -> Unit) {
    val types by vm.vehicleTypes.collectAsState()
    val vehicles by vm.vehicles.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<VehicleType?>(null) }
    var deleteTarget by remember { mutableStateOf<VehicleType?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Master Jenis Kendaraan", onBack) },
        floatingActionButton = { AddFab { showAdd = true } }
    ) { pad ->
        if (types.isEmpty()) {
            EmptyState(Icons.Default.DirectionsCar, "Belum ada jenis kendaraan", "Tambahkan jenis seperti motor, mobil, truk, atau sepeda listrik.", Modifier.padding(pad).padding(top = Spacing.XL), actionLabel = "Tambah Jenis", onAction = { showAdd = true })
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.S), contentPadding = PaddingValues(top = Spacing.XS, bottom = 88.dp)) {
                item { Text("Jenis kendaraan dipakai saat menambah kendaraan dan di Master Kategori. Jenis yang masih dipakai kendaraan tidak bisa dihapus.", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = Spacing.XS)) }
                items(types, key = { it.code }) { t ->
                    val used = vehicles.count { it.type == t.code }
                    Surface(color = SurfaceVariantDark, shape = CardShape, border = CardBorder) {
                        Row(Modifier.fillMaxWidth().padding(start = Spacing.M, top = Spacing.S, bottom = Spacing.S, end = Spacing.XS), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) {
                                Icon(VehicleTypes.icon(t.code), null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(Spacing.M))
                            Column(Modifier.weight(1f)) {
                                Text(t.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(if (used > 0) "$used kendaraan" else "Belum dipakai", fontSize = 12.sp, color = TextGray)
                            }
                            ItemMenu(onEdit = { editTarget = t }, onDelete = {
                                if (used > 0) message = "\"${t.name}\" masih dipakai $used kendaraan. Ubah atau hapus kendaraannya dulu."
                                else deleteTarget = t
                            })
                        }
                    }
                }
            }
        }
    }

    if (showAdd) VehicleTypeFormDialog("Tambah Jenis", null, { showAdd = false }) { n, i -> vm.addVehicleType(n, i); showAdd = false }
    editTarget?.let { t -> VehicleTypeFormDialog("Edit Jenis", t, { editTarget = null }) { n, i -> vm.updateVehicleType(t.copy(name = n.trim(), icon = i)); editTarget = null } }
    deleteTarget?.let { t ->
        ConfirmDialog("Hapus jenis kendaraan?", "\"${t.name}\" dihapus beserta master kategori khusus jenis ini.", "Hapus", {
            vm.deleteVehicleType(t.code) { ok -> if (!ok) message = "\"${t.name}\" masih dipakai kendaraan, tidak dihapus." }
            deleteTarget = null
        }, { deleteTarget = null })
    }
    message?.let { InfoDialog(it) { message = null } }
}

@Composable
private fun VehicleTypeFormDialog(title: String, initial: VehicleType?, onDismiss: () -> Unit, onSave: (name: String, icon: String) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: vehicleIconOptions.first().first) }
    FormDialog(
        title = title, icon = vehicleIconOptions.firstOrNull { it.first == icon }?.second ?: Icons.Default.DirectionsCar, onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(), onConfirm = { onSave(name.trim(), icon) }
    ) {
        AppTextField(name, { name = it }, "Nama jenis", Icons.Default.Edit, hint = "Contoh: Truk, Sepeda Listrik")
        FormGroupLabel("Ikon")
        vehicleIconOptions.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                row.forEach { (key, vec) ->
                    val on = key == icon
                    Surface(
                        onClick = { icon = key }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp),
                        color = if (on) PillSelected else androidx.compose.ui.graphics.Color.Transparent,
                        border = BorderStroke(1.dp, if (on) LavenderPrimary else FieldBorder)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(vec, null, tint = if (on) LavenderPrimary else TextGray, modifier = Modifier.size(22.dp)) }
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
