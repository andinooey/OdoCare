@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.collectAsState
import com.odocare.app.MainViewModel
import com.odocare.app.data.FuelLog
import com.odocare.app.data.ServiceRecord
import com.odocare.app.ui.*
import java.time.LocalDate
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
    val records by vm.records.collectAsState()
    val fuelLogs by vm.fuelLogs.collectAsState()
    val vehicle by vm.activeVehicle.collectAsState()
    val km by vm.currentKm.collectAsState()
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
    // Jendela riwayat: tampil 30 hari terakhir dulu; tombol "Muat 30 hari sebelumnya" memperlebarnya 30 hari lagi (data tidak hilang)
    var loadedWindows by remember { mutableStateOf(1) }
    val windowDays = 30 * loadedWindows
    val windowStart = remember(windowDays) { LocalDate.now().minusDays(windowDays - 1L) }
    fun inWindow(ms: Long) = !msToLocalDate(ms).isBefore(windowStart)
    val hasOlderService = records.any { !inWindow(it.date) }

    // Filter tanggal + pencarian (nama komponen atau catatan), lalu urutkan
    val q = query.trim()
    val shown = records
        .filter { inWindow(it.date) && pf.accepts(it.date) && (q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.notes?.contains(q, ignoreCase = true) == true) }
        .let { list ->
            when (sort) {
                // Hari yang sama: KM tertinggi di atas (Terbaru). Terlama dibalik supaya urutan kronologisnya konsisten.
                HistorySort.NEWEST -> list.sortedWith(compareByDescending<ServiceRecord> { msToLocalDate(it.date) }.thenByDescending { it.km })
                HistorySort.OLDEST -> list.sortedWith(compareBy<ServiceRecord> { msToLocalDate(it.date) }.thenBy { it.km })
                HistorySort.COST -> list.sortedByDescending { it.cost }
                HistorySort.KM -> list.sortedByDescending { it.km }
                HistorySort.NAME -> list.sortedBy { it.title.lowercase() }
            }
        }

    // Dari catat servis di Home: scroll ke record baru, baru setelah scroll selesai kartunya berkedip.
    // Bila record tidak tampil (filter periode / pencarian / di luar jendela 30 hari), scroll dibatalkan dan hanya centang yang muncul.
    val listState = rememberLazyListState()
    val focusId = remember { vm.pendingHistoryFocus.also { vm.pendingHistoryFocus = null } }
    val grouped = sort == HistorySort.NEWEST || sort == HistorySort.OLDEST
    var focusHandled by remember { mutableStateOf(false) }
    LaunchedEffect(focusId, records, shown) {
        val id = focusId ?: return@LaunchedEffect
        if (focusHandled) return@LaunchedEffect
        if (records.none { it.id == id }) return@LaunchedEffect   // record belum sampai dari database; efek jalan lagi saat daftar berubah
        val index = listIndexOf(shown, id, grouped)
        if (index == null) { focusHandled = true; return@LaunchedEffect }   // tersaring: batal scroll, hanya centang
        withFrameNanos { }                      // tunggu LazyColumn terpasang
        listState.animateScrollToItem(index)    // suspend sampai scroll selesai
        focusHandled = true
        vm.flash(id)                            // kedipan baru mulai setelah scroll selesai
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Histori", vehicle?.name, Modifier.padding(start = Spacing.L, end = Spacing.S, top = Spacing.L, bottom = Spacing.M)) {
            IconButton(onClick = onOpenOdometer) { Icon(Icons.Default.Speed, "Odometer", tint = TextWhite, modifier = Modifier.size(22.dp)) }
            IconButton(onClick = onOpenReport) { Icon(Icons.Default.BarChart, "Laporan", tint = LavenderPrimary, modifier = Modifier.size(22.dp)) }
        }
        VehicleChips(vm, Modifier.padding(start = Spacing.L, end = Spacing.L, bottom = Spacing.S))
        if (vehicle == null) {
            NoVehicleState(vm, Modifier.padding(top = Spacing.XL))
        } else {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = Spacing.L, end = Spacing.L, bottom = Spacing.S)) {
                listOf("Servis", "BBM").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = section == i, onClick = { section = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = PillSelected, activeContentColor = TextWhite, inactiveContainerColor = Color.Transparent, inactiveContentColor = TextGray),
                        icon = { SegmentedButtonDefaults.Icon(active = section == i) }
                    ) { Text(label, fontSize = 12.sp, maxLines = 1) }
                }
            }
            Box(Modifier.padding(horizontal = Spacing.L)) { DateFilterField(pf, windowStart, "$windowDays hari terakhir") }
            if (section == 0 && records.isNotEmpty()) {
                Row(Modifier.padding(start = Spacing.L, end = Spacing.S, top = Spacing.S), verticalAlignment = Alignment.CenterVertically) {
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
                section == 1 -> FuelHistory(fuelLogs, pf, windowStart, vm.flashId, onLoadOlder = { loadedWindows += 1 }, onAdd = { showAddFuel = true }, onEdit = { editFuel = it }, onDelete = { vm.deleteFuel(it.id) })
                records.isEmpty() -> EmptyState(Icons.Default.History, "Belum ada riwayat servis", "Tap \"Catat servis\" pada komponen di tab Servis. Riwayatnya muncul di sini.", Modifier.padding(top = Spacing.XL))
                shown.isEmpty() -> Column {
                    EmptyState(
                        Icons.Default.History,
                        if (q.isEmpty()) "Tidak ada servis pada periode ini" else "Tidak ada hasil untuk \"$q\"",
                        if (q.isEmpty()) "Ubah filter tanggal untuk melihat riwayat lain." else "Coba kata lain, atau ubah filter tanggal.",
                        Modifier.padding(top = Spacing.XL)
                    )
                    if (hasOlderService) Box(Modifier.padding(horizontal = Spacing.L)) { LoadOlderButton { loadedWindows += 1 } }
                }
                else -> {
                    val total = shown.sumOf { it.cost }
                    Text(
                        "${shown.size} servis" + (if (total > 0) " · ${rupiah(total)}" else "") + " · ${sort.label}",
                        fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(start = Spacing.L, end = Spacing.L, top = Spacing.M)
                    )
                    // Urutan Terbaru / Terlama: dikelompokkan per tanggal seperti mutasi bank. Urutan lain: daftar biasa (tanggal tampil di kartu).
                    val card: @Composable (ServiceRecord) -> Unit = { r ->
                        ServiceCard(r, showDate = !grouped, flash = vm.flashId == r.id, onClick = { detailTarget = r }, onEdit = { editTarget = r }, onDelete = { vm.deleteRecord(r.id) })
                    }
                    LazyColumn(Modifier.padding(horizontal = Spacing.L), state = listState, verticalArrangement = Arrangement.spacedBy(Spacing.S), contentPadding = PaddingValues(top = Spacing.XS, bottom = Spacing.XL)) {
                        if (grouped) dayGroups(shown, { it.date }, { it.id }, card)
                        else items(shown, key = { it.id }) { card(it) }
                        if (hasOlderService) item(key = "load_older") { LoadOlderButton { loadedWindows += 1 } }
                    }
                }
            }
        }
    }
    if (vehicle != null && section == 1) Box(Modifier.align(Alignment.BottomEnd).padding(Spacing.L)) { AddFab { showAddFuel = true } }
    }

    if (showAddFuel) FuelFormDialog("Catat BBM", null, km, vehicle?.name, vehicle?.fuelPricePerLiter ?: 0L, { showAddFuel = false }) { date, kmV, liters, total, full, lp -> vm.addFuel(date, kmV, liters, total, full, lp); showAddFuel = false }
    editFuel?.let { f ->
        FuelFormDialog("Edit BBM", f, km, vehicle?.name, vehicle?.fuelPricePerLiter ?: 0L, { editFuel = null }) { date, kmV, liters, total, full, lp ->
            vm.updateFuel(f.copy(date = date, km = kmV, liters = liters, totalPrice = total, fullTank = full, literPrice = lp)); editFuel = null
        }
    }
    detailTarget?.let { r ->
        ServiceDetailDialog(
            r, vehicle?.name, records,
            onImage = { viewImage = it }, onDismiss = { detailTarget = null }
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

/** Satu-satunya filter di Histori: rentang tanggal di dalam jendela yang sedang dimuat. Tombol X mengembalikan ke seluruh jendela. */
@Composable
private fun DateFilterField(f: PeriodFilter, minDate: LocalDate, emptyLabel: String) {
    var open by remember { mutableStateOf(false) }
    val s = f.customStart; val e = f.customEnd
    val active = f.isCustom && s != null && e != null
    Surface(onClick = { open = true }, color = SurfaceVariantDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = Spacing.M, top = Spacing.XS, bottom = Spacing.XS, end = Spacing.XS).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.M))
            Text(if (active) (if (s == e) fmtLocal(s!!) else "${fmtLocal(s!!)} – ${fmtLocal(e!!)}") else emptyLabel, Modifier.weight(1f), fontSize = 14.sp, color = if (active) TextWhite else TextGray)
            if (active) IconButton(onClick = { f.period = Period.ALL }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Hapus filter tanggal", tint = TextGray, modifier = Modifier.size(18.dp)) }
            else Icon(Icons.Default.ArrowDropDown, null, tint = TextGray, modifier = Modifier.padding(end = Spacing.S).size(22.dp))
        }
    }
    if (open) DateRangePickerDialogX(f.customStart, f.customEnd, { open = false }, minDate = minDate) { a, b -> f.customStart = a; f.customEnd = b; f.period = Period.CUSTOM; open = false }
}

/**
 * Detail satu riwayat servis: halaman penuh bergaya form, hanya baca. Hanya ada tombol Tutup;
 * Edit dan Hapus tetap lewat menu titik tiga di daftar.
 */
@Composable
private fun ServiceDetailDialog(
    r: ServiceRecord, vehicleName: String?, all: List<ServiceRecord>,
    onImage: (String) -> Unit, onDismiss: () -> Unit
) {
    // Servis sebelumnya untuk komponen yang sama (riwayat kendaraan aktif)
    val prev = all.filter { it.id != r.id && it.title == r.title && it.date < r.date }.maxByOrNull { it.date }
    DetailDialog(title = "Detail Servis", subtitle = r.title, icon = Icons.Default.Build, onDismiss = onDismiss) {
        FormGroupLabel("Informasi")
        ReadOnlyField("Komponen", r.title, Icons.Default.Build, emphasize = true)
        if (vehicleName != null) ReadOnlyField("Kendaraan", vehicleName, Icons.Default.DirectionsCar)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
            Box(Modifier.weight(1f)) { ReadOnlyField("Tanggal", fmtDate(r.date), Icons.Default.Event) }
            Box(Modifier.weight(1f)) { ReadOnlyField("Odometer", "${fmtKm(r.km)} km", Icons.Default.Speed) }
        }
        if (prev != null && r.km >= prev.km) {
            val days = ChronoUnit.DAYS.between(msToLocalDate(prev.date), msToLocalDate(r.date))
            ReadOnlyField("Sejak servis sebelumnya", "${fmtKm(r.km - prev.km)} km · $days hari", Icons.Default.History)
        }
        FormGroupLabel("Biaya")
        if (r.cost > 0) {
            ReadOnlyField("Total biaya", rupiah(r.cost), Icons.Default.Payments, valueColor = LavenderPrimary, emphasize = true)
            if (r.hasBreakdown) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
                    Box(Modifier.weight(1f)) { ReadOnlyField("Suku cadang", rupiah(r.partsCost), Icons.Default.Settings) }
                    Box(Modifier.weight(1f)) { ReadOnlyField("Jasa", rupiah(r.laborCost), Icons.Default.Handyman) }
                }
            }
        } else {
            ReadOnlyField("Total biaya", "Tidak dicatat", Icons.Default.Payments, valueColor = TextGray)
        }
        if (!r.notes.isNullOrBlank()) {
            FormGroupLabel("Catatan")
            ReadOnlyField("Catatan", r.notes, Icons.Default.Notes)
        }
        if (r.imagePath != null) {
            FormGroupLabel("Foto bukti")
            StoredImage(r.imagePath, Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp)).clickable { onImage(r.imagePath) }, maxSide = 1000)
        }
    }
}

/** Daftar BBM kendaraan aktif, memakai filter tanggal yang sama dengan daftar servis. */
@Composable
private fun FuelHistory(all: List<FuelLog>, pf: PeriodFilter, windowStart: LocalDate, flashId: String?, onLoadOlder: () -> Unit, onAdd: () -> Unit, onEdit: (FuelLog) -> Unit, onDelete: (FuelLog) -> Unit) {
    fun inWindow(ms: Long) = !msToLocalDate(ms).isBefore(windowStart)
    val hasOlder = all.any { !inWindow(it.date) }
    val shown = all.filter { inWindow(it.date) && pf.accepts(it.date) }.sortedWith(compareByDescending<FuelLog> { it.date }.thenByDescending { it.km })
    when {
        all.isEmpty() -> EmptyState(Icons.Default.LocalGasStation, "Belum ada catatan BBM", "Catat tiap isi bensin untuk melihat km per liter dan biaya per km.", Modifier.padding(top = Spacing.XL), actionLabel = "Catat BBM", onAction = onAdd)
        shown.isEmpty() -> Column {
            EmptyState(Icons.Default.LocalGasStation, "Tidak ada catatan BBM pada periode ini", "Ubah filter tanggal untuk melihat catatan lain.", Modifier.padding(top = Spacing.XL))
            if (hasOlder) Box(Modifier.padding(horizontal = Spacing.L)) { LoadOlderButton(onLoadOlder) }
        }
        else -> {
            Text(
                "${shown.size} pengisian · ${fmtLiter(shown.sumOf { it.liters })} L · ${rupiah(shown.sumOf { it.totalPrice })}",
                fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(start = Spacing.L, end = Spacing.L, top = Spacing.M)
            )
            LazyColumn(Modifier.padding(horizontal = Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.S), contentPadding = PaddingValues(top = Spacing.XS, bottom = 88.dp)) {
                dayGroups(shown, { it.date }, { it.id }) { f -> FuelCard(f, flash = flashId == f.id, onEdit = { onEdit(f) }, onDelete = { onDelete(f) }) }
                if (hasOlder) item(key = "load_older") { LoadOlderButton(onLoadOlder) }
            }
        }
    }
}

/** Susun daftar per tanggal: header tanggal, lalu kartu transaksi hari itu. [list] harus sudah terurut. */
private fun <T> LazyListScope.dayGroups(list: List<T>, dateOf: (T) -> Long, key: (T) -> String, card: @Composable (T) -> Unit) {
    list.groupBy { msToLocalDate(dateOf(it)) }.forEach { (day, rows) ->
        item(key = "day_$day") { DayHeader(dateOf(rows.first())) }
        items(rows, key = key) { card(it) }
    }
}

/** Posisi item [id] di dalam LazyColumn daftar servis (memperhitungkan header tanggal bila [grouped]). null bila tidak ada di [shown]. */
private fun listIndexOf(shown: List<ServiceRecord>, id: String, grouped: Boolean): Int? {
    if (!grouped) return shown.indexOfFirst { it.id == id }.takeIf { it >= 0 }
    var index = 0
    shown.groupBy { msToLocalDate(it.date) }.forEach { (_, rows) ->
        index++                                            // header tanggal
        rows.forEach { if (it.id == id) return index; index++ }
    }
    return null
}

/** Header tanggal (tanpa total harian). */
@Composable
private fun DayHeader(ms: Long) {
    Text(
        fmtDayHeader(ms), style = AppType.Caption.copy(fontWeight = FontWeight.SemiBold), color = TextGray,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.M, bottom = Spacing.XS)
    )
}

/** Tombol memperlebar jendela riwayat 30 hari ke belakang. */
@Composable
private fun LoadOlderButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = Spacing.M), shape = RoundedCornerShape(12.dp)) {
        Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.S))
        Text("Muat 30 hari sebelumnya", fontSize = 14.sp)
    }
}

/**
 * Kartu transaksi servis: judul + biaya, lalu tanggal dan KM, rincian biaya (bila ada), catatan satu baris, dan penanda foto.
 * Fotonya sendiri hanya tampil di detail servis. showDate = true hanya saat daftar tidak dikelompokkan per tanggal.
 */
@Composable
private fun ServiceCard(r: ServiceRecord, showDate: Boolean, flash: Boolean, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(onClick = onClick, color = flashingColor(SurfaceVariantDark, flash), shape = CardShape, border = CardBorder) {
        Row(Modifier.fillMaxWidth().padding(start = Spacing.M, top = Spacing.S, bottom = Spacing.S, end = Spacing.XS), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = Spacing.XS).size(32.dp).clip(RoundedCornerShape(8.dp)).background(PillSelected), contentAlignment = Alignment.Center) { Icon(Icons.Default.Build, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(Spacing.M))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.title, color = TextWhite, style = AppType.CardTitle, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (r.cost > 0) {
                        Spacer(Modifier.width(Spacing.S))
                        Text(rupiah(r.cost), style = AppType.Value, color = LavenderPrimary)
                    }
                }
                Text((if (showDate) "${fmtDate(r.date)} · " else "") + "${fmtKm(r.km)} km", style = AppType.Caption, color = TextGray)
                if (r.hasBreakdown) Text("Suku cadang ${rupiah(r.partsCost)} · Jasa ${rupiah(r.laborCost)}", style = AppType.Caption, color = TextGray)
                if (!r.notes.isNullOrBlank()) Text(r.notes, style = AppType.Caption, color = TextGray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (r.imagePath != null) {
                    Spacer(Modifier.height(Spacing.XS))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, tint = LavenderPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(Spacing.XS))
                        Text("Foto terlampir", style = AppType.Caption, color = LavenderPrimary)
                    }
                }
            }
            ItemMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

/** Kartu transaksi BBM: liter dan harga per liter saat transaksi, KM, status tangki. */
@Composable
private fun FuelCard(f: FuelLog, flash: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = flashingColor(SurfaceVariantDark, flash), shape = CardShape, border = CardBorder) {
        Row(Modifier.fillMaxWidth().padding(start = Spacing.M, top = Spacing.S, bottom = Spacing.S, end = Spacing.XS), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = Spacing.XS).size(32.dp).clip(RoundedCornerShape(8.dp)).background(PillSelected), contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalGasStation, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(Spacing.M))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Isi BBM", color = TextWhite, style = AppType.CardTitle, modifier = Modifier.weight(1f))
                    Text(rupiah(f.totalPrice), style = AppType.Value, color = LavenderPrimary)
                }
                Text("${fmtLiter(f.liters)} L · ${rupiah(f.pricePerLiter)}/L", style = AppType.Caption, color = TextGray)
                Text("${fmtKm(f.km)} km" + if (f.fullTank) " · Tangki penuh" else " · Sebagian", style = AppType.Caption, color = TextGray)
            }
            ItemMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}
