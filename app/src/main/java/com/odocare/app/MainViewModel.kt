package com.odocare.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.InvalidationTracker
import com.odocare.app.data.*
import com.odocare.app.notif.Notif
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

    // Semua kendaraan (Dashboard & Laporan)
    val masters: StateFlow<List<MasterCategory>> = repo.masters.state(emptyList())
    val allSchedules: StateFlow<List<MaintenanceSchedule>> = repo.allSchedules.state(emptyList())
    val allDocuments: StateFlow<List<LegalDocument>> = repo.allDocuments.state(emptyList())
    val allRecords: StateFlow<List<ServiceRecord>> = repo.allRecords.state(emptyList())
    val allOdometer: StateFlow<List<OdometerLog>> = repo.allOdometer.state(emptyList())

    // ---- Kendaraan ----
    fun addVehicle(name: String, plate: String, type: String, initialKm: Int) = viewModelScope.launch {
        if (name.isNotBlank()) repo.addVehicleWithDefaults(Vehicle(name = name.trim(), plate = plate.trim().uppercase(), type = type, initialKm = initialKm))
    }
    fun updateVehicle(v: Vehicle) = viewModelScope.launch { repo.updateVehicle(v) }
    fun setActiveVehicle(id: String) = viewModelScope.launch { repo.setActive(id) }
    fun deleteVehicle(id: String) = viewModelScope.launch { repo.deleteVehicle(id) }

    // ---- Odometer ----
    fun addOdometer(km: Int) = viewModelScope.launch { activeVehicle.value?.let { if (km > 0) repo.addOdometer(OdometerLog(vehicleId = it.id, km = km)) } }
    fun updateOdometer(log: OdometerLog) = viewModelScope.launch { repo.updateOdometer(log) }
    fun deleteOdometer(id: String) = viewModelScope.launch { repo.deleteOdometer(id) }

    // ---- Jadwal / kategori servis ----
    fun addSchedule(name: String, intervalKm: Int?, intervalMonths: Int?, lastKm: Int?, lastDate: Long) = viewModelScope.launch {
        activeVehicle.value?.let { if (name.isNotBlank()) repo.addSchedule(MaintenanceSchedule(vehicleId = it.id, name = name.trim(), intervalKm = intervalKm, intervalMonths = intervalMonths, lastServiceKm = lastKm ?: currentKm.value, lastServiceDate = lastDate)) }
    }
    fun updateSchedule(s: MaintenanceSchedule) = viewModelScope.launch { repo.updateSchedule(s) }
    fun deleteSchedule(id: String) = viewModelScope.launch { repo.deleteSchedule(id) }
    fun recordService(schedule: MaintenanceSchedule, km: Int, dateMillis: Long, cost: Long, notes: String?) = viewModelScope.launch { if (km > 0) repo.recordService(schedule, km, dateMillis, cost, notes) }

    // ---- Riwayat servis ----
    fun updateRecord(r: ServiceRecord) = viewModelScope.launch { repo.updateRecord(r) }
    fun deleteRecord(id: String) = viewModelScope.launch { repo.deleteRecord(id) }

    // ---- Dokumen ----
    fun addDocument(type: String, expiryMillis: Long, number: String?) = viewModelScope.launch { activeVehicle.value?.let { repo.addDocument(LegalDocument(vehicleId = it.id, type = type, number = number, expiryDate = expiryMillis)) } }
    fun updateDocument(d: LegalDocument) = viewModelScope.launch { repo.updateDocument(d) }
    fun renewDocument(doc: LegalDocument, newExpiry: Long) = viewModelScope.launch { repo.renewDocument(doc, newExpiry) }
    fun deleteDocument(id: String) = viewModelScope.launch { repo.deleteDocument(id) }

    // ---- Master kategori (tidak mengubah kendaraan yang sudah ada) ----
    fun addMaster(name: String, type: String, km: Int, months: Int) = viewModelScope.launch { if (name.isNotBlank()) repo.addMaster(MasterCategory(name = name.trim(), vehicleType = type, defaultKm = km, defaultMonths = months)) }
    fun updateMaster(m: MasterCategory) = viewModelScope.launch { repo.updateMaster(m) }
    fun deleteMaster(id: String) = viewModelScope.launch { repo.deleteMaster(id) }

    // ---- Notifikasi ----
    fun testNotification() = Notif.show(getApplication(), Notif.CH_SERVICE, "Uji Notifikasi", "Notifikasi OdoCare berfungsi.")

    // ---- Backup, perubahan sejak backup, snapshot otomatis ----
    val lastBackup = MutableStateFlow(prefs.getLong("last_backup", 0L))
    val changesSinceBackup = MutableStateFlow(prefs.getInt("changes_since_backup", 0))

    private val observer = object : InvalidationTracker.Observer(arrayOf("vehicles", "odometer_logs", "maintenance_schedules", "service_records", "legal_documents", "document_history", "master_categories")) {
        override fun onInvalidated(tables: Set<String>) {
            val n = prefs.getInt("changes_since_backup", 0) + 1
            prefs.edit().putInt("changes_since_backup", n).apply()
            changesSinceBackup.value = n
            maybeSnapshot()
        }
    }
    init { repo.db.invalidationTracker.addObserver(observer) }
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
