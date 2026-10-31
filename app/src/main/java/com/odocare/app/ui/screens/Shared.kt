@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.odocare.app.MainViewModel
import com.odocare.app.data.Vehicle
import com.odocare.app.ui.*

/** Form tambah / ubah kendaraan. Jenis dan opsi pembuatan otomatis hanya ada saat menambah. */
@Composable
fun VehicleFormDialog(title: String, initial: Vehicle?, onDismiss: () -> Unit, onSave: (name: String, plate: String, type: String, initialKm: Int, autoSchedules: Boolean, autoDocs: Boolean) -> Unit) {
    val types = VehicleTypes.all
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var plate by remember { mutableStateOf(initial?.plate ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: types.firstOrNull()?.code ?: "MOTOR") }
    var kmT by remember { mutableStateOf(initial?.initialKm?.takeIf { it > 0 }?.toString() ?: "") }
    var autoSchedules by remember { mutableStateOf(true) }
    var autoDocs by remember { mutableStateOf(true) }
    FormDialog(
        title = title, icon = VehicleTypes.icon(type), onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(), onConfirm = { onSave(name, plate, type, kmT.toIntOrNull() ?: 0, initial == null && autoSchedules, initial == null && autoDocs) }
    ) {
        if (initial == null) {
            if (types.size <= 3) SegmentedChoice(types.map { it.code }, type, { VehicleTypes.label(it) }, { type = it }, icon = { VehicleTypes.icon(it) })
            else DropdownField("Jenis kendaraan", VehicleTypes.label(type), VehicleTypes.icon(type), types.map { it.code to it.name }) { type = it }
        }
        AppTextField(name, { name = it }, "Nama kendaraan", Icons.Default.Badge, hint = "Contoh: Honda Vario")
        AppTextField(plate, { plate = it.uppercase() }, "Plat nomor (opsional)", Icons.Default.Pin)
        NumberField(kmT, { kmT = it }, "KM odometer saat ini", icon = Icons.Default.Speed)
        if (initial == null) {
            FormGroupLabel("Dibuat otomatis")
            SwitchRow(Icons.Default.Build, "Jadwal servis", "Komponen bawaan sesuai jenis kendaraan (Master Kategori)", autoSchedules) { autoSchedules = it }
            SwitchRow(Icons.Default.Description, "Pajak & dokumen", "Pajak tahunan dan ganti plat 5 tahunan", autoDocs) { autoDocs = it }
            FormNote("Bila dimatikan, tambahkan sendiri nanti lewat tab Servis (Jadwal & Komponen atau Pajak & Dokumen).")
        }
    }
}

/** Tampilan kosong untuk layar yang butuh kendaraan, lengkap dengan tombol tambah. */
@Composable
fun NoVehicleState(vm: MainViewModel, modifier: Modifier = Modifier, message: String = "Tambahkan kendaraan dulu untuk mulai mencatat servis, odometer, dan pajak.") {
    var show by remember { mutableStateOf(false) }
    EmptyState(Icons.Default.DirectionsCar, "Belum ada kendaraan", message, modifier, actionLabel = "Tambah Kendaraan", onAction = { show = true })
    if (show) VehicleFormDialog("Tambah Kendaraan", null, { show = false }) { n, p, t, k, a, d -> vm.addVehicle(n, p, t, k, a, d); show = false }
}

/**
 * Judul tab Servis berbentuk dropdown: Jadwal & Komponen (default) atau Pajak & Dokumen.
 * Pilihan disimpan di MainViewModel.serviceSection; menekan tab Servis di navigasi bawah mengembalikannya ke Jadwal & Komponen.
 */
@Composable
fun ServiceSectionTitle(vm: MainViewModel, subtitle: String?, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    var open by remember { mutableStateOf(false) }
    val section = vm.serviceSection
    val options = listOf(Triple(0, "Jadwal & Komponen", Icons.Default.Build), Triple(1, "Pajak & Dokumen", Icons.Default.Description))
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            Column(Modifier.clip(RoundedCornerShape(10.dp)).clickable { open = true }.padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(options.first { it.first == section }.second, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Icon(Icons.Default.ArrowDropDown, "Pilih bagian", tint = LavenderPrimary, modifier = Modifier.size(28.dp))
                }
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = TextGray)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(MenuBg)) {
                options.forEach { (index, label, icon) ->
                    val on = index == section
                    DropdownMenuItem(
                        text = { Text(label, fontSize = 14.sp, color = if (on) LavenderPrimary else TextWhite, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal) },
                        leadingIcon = { Icon(icon, null, tint = if (on) LavenderPrimary else TextGray, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (on) { { Icon(Icons.Default.Check, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) } } else null,
                        onClick = { open = false; vm.serviceSection = index }
                    )
                }
            }
        }
        actions()
    }
}
