package com.odocare.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.InvalidationTracker
import com.odocare.app.data.*
import com.odocare.app.notif.Notif
import com.odocare.app.ui.DocTypes
import com.odocare.app.ui.VehicleTypes
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repo(app)
    private val prefs = repo.prefs
    private fun <T> Flow<T>.state(initial: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val vehicles: StateFlow<List<Vehicle>> = repo.vehicles.state(emptyList())
    val activeVehicle: StateFlow<Vehicle?> = repo.activeVehicle.state(null)
    val currentKm: StateFlow<Int?> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(null) else repo.latestOdometer(v.id) }.state(null)
    val odometerLogs: StateFlow<List<OdometerLog>> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(emptyList()) else repo.odometer(v.id) }.state(emptyList())
    val schedules: StateFlow<List<MaintenanceSchedule>> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(emptyList()) else repo.schedules(v.id) }.state(emptyList())
    val documents: StateFlow<List<LegalDocument>> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(emptyList()) else repo.documents(v.id) }.state(emptyList())
    val records: StateFlow<List<ServiceRecord>> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(emptyList()) else repo.records(v.id) }.state(emptyList())
    val fuelLogs: StateFlow<List<FuelLog>> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(emptyList()) else repo.fuel(v.id) }.state(emptyList())

    // Semua kendaraan (Dashboard & Laporan)
    val masters: StateFlow<List<MasterCategory>> = repo.masters.state(emptyList())
    val vehicleTypes: StateFlow<List<VehicleType>> = repo.vehicleTypes.state(com.odocare.app.domain.Catalog.defaultTypes())
    val documentTypes: StateFlow<List<DocumentType>> = repo.documentTypes.state(com.odocare.app.domain.Catalog.defaultDocTypes())
    val notifications: StateFlow<List<AppNotification>> = repo.notifications.state(emptyList())
    val unreadNotifications: StateFlow<Int> = repo.unreadNotifications.state(0)
    val allSchedules: StateFlow<List<MaintenanceSchedule>> = repo.allSchedules.state(emptyList())
    val allDocuments: StateFlow<List<LegalDocument>> = repo.allDocuments.state(emptyList())
    val allRecords: StateFlow<List<ServiceRecord>> = repo.allRecords.state(emptyList())
    val allOdometer: StateFlow<List<OdometerLog>> = repo.allOdometer.state(emptyList())
    val allFuel: StateFlow<List<FuelLog>> = repo.allFuel.state(emptyList())

    // ---- Kendaraan ----
    fun addVehicle(name: String, plate: String, type: String, initialKm: Int, autoSchedules: Boolean, autoDocs: Boolean) = viewModelScope.launch {
        if (name.isNotBlank()) repo.addVehicleWithDefaults(Vehicle(name = name.trim(), plate = plate.trim().uppercase(), type = type, initialKm = initialKm), autoSchedules, autoDocs)
    }
    fun updateVehicle(v: Vehicle) = viewModelScope.launch { repo.updateVehicle(v) }
    fun setActiveVehicle(id: String) = viewModelScope.launch { repo.setActive(id) }
    fun deleteVehicle(id: String) = viewModelScope.launch { repo.deleteVehicle(id) }

    // ---- Odometer ----
    fun addOdometer(km: Int) = viewModelScope.launch { activeVehicle.value?.let { if (km > 0) repo.addOdometer(OdometerLog(vehicleId = it.id, km = km)) } }
    fun updateOdometer(log: OdometerLog) = viewModelScope.launch { repo.updateOdometer(log) }
    fun deleteOdometer(id: String) = viewModelScope.launch { repo.deleteOdometer(id) }

    // ---- Jadwal / kategori servis ----
    fun addSchedule(name: String, intervalKm: Int?, intervalMonths: Int?, lastKm: Int?, lastDate: Long, notifyEnabled: Boolean, notifyHour: Int, notifyMinute: Int, warnKm: Int, warnDays: Int) = viewModelScope.launch {
        activeVehicle.value?.let { if (name.isNotBlank()) repo.addSchedule(MaintenanceSchedule(vehicleId = it.id, name = name.trim(), intervalKm = intervalKm, intervalMonths = intervalMonths, lastServiceKm = lastKm ?: currentKm.value, lastServiceDate = lastDate, notifyEnabled = notifyEnabled, notifyHour = notifyHour, notifyMinute = notifyMinute, warnKm = warnKm, warnDays = warnDays)) }
    }
    fun updateSchedule(s: MaintenanceSchedule) = viewModelScope.launch { repo.updateSchedule(s) }
    fun deleteSchedule(id: String) = viewModelScope.launch { repo.deleteSchedule(id) }
    fun recordService(schedule: MaintenanceSchedule, km: Int, dateMillis: Long, cost: Long, partsCost: Long, laborCost: Long, notes: String?, imagePath: String?) = viewModelScope.launch { if (km > 0) repo.recordService(schedule, km, dateMillis, cost, partsCost, laborCost, notes, imagePath) }

    // ---- Riwayat servis ----
    fun updateRecord(r: ServiceRecord) = viewModelScope.launch {
        val old = records.value.firstOrNull { it.id == r.id }?.imagePath
        repo.updateRecord(r)
        if (old != null && old != r.imagePath) ImageStore.delete(old)
    }
    /** Hapus langsung dengan tombol Urungkan. File foto baru dihapus setelah kesempatan urungkan lewat. */
    fun deleteRecord(id: String) = viewModelScope.launch {
        val rec = records.value.firstOrNull { it.id == id } ?: return@launch
        repo.deleteRecord(id)
        offerUndo("Riwayat \"${rec.title}\" dihapus", onUndo = { viewModelScope.launch { repo.restoreRecord(rec) } }, onExpire = { ImageStore.delete(rec.imagePath) })
    }

    // ---- BBM ----
    fun addFuel(date: Long, km: Int, liters: Double, totalPrice: Long, fullTank: Boolean) = viewModelScope.launch {
        activeVehicle.value?.let { if (km > 0 && liters > 0 && totalPrice > 0) repo.addFuel(FuelLog(vehicleId = it.id, date = date, km = km, liters = liters, totalPrice = totalPrice, fullTank = fullTank)) }
    }
    fun updateFuel(f: FuelLog) = viewModelScope.launch { repo.updateFuel(f) }
    fun deleteFuel(id: String) = viewModelScope.launch {
        val f = fuelLogs.value.firstOrNull { it.id == id } ?: return@launch
        repo.deleteFuel(id)
        offerUndo("Catatan BBM dihapus", onUndo = { viewModelScope.launch { repo.restoreFuel(f) } })
    }

    // ---- Dokumen ----
    fun addDocument(type: String, expiryMillis: Long, number: String?, lastRenewed: Long?, intervalValue: Int?, intervalUnit: String?, reminders: String, notifyEnabled: Boolean, notifyHour: Int, notifyMinute: Int, attachmentPath: String?, attachmentName: String?) = viewModelScope.launch {
        activeVehicle.value?.let { repo.addDocument(LegalDocument(vehicleId = it.id, type = type, number = number, expiryDate = expiryMillis, lastRenewedDate = lastRenewed, intervalValue = intervalValue, intervalUnit = intervalUnit, reminders = reminders, notifyEnabled = notifyEnabled, notifyHour = notifyHour, notifyMinute = notifyMinute, attachmentPath = attachmentPath, attachmentName = attachmentName)) }
    }
    fun updateDocument(d: LegalDocument) = viewModelScope.launch { repo.updateDocument(d) }
    fun renewDocument(doc: LegalDocument, renewedDate: Long, newExpiry: Long) = viewModelScope.launch { repo.renewDocument(doc, renewedDate, newExpiry) }
    fun deleteDocument(id: String) = viewModelScope.launch {
        val old = documents.value.firstOrNull { it.id == id }?.attachmentPath
        repo.deleteDocument(id)
        ImageStore.delete(old)
    }

    /** Bagian aktif di tab Servis: 0 = Jadwal & Komponen (default), 1 = Pajak & Dokumen. */
    var serviceSection by mutableStateOf(0)

    /** Tab tujuan layar Servis (0 Semua, 1 Lewat, 2 Segera, 3 Aman), diisi dari kotak ringkasan di Home. Dibaca sekali. */
    var pendingScheduleTab: Int = 0

    // ---- Master kategori (tidak mengubah kendaraan yang sudah ada) ----
    fun addMaster(name: String, type: String, km: Int, months: Int, notifyEnabled: Boolean, notifyHour: Int, notifyMinute: Int, warnKm: Int, warnDays: Int) = viewModelScope.launch {
        if (name.isNotBlank()) repo.addMaster(MasterCategory(name = name.trim(), vehicleType = type, defaultKm = km, defaultMonths = months, notifyEnabled = notifyEnabled, notifyHour = notifyHour, notifyMinute = notifyMinute, warnKm = warnKm, warnDays = warnDays))
    }
    fun updateMaster(m: MasterCategory) = viewModelScope.launch { repo.updateMaster(m) }
    fun deleteMaster(id: String) = viewModelScope.launch { repo.deleteMaster(id) }

    // ---- Master jenis kendaraan ----
    fun addVehicleType(name: String, icon: String) = viewModelScope.launch {
        if (name.isNotBlank()) repo.addVehicleType(VehicleType(code = "T_" + UUID.randomUUID().toString().take(8).uppercase(), name = name.trim(), icon = icon))
    }
    fun updateVehicleType(t: VehicleType) = viewModelScope.launch { repo.updateVehicleType(t) }
    /** onResult(false) = jenis ini masih dipakai kendaraan, tidak dihapus. */
    fun deleteVehicleType(code: String, onResult: (Boolean) -> Unit) = viewModelScope.launch { onResult(repo.deleteVehicleType(code)) }

    // ---- Master jenis dokumen ----
    fun addDocType(name: String, vehicleType: String, value: Int, unit: String) = viewModelScope.launch {
        if (name.isNotBlank()) repo.addDocType(DocumentType(code = "D_" + UUID.randomUUID().toString().take(8).uppercase(), name = name.trim(), vehicleType = vehicleType, defaultValue = value, defaultUnit = unit))
    }
    fun updateDocType(t: DocumentType) = viewModelScope.launch { repo.updateDocType(t) }
    /** onResult(false) = jenis ini masih dipakai dokumen, tidak dihapus. */
    fun deleteDocType(code: String, onResult: (Boolean) -> Unit) = viewModelScope.launch { onResult(repo.deleteDocType(code)) }

    // ---- Riwayat notifikasi ----
    fun markNotificationRead(id: Long) = viewModelScope.launch { repo.markNotificationRead(id) }
    fun markAllNotificationsRead() = viewModelScope.launch { repo.markAllNotificationsRead() }
    fun deleteNotification(n: AppNotification) = viewModelScope.launch {
        repo.deleteNotification(n.id)
        offerUndo("Notifikasi dihapus", onUndo = { viewModelScope.launch { repo.restoreNotifications(listOf(n)) } })
    }
    fun clearNotifications() = viewModelScope.launch {
        val all = notifications.value
        if (all.isEmpty()) return@launch
        repo.clearNotifications()
        offerUndo("${all.size} notifikasi dihapus", onUndo = { viewModelScope.launch { repo.restoreNotifications(all) } })
    }

    // ---- Snackbar Urungkan (ditampilkan di MainActivity) ----
    class UndoEvent(val message: String, val onUndo: () -> Unit, val onExpire: () -> Unit)
    val undoEvents = kotlinx.coroutines.flow.MutableSharedFlow<UndoEvent>(extraBufferCapacity = 8)
    private fun offerUndo(message: String, onUndo: () -> Unit, onExpire: () -> Unit = {}) { undoEvents.tryEmit(UndoEvent(message, onUndo, onExpire)) }

    // ---- Pengingat backup di Home ----
    private val sp = app.getSharedPreferences("odocare", android.content.Context.MODE_PRIVATE)
    /** Banner pengingat backup disembunyikan sampai waktu ini (ms). */
    var backupSnoozeUntil by mutableStateOf(sp.getLong("backup_snooze_until", 0L))
        private set
    fun snoozeBackupReminder(days: Int = 7) {
        val t = System.currentTimeMillis() + days * 86_400_000L
        backupSnoozeUntil = t; sp.edit().putLong("backup_snooze_until", t).apply()
    }
    // ---- Onboarding: sekali untuk pengguna baru. 0 = mengecek, 1 = tampilkan, 2 = selesai/tidak perlu ----
    var onboardingState by mutableStateOf(if (sp.getBoolean("onboarding_done", false)) 2 else 0)
        private set
    init {
        if (onboardingState == 0) viewModelScope.launch {
            // Pengguna lama yang sudah punya kendaraan tidak perlu pengantar
            if (repo.vehicles.first().isNotEmpty()) finishOnboarding() else onboardingState = 1
        }
    }
    fun finishOnboarding() { sp.edit().putBoolean("onboarding_done", true).apply(); onboardingState = 2 }

    /** Dari banner Home: buka tab Kendaraan dengan bagian Backup & Data langsung terbuka. */
    var expandBackupSection by mutableStateOf(false)

    // ---- Notifikasi ----
    fun testNotification() = Notif.show(getApplication(), Notif.CH_SERVICE, "Notifikasi aktif", "Pengingat servis, pajak, dokumen, dan update KM akan muncul seperti ini.")

    // ---- Backup, perubahan sejak backup, snapshot otomatis ----
    val lastBackup = MutableStateFlow(prefs.getLong("last_backup", 0L))
    val changesSinceBackup = MutableStateFlow(prefs.getInt("changes_since_backup", 0))

    private val observer = object : InvalidationTracker.Observer(arrayOf("vehicles", "odometer_logs", "maintenance_schedules", "service_records", "legal_documents", "document_history", "master_categories", "document_types", "fuel_logs")) {
        override fun onInvalidated(tables: Set<String>) {
            val n = prefs.getInt("changes_since_backup", 0) + 1
            prefs.edit().putInt("changes_since_backup", n).apply()
            changesSinceBackup.value = n
            maybeSnapshot()
            Notif.onDataChanged(getApplication())   // jam/hari notifikasi berubah -> langsung berlaku, alarm dipasang ulang
        }
    }
    init {
        repo.db.invalidationTracker.addObserver(observer)
        // Daftar jenis kendaraan dipakai di banyak layar (label & ikon), jadi disimpan di registri global
        viewModelScope.launch { repo.vehicleTypes.collect { VehicleTypes.all = it } }
        viewModelScope.launch { repo.documentTypes.collect { DocTypes.all = it } }
    }
    override fun onCleared() { repo.db.invalidationTracker.removeObserver(observer) }

    /** Satu snapshot per jendela 10 menit, simpan 30 terakhir. */
    private fun maybeSnapshot() {
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("last_snapshot", 0L) < 10 * 60 * 1000L) return
        prefs.edit().putLong("last_snapshot", now).apply()
        viewModelScope.launch(Dispatchers.IO) { try { Snapshots.save(getApplication(), Backup.export(repo.db)) } catch (e: Exception) {} }
    }

    fun exportBackup(onJson: (String) -> Unit) = viewModelScope.launch { onJson(withContext(Dispatchers.IO) { Backup.export(repo.db) }) }

    fun markBackedUp() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong("last_backup", now).putInt("changes_since_backup", 0).apply()
        lastBackup.value = now; changesSinceBackup.value = 0
    }

    /** onDone(null) = berhasil, selain itu pesan error. Data lama tidak disentuh bila file tidak valid. */
    fun restore(json: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        val err = try { withContext(Dispatchers.IO) { Backup.restore(repo.db, json) }; null } catch (e: Exception) { e.message ?: "File backup tidak valid." }
        if (err == null) markBackedUp()
        onDone(err)
    }
}
