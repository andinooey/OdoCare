package com.odocare.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.odocare.app.data.*
import com.odocare.app.domain.*
import com.odocare.app.notif.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.odocare.app.ui.*
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

object ThemeState { var mode by mutableStateOf("Gelap") }

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)
        ThemeState.mode = Prefs.getStr(this, "theme", "Gelap")
        handle(intent)
        setContent {
            OdoCareTheme(ThemeState.mode) { App(vm) }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }
    private fun handle(i: Intent?) {
        val id = i?.getStringExtra("vehicleId") ?: return
        if (i.getStringExtra("channel") == NotificationHelper.CH_ODO) vm.openOdometerFor(id) else vm.selectedId.value = id
    }
}

private val nf = NumberFormat.getInstance(Locale("id", "ID"))
private fun fmtKm(v: Long?) = if (v == null) "-" else "${nf.format(v)} km"
private fun fmtRp(v: Long) = "Rp ${nf.format(v)}"
private val dfmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))
private fun fmtDay(d: Long?) = if (d == null) "-" else LocalDate.ofEpochDay(d).format(dfmt)
private fun fmtTs(t: Long?) = if (t == null) "-" else Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate().format(dfmt)
private fun digits(s: String) = s.filter { it.isDigit() }.take(12)

@Composable
fun App(vm: MainViewModel) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val tabs = listOf(Triple("home", "Beranda", Icons.Default.Home), Triple("jadwal", "Jadwal", Icons.Default.DateRange),
        Triple("riwayat", "Riwayat", Icons.Default.Menu), Triple("pengaturan", "Pengaturan", Icons.Default.Settings))
    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEach { (r, l, ic) ->
                NavigationBarItem(selected = route == r, icon = { Icon(ic, null) }, label = { Text(l) }, onClick = {
                    nav.navigate(r) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }
                })
            }
        }
    }) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm) }
            composable("jadwal") { ScheduleScreen(vm) }
            composable("riwayat") { HistoryScreen(vm) }
            composable("pengaturan") { SettingsScreen(vm) }
        }
    }
}

@Composable
fun FormDialog(title: String, confirm: String = "Simpan", onDismiss: () -> Unit, onOk: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) },
        confirmButton = { TextButton(onClick = onOk) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false) =
    OutlinedTextField(value, { onChange(if (numeric) digits(it) else it) }, label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text))

@Composable
fun Dropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SecondaryButton(onClick = { open = true }, Modifier.fillMaxWidth()) { Text("$label: $selected") }
        DropdownMenu(open, { open = false }) { options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); open = false }) } }
    }
}

private fun parseDate(s: String): LocalDate? = runCatching { LocalDate.parse(s.trim()) }.getOrNull()

@Composable
fun HomeScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val vehicles by vm.vehicles.collectAsState(); val cur by vm.current.collectAsState()
    val schedules by vm.schedules.collectAsState(); val docs by vm.documents.collectAsState()
    val services by vm.services.collectAsState()
    val showOdo by vm.showOdo.collectAsState()
    var addVehicle by remember { mutableStateOf(false) }

    if (addVehicle) AddVehicleDialog({ addVehicle = false }) { vm.addVehicle(it); addVehicle = false; Toast.makeText(ctx, "Kendaraan disimpan", Toast.LENGTH_SHORT).show() }
    if (showOdo && cur != null) OdometerDialog(cur!!, { vm.showOdo.value = false }) { km, note ->
        vm.saveOdometer(cur!!, km, note); vm.showOdo.value = false; Toast.makeText(ctx, "Odometer diperbarui", Toast.LENGTH_SHORT).show() }

    if (cur == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            EmptyState(Icons.Default.Home, "Belum ada kendaraan", "Tambahkan kendaraan pertama Anda untuk mulai mencatat kilometer dan jadwal servis.", "Tambah Kendaraan") { addVehicle = true }
        }
        return
    }
    val v = cur!!
    val mine = schedules.filter { it.vehicleId == v.id }.map { it to ServiceEngine.evaluate(it.input(), v.currentKm, LocalDate.now()) }
        .sortedByDescending { it.second.status.urgency }
    val month = LocalDate.now().withDayOfMonth(1).toEpochDay(); val year = LocalDate.now().withDayOfYear(1).toEpochDay()
    val vs = services.filter { it.vehicleId == v.id }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Column { Text("OdoCare", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary); Text("Pantau kilometer, servis, dan dokumen kendaraan Anda.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vehicles) { x -> FilterChip(x.id == v.id, { vm.selectedId.value = x.id }, { Text(x.name) }) }
                item { AssistChip({ addVehicle = true }, { Text("+ Kendaraan") }) }
            }
        }
        item {
            OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(v.name, style = MaterialTheme.typography.titleLarge)
                Text("${v.brand} ${v.model} • ${v.plate}".trim())
                OdometerValue(v.currentKm?.let { nf.format(it) } ?: "—")
                Text("Update terakhir: ${fmtTs(v.lastOdoAt)}")
                Text("Kilometer hanya berubah dari input Anda.", style = MaterialTheme.typography.bodySmall)
                PrimaryButton(onClick = { vm.showOdo.value = true }) { Text("Update Odometer") }
            } }
        }
        item {
            OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("Ringkasan Servis", style = MaterialTheme.typography.titleMedium)
                listOf(ServiceStatus.NORMAL, ServiceStatus.SEGERA_SERVIS, ServiceStatus.JATUH_TEMPO, ServiceStatus.TERLAMBAT).forEach { st ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusBadge(st.label, statusTone(st)); Text("${mine.count { it.second.status == st }}", style = MaterialTheme.typography.titleMedium) } }
            } }
        }
        item { Text("Jadwal Terdekat", style = MaterialTheme.typography.titleMedium) }
        if (mine.isEmpty()) item { Text("Belum ada jadwal servis. Buka tab Jadwal untuk menambahkan.") }
        items(mine.take(3)) { (s, r) -> ScheduleCard(s, r) }
        item {
            OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("Pengeluaran Servis", style = MaterialTheme.typography.titleMedium)
                Text("Bulan ini: ${fmtRp(vs.filter { it.epochDay >= month }.sumOf { it.cost })}")
                Text("Tahun ini: ${fmtRp(vs.filter { it.epochDay >= year }.sumOf { it.cost })}")
            } }
        }
        item { Text("Pengingat Dokumen", style = MaterialTheme.typography.titleMedium) }
        val dl = docs.map { it to DocumentEngine.status(it.dueEpochDay?.let { d -> LocalDate.ofEpochDay(d) }, LocalDate.now()) }
        if (dl.isEmpty()) item { Text("Belum ada dokumen.") }
        items(dl.take(3)) { (d, st) -> OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(d.name); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusBadge(st.label, docTone(st)); Text(fmtDay(d.dueEpochDay)) } } } }
    }
}

@Composable
fun ScheduleCard(s: MaintenanceSchedule, r: ServiceResult, extra: @Composable ColumnScope.() -> Unit = {}) =
    OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(s.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            StatusBadge(r.status.label, statusTone(r.status))
        }
        Text(s.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        r.targetKm?.let { Text("Servis berikutnya pada ${fmtKm(it)}") }
        r.remainingKm?.let { Text(if (it >= 0) "Sisa ${fmtKm(it)}" else "Terlambat ${fmtKm(-it)}") }
        r.targetDate?.let { Text("Jatuh tempo ${it.format(dfmt)}") }
        r.remainingDays?.let { Text(if (it >= 0) "Sisa $it hari" else "Terlambat ${-it} hari") }
        val rem = r.remainingKm; val iv = s.intervalKm
        if (rem != null && iv != null && iv > 0) LinearProgressIndicator(progress = { (1f - rem.toFloat() / iv).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        if (r.status == ServiceStatus.KM_BELUM_DIPERBARUI) Text("Kilometer perlu diperbarui.")
        extra()
    } }

@Composable
fun AddVehicleDialog(onDismiss: () -> Unit, onSave: (Vehicle) -> Unit) {
    var name by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Mobil") }
    var brand by remember { mutableStateOf("") }; var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }; var km by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
    FormDialog("Tambah Kendaraan", onDismiss = onDismiss, onOk = {
        if (name.isBlank()) err = "Nama kendaraan wajib diisi." else onSave(Vehicle(name = name.trim(), type = type, brand = brand, model = model, plate = plate, currentKm = km.toLongOrNull(), lastOdoAt = if (km.isNotEmpty()) System.currentTimeMillis() else null))
    }) {
        Field("Nama kendaraan", name, { name = it }); Dropdown("Jenis", listOf("Mobil", "Motor", "Lainnya"), type) { type = it }
        Field("Merek", brand, { brand = it }); Field("Model", model, { model = it }); Field("Nomor polisi", plate, { plate = it })
        Field("Kilometer saat ini", km, { km = it }, true); err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun OdometerDialog(v: Vehicle, onDismiss: () -> Unit, onSave: (Long, String) -> Unit) {
    var km by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }; var confirmLower by remember { mutableStateOf(false) }
    if (confirmLower) AlertDialog(onDismissRequest = { confirmLower = false }, title = { Text("Kilometer lebih rendah") },
        text = { Text("Nilai baru lebih rendah dari catatan sebelumnya (${fmtKm(v.currentKm)}). Apakah ini koreksi data yang disengaja?") },
        confirmButton = { TextButton(onClick = { onSave(km.toLong(), note.ifBlank { "Koreksi data" }) }) { Text("Ya, simpan koreksi") } },
        dismissButton = { TextButton(onClick = { confirmLower = false }) { Text("Periksa lagi") } })
    FormDialog("Update Odometer — ${v.name}", onDismiss = onDismiss, onOk = {
        val k = km.toLongOrNull()
        when { k == null -> err = "Masukkan angka kilometer yang valid."
            v.currentKm != null && k < v.currentKm -> confirmLower = true
            else -> onSave(k, note) }
    }) {
        Text("Kilometer terakhir: ${fmtKm(v.currentKm)}")
        Field("Kilometer baru", km, { km = it }, true); Field("Catatan (opsional)", note, { note = it })
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun ScheduleScreen(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(tab) { Tab(tab == 0, { tab = 0 }, text = { Text("Servis") }); Tab(tab == 1, { tab = 1 }, text = { Text("Dokumen & Legalitas") }) }
        if (tab == 0) ServiceTab(vm) else DocumentTab(vm)
    }
}

@Composable
fun ServiceTab(vm: MainViewModel) {
    val cur by vm.current.collectAsState(); val all by vm.schedules.collectAsState()
    var add by remember { mutableStateOf(false) }; var log by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var del by remember { mutableStateOf<MaintenanceSchedule?>(null) }; var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    val v = cur
    if (v == null) { Text("Tambahkan kendaraan di Beranda terlebih dahulu.", Modifier.padding(16.dp)); return }
    if (add) ScheduleDialog(v, { add = false }) { vm.saveSchedule(it); add = false }
    log?.let { s -> LogServiceDialog(s, v, { log = null }) { km, day, cost -> vm.logService(s, v, km, day, cost); log = null } }
    del?.let { s -> AlertDialog(onDismissRequest = { del = null }, title = { Text("Arsipkan jadwal?") }, text = { Text("${s.name} diarsipkan. Riwayat servis tetap tersimpan.") },
        confirmButton = { TextButton(onClick = { vm.archiveSchedule(s.id); del = null }) { Text("Arsipkan") } }, dismissButton = { TextButton(onClick = { del = null }) { Text("Batal") } }) }
    val shown = all.filter { it.vehicleId == v.id && it.name.contains(q, true) }
        .map { it to ServiceEngine.evaluate(it.input(), v.currentKm, LocalDate.now()) }
        .filter { filter == "Semua" || it.second.status.label.contains(filter) }.sortedByDescending { it.second.status.urgency }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PrimaryButton(onClick = { add = true }) { Text("Tambah Jadwal") } }
        item { Field("Cari jadwal", q, { q = it }) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(listOf("Semua", "Normal", "Segera Servis", "Jatuh Tempo", "Terlambat")) { f -> FilterChip(filter == f, { filter = f }, { Text(f) }) } } }
        if (shown.isEmpty()) item { EmptyState(Icons.Default.DateRange, "Belum ada jadwal servis", "Buat jadwal pertama agar Anda dapat memantau perawatan kendaraan.", "Tambah Jadwal") { add = true } }
        items(shown) { (s, r) -> ScheduleCard(s, r) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(onClick = { log = s }) { Text("Catat Servis") }; SecondaryButton(onClick = { del = s }) { Text("Arsipkan") } } } }
    }
}

@Composable
fun ScheduleDialog(v: Vehicle, onDismiss: () -> Unit, onSave: (MaintenanceSchedule) -> Unit) {
    var name by remember { mutableStateOf("") }; var cat by remember { mutableStateOf("Mesin") }
    var ik by remember { mutableStateOf("") }; var im by remember { mutableStateOf("") }
    var lk by remember { mutableStateOf(v.currentKm?.toString() ?: "") }; var ld by remember { mutableStateOf(LocalDate.now().toString()) }
    var warn by remember { mutableStateOf("500") }; var err by remember { mutableStateOf<String?>(null) }
    FormDialog("Tambah Jadwal Servis", onDismiss = onDismiss, onOk = {
        val ikm = ik.toLongOrNull(); val imo = im.toIntOrNull(); val date = parseDate(ld)
        err = when { name.isBlank() -> "Nama perawatan wajib diisi."
            ikm == null && imo == null -> "Isi interval kilometer, interval bulan, atau keduanya."
            imo != null && date == null -> "Tanggal servis terakhir harus berformat yyyy-MM-dd."
            ikm != null && lk.toLongOrNull() == null -> "Isi kilometer servis terakhir."
            else -> null }
        if (err == null) onSave(MaintenanceSchedule(vehicleId = v.id, name = name.trim(), category = cat, intervalKm = ikm, intervalMonths = imo,
            lastKm = lk.toLongOrNull(), lastEpochDay = date?.toEpochDay(), warnKm = warn.toLongOrNull() ?: 500))
    }) {
        Field("Nama perawatan (mis. Ganti oli)", name, { name = it })
        Dropdown("Kategori", listOf("Mesin", "Ban", "Rem", "Kelistrikan", "Umum"), cat) { cat = it }
        Field("Interval kilometer (kosongkan jika tidak dipakai)", ik, { ik = it }, true)
        Field("Interval bulan (kosongkan jika tidak dipakai)", im, { im = it.filter(Char::isDigit).take(3) })
        Field("Kilometer servis terakhir", lk, { lk = it }, true); DateField("Tanggal servis terakhir", ld) { ld = it }
        Field("Ingatkan saat sisa (km)", warn, { warn = it }, true)
        Text("Interval hanya contoh; sesuaikan dengan buku manual kendaraan.", style = MaterialTheme.typography.bodySmall)
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun LogServiceDialog(s: MaintenanceSchedule, v: Vehicle, onDismiss: () -> Unit, onSave: (Long, Long, Long) -> Unit) {
    var km by remember { mutableStateOf(v.currentKm?.toString() ?: "") }; var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var cost by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
    FormDialog("Catat Servis — ${s.name}", onDismiss = onDismiss, onOk = {
        val k = km.toLongOrNull(); val d = parseDate(date)
        if (k == null) err = "Kilometer tidak valid." else if (d == null) err = "Tanggal harus berformat yyyy-MM-dd." else onSave(k, d.toEpochDay(), cost.toLongOrNull() ?: 0)
    }) { Field("Kilometer saat servis", km, { km = it }, true); DateField("Tanggal servis", date) { date = it }; Field("Biaya aktual (Rp)", cost, { cost = it }, true)
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
}

private val docTypes = listOf("Pajak Tahunan", "STNK 5 Tahunan", "SIM A", "SIM C", "Asuransi", "Garansi", "Lainnya")

@Composable
fun DocumentTab(vm: MainViewModel) {
    val docs by vm.documents.collectAsState(); val vehicles by vm.vehicles.collectAsState()
    var add by remember { mutableStateOf(false) }; var renew by remember { mutableStateOf<LegalDocument?>(null) }
    if (add) DocDialog(vehicles, { add = false }) { vm.saveDocument(it); add = false }
    renew?.let { d -> RenewDialog(d, { renew = null }) { due, amt -> vm.renewDocument(d, due, amt); renew = null } }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PrimaryButton(onClick = { add = true }) { Text("Tambah Dokumen") } }
        item { Text("OdoCare hanya mencatat data yang Anda masukkan; tidak memverifikasi status resmi dokumen.", style = MaterialTheme.typography.bodySmall) }
        if (docs.isEmpty()) item { EmptyState(Icons.Default.Menu, "Belum ada dokumen", "Catat pajak, STNK, SIM, atau asuransi agar Anda diingatkan sebelum jatuh tempo.", "Tambah Dokumen") { add = true } }
        items(docs.sortedBy { it.dueEpochDay ?: Long.MAX_VALUE }) { d ->
            val st = DocumentEngine.status(d.dueEpochDay?.let { LocalDate.ofEpochDay(it) }, LocalDate.now())
            OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(d.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); StatusBadge(st.label, docTone(st)) }; Text(d.type, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Jatuh tempo: ${fmtDay(d.dueEpochDay)}"); Text("Pembayaran/perpanjangan terakhir: ${fmtDay(d.lastPaidEpochDay)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(onClick = { renew = d }) { Text("Catat Perpanjangan") }
                    SecondaryButton(onClick = { vm.archiveDocument(d.id) }) { Text("Arsipkan") } }
            } }
        }
    }
}

@Composable
fun DocDialog(vehicles: List<Vehicle>, onDismiss: () -> Unit, onSave: (LegalDocument) -> Unit) {
    var name by remember { mutableStateOf("") }; var type by remember { mutableStateOf(docTypes[0]) }
    var due by remember { mutableStateOf("") }; var ra by remember { mutableStateOf("30") }
    var ru by remember { mutableStateOf(ReminderUnit.DAY) }; var hh by remember { mutableStateOf("8") }; var mm by remember { mutableStateOf("0") }
    var veh by remember { mutableStateOf("Tanpa kendaraan") }; var err by remember { mutableStateOf<String?>(null) }
    FormDialog("Tambah Dokumen", onDismiss = onDismiss, onOk = {
        val date = parseDate(due); val h = hh.toIntOrNull() ?: -1; val m = mm.toIntOrNull() ?: -1
        err = when { name.isBlank() -> "Nama dokumen wajib diisi."
            due.isNotBlank() && date == null -> "Tanggal jatuh tempo harus berformat yyyy-MM-dd."
            else -> ReminderEngine.validate(ra, ru, h, m) }
        if (err == null) onSave(LegalDocument(vehicleId = vehicles.firstOrNull { it.name == veh }?.id, name = name.trim(), type = type,
            dueEpochDay = date?.toEpochDay(), remAmount = ra.toInt(), remUnit = ru.name, remHour = h, remMinute = m))
    }) {
        Field("Nama dokumen", name, { name = it }); Dropdown("Jenis", docTypes, type) { type = it }
        Dropdown("Kendaraan", listOf("Tanpa kendaraan") + vehicles.map { it.name }, veh) { veh = it }
        DateField("Tanggal jatuh tempo (sesuai dokumen)", due) { due = it }
        Text("Pengingat", style = MaterialTheme.typography.titleSmall)
        Field("Jumlah waktu", ra, { ra = it }, true); Dropdown("Satuan", ReminderUnit.values().map { it.label }, ru.label) { l -> ru = ReminderUnit.values().first { it.label == l } }
        TimeField("Jam notifikasi", hh, mm) { h, m -> hh = h; mm = m }
        Text("Pengingat dikirim $ra ${ru.label.lowercase()} sebelum jatuh tempo pukul ${hh.padStart(2, '0')}.${mm.padStart(2, '0')}.")
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun RenewDialog(d: LegalDocument, onDismiss: () -> Unit, onSave: (LocalDate, Long?) -> Unit) {
    var due by remember { mutableStateOf("") }; var amt by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
    FormDialog("Catat Perpanjangan — ${d.name}", onDismiss = onDismiss, onOk = {
        val date = parseDate(due); if (date == null) err = "Masukkan tanggal jatuh tempo baru (yyyy-MM-dd)." else onSave(date, amt.toLongOrNull())
    }) { DateField("Tanggal jatuh tempo baru", due) { due = it }; Field("Nominal (opsional)", amt, { amt = it }, true)
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
}

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val logs by vm.logs.collectAsState(); val services by vm.services.collectAsState(); val vehicles by vm.vehicles.collectAsState()
    val hist by vm.docHistory.collectAsState()
    val name = { id: String -> vehicles.firstOrNull { it.id == id }?.name ?: "Kendaraan" }
    var delLog by remember { mutableStateOf<OdometerLog?>(null) }
    delLog?.let { l -> AlertDialog(onDismissRequest = { delLog = null }, title = { Text("Hapus catatan?") }, text = { Text("Catatan ${fmtKm(l.km)} akan dihapus.") },
        confirmButton = { TextButton(onClick = { vm.deleteLog(l.id); delLog = null }) { Text("Hapus") } }, dismissButton = { TextButton(onClick = { delLog = null }) { Text("Batal") } }) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Riwayat Servis", style = MaterialTheme.typography.titleMedium) }
        if (services.isEmpty()) item { Text("Belum ada riwayat servis.") }
        items(services) { s -> OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text("${s.name} • ${name(s.vehicleId)}"); Text("${fmtDay(s.epochDay)} • ${fmtKm(s.km)} • ${fmtRp(s.cost)}") } } }
        item { Text("Riwayat Odometer", style = MaterialTheme.typography.titleMedium) }
        if (logs.isEmpty()) item { Text("Belum ada catatan odometer.") }
        items(logs) { l ->
            val prev = logs.filter { it.vehicleId == l.vehicleId && it.recordedAt < l.recordedAt }.maxByOrNull { it.recordedAt }
            OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text("${name(l.vehicleId)} • ${fmtKm(l.km)}"); Text("${fmtTs(l.recordedAt)} ${l.note}")
                prev?.let { Text("Jarak sejak catatan sebelumnya: ${fmtKm(l.km - it.km)}") }
                TextButton(onClick = { delLog = l }) { Text("Hapus") } } }
        }
        item { Text("Riwayat Perpanjangan Dokumen", style = MaterialTheme.typography.titleMedium) }
        if (hist.isEmpty()) item { Text("Belum ada perpanjangan tercatat.") }
        items(hist) { h -> OdoCareCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text("${fmtDay(h.actionEpochDay)}: ${fmtDay(h.oldDueEpochDay)} → ${fmtDay(h.newDueEpochDay)}"); h.amount?.let { Text(fmtRp(it)) } } } }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val c = LocalContext.current
    var granted by remember { mutableStateOf(NotificationHelper.hasPermission(c)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var odoOn by remember { mutableStateOf(Prefs.getBool(c, "odo_on", false)) }
    var amt by remember { mutableStateOf(Prefs.getInt(c, "odo_amount", 7).toString()) }
    var unit by remember { mutableStateOf(ReminderUnit.valueOf(Prefs.getStr(c, "odo_unit", "DAY"))) }
    var hh by remember { mutableStateOf(Prefs.getInt(c, "odo_hour", 8).toString()) }
    var mm by remember { mutableStateOf(Prefs.getInt(c, "odo_min", 0).toString()) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tampilan", style = MaterialTheme.typography.titleLarge)
        Dropdown("Tema", listOf("Ikuti sistem", "Gelap", "Terang"), ThemeState.mode) { ThemeState.mode = it; Prefs.put(c, "theme", it) }
        HorizontalDivider()
        Text("Notifikasi", style = MaterialTheme.typography.titleLarge)
        if (!granted) { Text("Izin notifikasi belum diberikan."); PrimaryButton(onClick = { if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Beri Izin Notifikasi") } }
        else Text("Izin notifikasi aktif.")
        PrimaryButton(onClick = { val ok = NotificationHelper.show(c, 1, NotificationHelper.CH_SERVIS, "Uji notifikasi OdoCare", "Notifikasi berfungsi.", null)
            Toast.makeText(c, if (ok) "Notifikasi uji dikirim" else "Tidak dapat mengirim: periksa izin/pengaturan kategori", Toast.LENGTH_SHORT).show() }) { Text("Uji Notifikasi") }
        listOf(NotificationHelper.CH_SERVIS to "Pengingat Servis", NotificationHelper.CH_DOKUMEN to "Pengingat Dokumen", NotificationHelper.CH_ODO to "Pengingat Odometer").forEach { (k, l) ->
            var on by remember { mutableStateOf(NotificationHelper.enabled(c, k)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(l); Switch(on, { on = it; Prefs.put(c, "notif_$k", it) }) } }
        Text("Waktu pengiriman bisa bergeser jika sistem Android membatasi alarm tepat.", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Pengingat Update Odometer", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Aktif"); Switch(odoOn, { odoOn = it }) }
        Field("Setiap (jumlah)", amt, { amt = digits(it) }, true)
        Dropdown("Satuan", ReminderUnit.values().map { it.label }, unit.label) { l -> unit = ReminderUnit.values().first { it.label == l } }
        TimeField("Jam notifikasi", hh, mm) { h, m -> hh = h; mm = m }
        Text("Pengingat dikirim setiap $amt ${unit.label.lowercase()} sejak update terakhir, pukul ${hh.padStart(2, '0')}.${mm.padStart(2, '0')}.")
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton(onClick = {
            err = ReminderEngine.validate(amt, unit, hh.toIntOrNull() ?: -1, mm.toIntOrNull() ?: -1)
            if (err == null) {
                Prefs.put(c, "odo_on", odoOn); Prefs.put(c, "odo_amount", amt.toInt()); Prefs.put(c, "odo_unit", unit.name)
                Prefs.put(c, "odo_hour", hh.toInt()); Prefs.put(c, "odo_min", mm.toInt())
                scope.launch { Rescheduler.rescheduleAll(c) }
                Toast.makeText(c, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
            }
        }) { Text("Simpan Pengaturan Odometer") }
        HorizontalDivider()
        Text("Tentang", style = MaterialTheme.typography.titleLarge)
        Text("OdoCare 1.0.0 — data disimpan lokal di perangkat. Anda bertanggung jawab atas pencadangan data.")
    }
}
