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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.ServiceRecord
import com.odocare.app.ui.*
import java.time.temporal.ChronoUnit

/**
 * Tab Histori: riwayat servis kendaraan aktif, bisa difilter tanggal. Ketuk satu riwayat untuk melihat detailnya
 * (lalu ubah atau hapus dari sana). Ikon laporan di kanan atas membuka layar Laporan.
 */
@Composable
fun HistoryScreen(vm: MainViewModel, onOpenReport: () -> Unit, onOpenOdometer: () -> Unit) {
    val records by vm.records.collectAsStateWithLifecycle()
    val vehicle by vm.activeVehicle.collectAsStateWithLifecycle()
    val pf = remember { PeriodFilter(Period.ALL) }
    var detailTarget by remember { mutableStateOf<ServiceRecord?>(null) }
    var editTarget by remember { mutableStateOf<ServiceRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<ServiceRecord?>(null) }
    var viewImage by remember { mutableStateOf<String?>(null) }

    val shown = records.filter { pf.accepts(it.date) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        ScreenTitle("Histori", vehicle?.name, Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 10.dp)) {
            IconButton(onClick = onOpenOdometer) { Icon(Icons.Default.Speed, "Odometer", tint = TextWhite, modifier = Modifier.size(22.dp)) }
            IconButton(onClick = onOpenReport) { Icon(Icons.Default.BarChart, "Laporan", tint = LavenderPrimary, modifier = Modifier.size(22.dp)) }
        }
        if (vehicle == null) {
            NoVehicleState(vm, Modifier.padding(top = 32.dp))
        } else {
            Box(Modifier.padding(horizontal = 16.dp)) { DateFilterField(pf) }
            when {
                records.isEmpty() -> EmptyState(Icons.Default.History, "Belum ada riwayat servis", "Tap \"Catat servis\" pada komponen di tab Servis. Riwayatnya muncul di sini.", Modifier.padding(top = 48.dp))
                shown.isEmpty() -> EmptyState(Icons.Default.History, "Tidak ada servis pada periode ini", "Ubah filter tanggal untuk melihat riwayat lain.", Modifier.padding(top = 48.dp))
                else -> {
                    val total = shown.sumOf { it.cost }
                    Text(
                        "${shown.size} servis" + if (total > 0) " · ${rupiah(total)}" else "",
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
                                        Text("${fmtDate(r.date)} · ${fmtKm(r.km)} km", fontSize = 11.sp, color = TextGray)
                                        if (!r.notes.isNullOrBlank()) Text(r.notes, fontSize = 11.sp, color = TextGray, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
        }
    }

    detailTarget?.let { r ->
        ServiceDetailDialog(
            r, vehicle?.name, records,
            onImage = { viewImage = it }, onDismiss = { detailTarget = null },
            onEdit = { detailTarget = null; editTarget = r }, onDelete = { detailTarget = null; deleteTarget = r }
        )
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
                if (prev != null && r.km >= prev.km) {
                    val days = ChronoUnit.DAYS.between(msToLocalDate(prev.date), msToLocalDate(r.date))
                    Text("${fmtKm(r.km - prev.km)} km · $days hari sejak servis sebelumnya", fontSize = 11.sp, color = TextGray)
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
