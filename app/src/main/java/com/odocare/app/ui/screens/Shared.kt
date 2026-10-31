@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.odocare.app.MainViewModel
import com.odocare.app.data.Vehicle
import com.odocare.app.ui.*

/** Form tambah / ubah kendaraan. Jenis hanya bisa dipilih saat menambah. */
@Composable
fun VehicleFormDialog(title: String, initial: Vehicle?, onDismiss: () -> Unit, onSave: (name: String, plate: String, type: String, initialKm: Int) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var plate by remember { mutableStateOf(initial?.plate ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "MOTOR") }
    var kmT by remember { mutableStateOf(initial?.initialKm?.takeIf { it > 0 }?.toString() ?: "") }
    FormDialog(
        title = title, icon = if (type == "MOBIL") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler, onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(), onConfirm = { onSave(name, plate, type, kmT.toIntOrNull() ?: 0) }
    ) {
        if (initial == null) SegmentedChoice(
            listOf("MOTOR", "MOBIL"), type, { vehicleTypeLabel(it) }, { type = it },
            icon = { if (it == "MOBIL") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler }
        )
        AppTextField(name, { name = it }, "Nama kendaraan", Icons.Default.Badge, hint = "Contoh: Honda Vario")
        AppTextField(plate, { plate = it.uppercase() }, "Plat nomor (opsional)", Icons.Default.Pin)
        NumberField(kmT, { kmT = it }, "KM odometer saat ini", icon = Icons.Default.Speed)
        if (initial == null) FormNote("Jadwal servis dan dokumen bawaan dibuat otomatis sesuai jenis kendaraan.")
    }
}

/** Tampilan kosong untuk layar yang butuh kendaraan, lengkap dengan tombol tambah. */
@Composable
fun NoVehicleState(vm: MainViewModel, modifier: Modifier = Modifier, message: String = "Tambahkan kendaraan dulu untuk mulai mencatat servis, odometer, dan pajak.") {
    var show by remember { mutableStateOf(false) }
    EmptyState(Icons.Default.DirectionsCar, "Belum ada kendaraan", message, modifier, actionLabel = "Tambah Kendaraan", onAction = { show = true })
    if (show) VehicleFormDialog("Tambah Kendaraan", null, { show = false }) { n, p, t, k -> vm.addVehicle(n, p, t, k); show = false }
}
