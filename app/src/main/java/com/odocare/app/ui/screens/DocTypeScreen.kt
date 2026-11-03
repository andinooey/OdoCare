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
import androidx.compose.runtime.collectAsState
import com.odocare.app.MainViewModel
import com.odocare.app.data.DocumentType
import com.odocare.app.domain.IntervalUnit
import com.odocare.app.ui.*

/** Master jenis dokumen: Pajak Tahunan, SIM, Asuransi, dan jenis lain yang ditambahkan sendiri. Dipakai di dropdown form Pajak & Dokumen. */
@Composable
fun DocTypeScreen(vm: MainViewModel, onBack: () -> Unit) {
    val types by vm.documentTypes.collectAsState()
    val docs by vm.allDocuments.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<DocumentType?>(null) }
    var deleteTarget by remember { mutableStateOf<DocumentType?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = { BackTopBar("Master Jenis Dokumen", onBack) },
        floatingActionButton = { AddFab { showAdd = true } }
    ) { pad ->
        if (types.isEmpty()) {
            EmptyState(Icons.Default.Description, "Belum ada jenis dokumen", "Tambahkan jenis seperti pajak, STNK, SIM, atau asuransi.", Modifier.padding(pad).padding(top = Spacing.XL), actionLabel = "Tambah Jenis", onAction = { showAdd = true })
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.S), contentPadding = PaddingValues(top = Spacing.XS, bottom = 88.dp)) {
                item { Text("Daftar ini muncul di pilihan \"Jenis dokumen\" saat menambah dokumen. Jenis yang masih dipakai dokumen tidak bisa dihapus.", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = Spacing.XS)) }
                items(types, key = { it.code }) { t ->
                    val used = docs.count { it.type == t.code }
                    Surface(color = SurfaceVariantDark, shape = CardShape, border = CardBorder) {
                        Row(Modifier.fillMaxWidth().padding(start = Spacing.M, top = Spacing.S, bottom = Spacing.S, end = Spacing.XS), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) {
                                Icon(if (t.vehicleType == "ALL") Icons.Default.Description else VehicleTypes.icon(t.vehicleType), null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(Spacing.M))
                            Column(Modifier.weight(1f)) {
                                Text(t.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                val parts = mutableListOf(vehicleTypeLabel(t.vehicleType))
                                if (t.defaultValue > 0) parts += "berlaku ${t.defaultValue} ${(IntervalUnit.parse(t.defaultUnit) ?: IntervalUnit.YEAR).label.lowercase()}"
                                parts += if (used > 0) "$used dokumen" else "Belum dipakai"
                                Text(parts.joinToString(" · "), fontSize = 12.sp, color = TextGray)
                            }
                            ItemMenu(onEdit = { editTarget = t }, onDelete = {
                                if (used > 0) message = "\"${t.name}\" masih dipakai $used dokumen. Ubah atau hapus dokumennya dulu."
                                else deleteTarget = t
                            })
                        }
                    }
                }
            }
        }
    }

    if (showAdd) DocTypeFormDialog("Tambah Jenis Dokumen", null, { showAdd = false }) { n, vt, v, u -> vm.addDocType(n, vt, v, u); showAdd = false }
    editTarget?.let { t -> DocTypeFormDialog("Edit Jenis Dokumen", t, { editTarget = null }) { n, vt, v, u -> vm.updateDocType(t.copy(name = n.trim(), vehicleType = vt, defaultValue = v, defaultUnit = u)); editTarget = null } }
    deleteTarget?.let { t ->
        ConfirmDialog("Hapus jenis dokumen?", "\"${t.name}\" dihapus dari pilihan jenis dokumen.", "Hapus", {
            vm.deleteDocType(t.code) { ok -> if (!ok) message = "\"${t.name}\" masih dipakai dokumen, tidak dihapus." }
            deleteTarget = null
        }, { deleteTarget = null })
    }
    message?.let { InfoDialog(it) { message = null } }
}

@Composable
private fun DocTypeFormDialog(title: String, initial: DocumentType?, onDismiss: () -> Unit, onSave: (name: String, vehicleType: String, value: Int, unit: String) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var vehicleType by remember { mutableStateOf(initial?.vehicleType ?: "ALL") }
    var valueText by remember { mutableStateOf(initial?.defaultValue?.takeIf { it > 0 }?.toString() ?: "") }
    var unit by remember { mutableStateOf(IntervalUnit.parse(initial?.defaultUnit) ?: IntervalUnit.YEAR) }
    FormDialog(
        title = title, icon = Icons.Default.Description, onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(), onConfirm = { onSave(name.trim(), vehicleType, valueText.toIntOrNull() ?: 0, unit.name) }
    ) {
        AppTextField(name, { name = it }, "Nama jenis dokumen", Icons.Default.Edit, hint = "Contoh: Pajak Tahunan, KIR")
        FormGroupLabel("Berlaku untuk")
        DropdownField(
            "Jenis kendaraan", VehicleTypes.label(vehicleType), if (vehicleType == "ALL") Icons.Default.Description else VehicleTypes.icon(vehicleType),
            listOf("ALL" to "Semua") + VehicleTypes.all.map { it.code to it.name }
        ) { vehicleType = it }
        FormNote("Jenis ini hanya muncul untuk kendaraan dengan jenis yang dipilih.")
        FormGroupLabel("Masa berlaku bawaan (opsional)")
        NumberField(valueText, { valueText = it }, "Berlaku selama", icon = Icons.Default.Update, hint = "Kosong = isi sendiri saat menambah dokumen")
        UnitChips(unit) { unit = it }
    }
}
