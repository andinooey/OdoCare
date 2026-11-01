@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.FuelLog
import com.odocare.app.data.ServiceRecord
import com.odocare.app.ui.*
import java.time.temporal.ChronoUnit

private enum class HistorySort(val label: String) {
    NEWEST("Terbaru"), OLDEST("Terlama"), COST("Biaya tertinggi"), KM("KM tertinggi"), NAME("Nama A–Z")
}

/**
 * Tab Histori: riwayat servis kendaraan aktif, bisa difilter tanggal. Ketuk satu riwayat untuk melihat detailnya
 * (lalu ubah atau hapus dari sana). Ikon laporan di kanan atas membuka layar Laporan.
 */
@Composable
fun HistoryScreen(vm: MainViewModel, onOpenReport: () -> Unit, onOpenOdometer: () -> Unit) {
    val records by vm.records.collectAsStateWithLifecycle()
    val fuelLogs by vm.fuelLogs.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val pf = remember { PeriodFilter(Period.ALL) }
    // 0 = Servis, 1 = BBM. Filter tanggal (pf) dipakai bersama oleh kedua daftar.
    var section by remember { mutableStateOf(0) }
    var showAddFuel by remember { mutableStateOf(false) }
    var editFuel by remember { mutableStateOf<FuelLog?>(null) }
    var detailTarget by remember { mutableStateOf<ServiceRecord?>(null) }
    var editTarget by remember { mutableStateOf<ServiceRecord?>(null) }
    var viewImage by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(HistorySort.NEWEST) }
    var sortOpen by remember { mutableStateOf(false) }

    // Filter tanggal + pencarian (nama komponen atau catatan), lalu urutkan
    val q = query.trim()
    val shown = records
        .filter { pf.accepts(it.date) && (q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.notes?.contains(q, ignoreCase = true) == true) }
        .let { list ->
            when (sort) {
                HistorySort.NEWEST -> list.sortedByDescending { it.date }
                HistorySort.OLDEST -> list.sortedBy { it.date }
                HistorySort.COST -> list.sortedByDescending { it.cost }
                HistorySort.KM -> list.sortedByDescending { it.km }
                HistorySort.NAME -> list.sortedBy { it.title.lowercase() }
            }
        }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Histori", vehicle?.name, Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 10.dp)) {
            IconButton(onClick = onOpenOdometer) { Icon(Icons.Default.Speed, "Odometer", tint = TextWhite, modifier = Modifier.size(22.dp)) }
            IconButton(onClick = onOpenReport) { Icon(Icons.Default.BarChart, "Laporan", tint = LavenderPrimary, modifier = Modifier.size(22.dp)) }
        }
        VehicleChips(vm, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
        if (vehicle == null) {
            NoVehicleState(vm, Modifier.padding(top = 32.dp))
        } else {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                listOf("Servis", "BBM").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = section == i, onClick = { section = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = PillSelected, activeContentColor = TextWhite, inactiveContainerColor = Color.Transparent, inactiveContentColor = TextGray),
                        icon = { SegmentedButtonDefaults.Icon(active = section == i) }
                    ) { Text(label, fontSize = 12.sp, maxLines = 1) }
                }
            }
            Box(Modifier.padding(horizontal = 16.dp)) { DateFilterField(pf) }
            if (section == 0 && records.isNotEmpty()) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppTextField(query, { query = it }, "Cari komponen atau catatan", Icons.Default.Search, modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { sortOpen = true }) { Icon(Icons.Default.SwapVert, "Urutkan", tint = if (sort == HistorySort.NEWEST) TextWhite else LavenderPrimary, modifier = Modifier.size(24.dp)) }
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }, modifier = Modifier.background(MenuBg)) {
                            HistorySort.values().forEach { o ->
                                DropdownMenuItem(
                                    text = { Text(o.label, fontSize = 14.sp, color = if (o == sort) LavenderPrimary else TextWhite) },
                                    trailingIcon = if (o == sort) { { Icon(Icons.Default.Check, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) } } else null,
                                    onClick = { sort = o; sortOpen = false }
                                )
                            }
                        }
                    }
                }
            }
            when {
                section == 1 -> FuelHistory(fuelLogs, pf, onAdd = { showAddFuel = true }, onEdit = { editFuel = it }, onDelete = { vm.deleteFuel(it.id) })
                records.isEmpty() -> EmptyState(Icons.Default.History, "Belum ada riwayat servis", "Tap \"Catat servis\" pada komponen di tab Servis. Riwayatnya muncul di sini.", Modifier.padding(top = 48.dp))
                shown.isEmpty() -> EmptyState(
                    Icons.Default.History,
                    if (q.isEmpty()) "Tidak ada servis pada periode ini" else "Tidak ada hasil untuk \"$q\"",
                    if (q.isEmpty()) "Ubah filter tanggal untuk melihat riwayat lain." else "Coba kata lain, atau ubah filter tanggal.",
                    Modifier.padding(top = 48.dp)
                )
                else -> {
                    val total = shown.sumOf { it.cost }
                    Text(
                        "${shown.size} servis" + (if (total > 0) " · ${rupiah(total)}" else "") + " · ${sort.label}",
                        fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)
                    )
                    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                        items(shown, key = { it.id }) { r ->
                            Surface(onClick = { detailTarget = r }, color = SurfaceVariantDark, shape = CardShape) {
                                Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp), verticalAlignment = Alignment.Top) {
                                    Box(Modifier.padding(top = 2.dp).size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) { Icon(Icons.Default.Build, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(r.title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            if (r.cost > 0) Text(rupiah(r.cost), fontSize = 12.sp, color = LavenderPrimary, fontWeight = FontWeight.Medium)
                                        }
                                        Text("${fmtDate(r.date)} · ${fmtKm(r.km)} km", fontSize = 12.sp, color = TextGray)
                                        if (!r.notes.isNullOrBlank()) Text(r.notes, fontSize = 12.sp, color = TextGray, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        if (r.imagePath != null) {
                                            Spacer(Modifier.height(6.dp))
                                            StoredImage(r.imagePath, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).clickable { viewImage = r.imagePath })
                                        }
                                    }
                                    ItemMenu(onEdit = { editTarget = r }, onDelete = { vm.deleteRecord(r.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (vehicle != null && section == 1) Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) { AddFab { showAddFuel = true } }
    }

    if (showAddFuel) FuelFormDialog("Catat BBM", null, km, vehicle?.name, { showAddFuel = false }) { date, kmV, liters, total, full -> vm.addFuel(date, kmV, liters, total, full); showAddFuel = false }
    editFuel?.let { f ->
        FuelFormDialog("Edit BBM", f, km, vehicle?.name, { editFuel = null }) { date, kmV, liters, total, full ->
            vm.updateFuel(f.copy(date = date, km = kmV, liters = liters, totalPrice = total, fullTank = full)); editFuel = null
        }
    }
    detailTarget?.let { r ->
        ServiceDetailDialog(
            r, vehicle?.name, records,
            onImage = { viewImage = it }, onDismiss = { detailTarget = null },
            onEdit = { detailTarget = null; editTarget = r }, onDelete = { detailTarget = null; vm.deleteRecord(r.id) }
        )
    }
    editTarget?.let { r ->
        ServiceFormDialog("Edit Riwayat", r.km, r.date, r.cost, r.notes, r.imagePath, { editTarget = null }, { kmV, date, cost, parts, labor, notes, img ->
            vm.updateRecord(r.copy(km = kmV, date = date, cost = cost, partsCost = parts, laborCost = labor, notes = notes, imagePath = img)); editTarget = null
        }, subtitle = r.title, initialParts = r.partsCost, initialLabor = r.laborCost)
    }
    viewImage?.let { path ->
        AlertDialog(onDismissRequest = { viewImage = null }, containerColor = SurfaceDark, shape = RoundedCornerShape(20.dp), text = { StoredImage(path, Modifier.fillMaxWidth(), maxSide = 1600, contentScale = ContentScale.Fit) },
            confirmButton = { TextButton(onClick = { viewImage = null }) { Text("Tutup") } })
    }
}

/** Satu-satunya filter di Histori: rentang tanggal. Ketuk untuk memilih, tombol X mengembalikan ke semua tanggal. */
@Composable
private fun DateFilterField(f: PeriodFilter) {
    var open by remember { mutableStateOf(false) }
    val s = f.customStart; val e = f.customEnd
    val active = f.isCustom && s != null && e != null
    Surface(onClick = { open = true }, color = SurfaceVariantDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (active) (if (s == e) fmtLocal(s!!) else "${fmtLocal(s!!)} – ${fmtLocal(e!!)}") else "Semua tanggal", Modifier.weight(1f), fontSize = 13.sp, color = if (active) TextWhite else TextGray)
            if (active) IconButton(onClick = { f.period = Period.ALL }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Hapus filter tanggal", tint = TextGray, modifier = Modifier.size(18.dp)) }
            else Icon(Icons.Default.ArrowDropDown, null, tint = TextGray, modifier = Modifier.padding(end = 8.dp).size(22.dp))
        }
    }
    if (open) DateRangePickerDialogX(f.customStart, f.customEnd, { open = false }) { a, b -> f.customStart = a; f.customEnd = b; f.period = Period.CUSTOM; open = false }
}

/**
 * Detail satu riwayat servis, ringkas: hanya data yang ada (biaya, catatan, foto, servis sebelumnya tampil bila terisi).
 * Ubah dan hapus ada di tombol bawah.
 */
@Composable
private fun ServiceDetailDialog(
    r: ServiceRecord, vehicleName: String?, all: List<ServiceRecord>,
    onImage: (String) -> Unit, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    // Servis sebelumnya untuk komponen yang sama (riwayat kendaraan aktif)
    val prev = all.filter { it.id != r.id && it.title == r.title && it.date < r.date }.maxByOrNull { it.date }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceDark, shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(r.title, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (vehicleName != null) Text(vehicleName, color = TextGray, fontSize = 12.sp)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailLine(Icons.Default.Event, fmtDate(r.date))
                DetailLine(Icons.Default.Speed, "${fmtKm(r.km)} km")
                if (r.cost > 0) DetailLine(Icons.Default.Payments, rupiah(r.cost))
                if (r.hasBreakdown) {
                    // Sejajar dengan teks di atasnya (lebar ikon 18dp + jarak 10dp)
                    Text(
                        listOfNotNull(r.partsCost.takeIf { it > 0 }?.let { "Suku cadang ${rupiah(it)}" }, r.laborCost.takeIf { it > 0 }?.let { "Jasa ${rupiah(it)}" }).joinToString(" · "),
                        fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(start = 28.dp)
                    )
                }
                if (prev != null && r.km >= prev.km) {
                    val days = ChronoUnit.DAYS.between(msToLocalDate(prev.date), msToLocalDate(r.date))
                    Text("${fmtKm(r.km - prev.km)} km · $days hari sejak servis sebelumnya", fontSize = 12.sp, color = TextGray)
                }
                if (!r.notes.isNullOrBlank()) {
                    Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(r.notes, fontSize = 12.sp, color = TextWhite, modifier = Modifier.padding(10.dp))
                    }
                }
                if (r.imagePath != null) {
                    StoredImage(r.imagePath, Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp)).clickable { onImage(r.imagePath) }, maxSide = 1000)
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onEdit) { Text("Edit", color = LavenderPrimary) }
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        },
        dismissButton = { TextButton(onClick = onDelete) { Text("Hapus", color = DangerText) } }
    )
}

@Composable
private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = TextWhite)
    }
}

/** Daftar BBM kendaraan aktif, memakai filter tanggal yang sama dengan daftar servis. */
@Composable
private fun FuelHistory(all: List<FuelLog>, pf: PeriodFilter, onAdd: () -> Unit, onEdit: (FuelLog) -> Unit, onDelete: (FuelLog) -> Unit) {
    val shown = all.filter { pf.accepts(it.date) }.sortedWith(compareByDescending<FuelLog> { it.date }.thenByDescending { it.km })
    when {
        all.isEmpty() -> EmptyState(Icons.Default.LocalGasStation, "Belum ada catatan BBM", "Catat tiap isi bensin untuk melihat km per liter dan biaya per km.", Modifier.padding(top = 48.dp), actionLabel = "Catat BBM", onAction = onAdd)
        shown.isEmpty() -> EmptyState(Icons.Default.LocalGasStation, "Tidak ada catatan BBM pada periode ini", "Ubah filter tanggal untuk melihat catatan lain.", Modifier.padding(top = 48.dp))
        else -> {
            Text(
                "${shown.size} pengisian · ${fmtLiter(shown.sumOf { it.liters })} L · ${rupiah(shown.sumOf { it.totalPrice })}",
                fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)
            )
            LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)) {
                items(shown, key = { it.id }) { f ->
                    Surface(color = SurfaceVariantDark, shape = CardShape) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.padding(top = 2.dp).size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalGasStation, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${fmtLiter(f.liters)} L", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(rupiah(f.totalPrice), fontSize = 12.sp, color = LavenderPrimary, fontWeight = FontWeight.Medium)
                                }
                                Text("${fmtDate(f.date)} · ${fmtKm(f.km)} km", fontSize = 12.sp, color = TextGray)
                                Text("${rupiah(f.pricePerLiter)} / L" + if (f.fullTank) " · Tangki penuh" else " · Sebagian", fontSize = 12.sp, color = TextGray)
                            }
                            ItemMenu(onEdit = { onEdit(f) }, onDelete = { onDelete(f) })
                        }
                    }
                }
            }
        }
    }
}
