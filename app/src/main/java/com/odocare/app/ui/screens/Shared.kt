@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odocare.app.MainViewModel
import com.odocare.app.data.Vehicle
import com.odocare.app.ui.*

/** Form tambah / ubah kendaraan. Jenis dan opsi pembuatan otomatis hanya ada saat menambah. */
@Composable
fun VehicleFormDialog(title: String, initial: Vehicle?, onDismiss: () -> Unit, onSave: (name: String, plate: String, type: String, initialKm: Int, autoSchedules: Boolean, autoDocs: Boolean, fuelPrice: Long) -> Unit) {
    val types = VehicleTypes.all
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var plate by remember { mutableStateOf(initial?.plate ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: types.firstOrNull()?.code ?: "MOTOR") }
    var kmT by remember { mutableStateOf(initial?.initialKm?.takeIf { it > 0 }?.toString() ?: "") }
    var fuelT by remember { mutableStateOf(initial?.fuelPricePerLiter?.takeIf { it > 0 }?.toString() ?: "") }
    var autoSchedules by remember { mutableStateOf(true) }
    var autoDocs by remember { mutableStateOf(true) }
    FormDialog(
        title = title, icon = VehicleTypes.icon(type), onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(), onConfirm = { onSave(name, plate, type, kmT.toIntOrNull() ?: 0, initial == null && autoSchedules, initial == null && autoDocs, fuelT.toLongOrNull() ?: 0L) }
    ) {
        if (initial == null) {
            if (types.size <= 3) SegmentedChoice(types.map { it.code }, type, { VehicleTypes.label(it) }, { type = it }, icon = { VehicleTypes.icon(it) })
            else DropdownField("Jenis kendaraan", VehicleTypes.label(type), VehicleTypes.icon(type), types.map { it.code to it.name }) { type = it }
        }
        AppTextField(name, { name = it }, "Nama kendaraan", Icons.Default.Badge, hint = "Contoh: Honda Vario")
        AppTextField(plate, { plate = it.uppercase() }, "Plat nomor (opsional)", Icons.Default.Pin)
        NumberField(kmT, { kmT = it }, "KM odometer saat ini", icon = Icons.Default.Speed)
        NumberField(fuelT, { fuelT = it }, "Harga BBM per liter (opsional)", currency = true, icon = Icons.Default.LocalGasStation, hint = "Dipakai menghitung liter otomatis saat catat BBM. Ubah kapan saja bila harga naik atau turun.")
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
    if (show) VehicleFormDialog("Tambah Kendaraan", null, { show = false }) { n, p, t, k, a, d, f -> vm.addVehicle(n, p, t, k, a, d, f); show = false }
}

/** Pemilih kendaraan aktif (chip). Hanya tampil bila punya lebih dari satu kendaraan; dipakai seragam di Home, Servis, dan Histori. */
@Composable
fun VehicleChips(vm: MainViewModel, modifier: Modifier = Modifier) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val active by vm.activeVehicle.collectAsStateWithLifecycle()
    if (vehicles.size > 1) {
        Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vehicles.forEach { item -> FilterChip(selected = item.id == active?.id, onClick = { vm.setActiveVehicle(item.id) }, label = { Text(item.name, fontSize = 12.sp) }) }
        }
    }
}

/**
 * Kepala tab Servis: judul, pemilih kendaraan, dan tombol segmen Jadwal & Komponen / Pajak & Dokumen.
 * Pilihan disimpan di MainViewModel.serviceSection; menekan tab Servis di navigasi bawah mengembalikannya ke Jadwal & Komponen.
 */
@Composable
fun ServiceSectionTitle(vm: MainViewModel, subtitle: String?, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val section = vm.serviceSection
    Column {
        ScreenTitle("Servis", subtitle, modifier, actions)
        VehicleChips(vm, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            listOf("Jadwal & Komponen", "Pajak & Dokumen").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = section == i, onClick = { vm.serviceSection = i },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = PillSelected, activeContentColor = TextPrimary, inactiveContainerColor = Color.Transparent, inactiveContentColor = TextMuted),
                    icon = { SegmentedButtonDefaults.Icon(active = section == i) }
                ) { Text(label, fontSize = 12.sp, maxLines = 1) }
            }
        }
    }
}
