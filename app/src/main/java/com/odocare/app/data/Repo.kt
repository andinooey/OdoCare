package com.odocare.app.data

import android.content.Context
import androidx.room.withTransaction
import com.odocare.app.domain.Catalog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class Repo(private val ctx: Context) {
    val db = AppDb.get(ctx)
    val prefs = ctx.getSharedPreferences("odocare", Context.MODE_PRIVATE)
    private val vDao = db.vehicleDao(); private val oDao = db.odometerDao()
    private val sDao = db.scheduleDao(); private val srDao = db.serviceRecordDao()
    private val dDao = db.documentDao(); private val dhDao = db.docHistoryDao(); private val mDao = db.masterDao(); private val vtDao = db.vehicleTypeDao(); private val dtDao = db.documentTypeDao()

    val vehicles: Flow<List<Vehicle>> = vDao.observeAll()
    val activeVehicle: Flow<Vehicle?> = vDao.observeActive()
    val masters: Flow<List<MasterCategory>> = mDao.observeAll()
    val vehicleTypes: Flow<List<VehicleType>> = vtDao.observeAll()
    val documentTypes: Flow<List<DocumentType>> = dtDao.observeAll()
    val notifications: Flow<List<AppNotification>> = db.appNotificationDao().observeAll()
    val unreadNotifications: Flow<Int> = db.appNotificationDao().observeUnread()
    suspend fun markNotificationRead(id: Long) = db.appNotificationDao().markRead(id)
    suspend fun markAllNotificationsRead() = db.appNotificationDao().markAllRead()
    suspend fun deleteNotification(id: Long) = db.appNotificationDao().deleteById(id)
    suspend fun clearNotifications() = db.appNotificationDao().deleteAll()
    val allOdometer: Flow<List<OdometerLog>> = oDao.observeAll()
    val allSchedules: Flow<List<MaintenanceSchedule>> = sDao.observeAllActive()
    val allRecords: Flow<List<ServiceRecord>> = srDao.observeAll()
    val allDocuments: Flow<List<LegalDocument>> = dDao.observeAll()

    fun odometer(vid: String): Flow<List<OdometerLog>> = oDao.observeForVehicle(vid)
    fun latestOdometer(vid: String): Flow<Int?> = oDao.observeLatest(vid)
    fun schedules(vid: String): Flow<List<MaintenanceSchedule>> = sDao.observeForVehicle(vid)
    fun documents(vid: String): Flow<List<LegalDocument>> = dDao.observeForVehicle(vid)
    fun records(vid: String): Flow<List<ServiceRecord>> = srDao.observeForVehicle(vid)

    /** Isi master awal sekali saja (id seed tetap, jadi aman bila terpanggil dua kali). */
    suspend fun seedMasterIfNeeded() {
        if (prefs.getBoolean("master_seeded", false)) return
        Catalog.seed().forEach { mDao.insert(it) }
        prefs.edit().putBoolean("master_seeded", true).apply()
    }

    /** Isi jenis kendaraan bawaan sekali saja. IGNORE: baris yang sudah ada (mis. hasil restore) tidak ditimpa. */
    suspend fun seedVehicleTypesIfNeeded() {
        if (prefs.getBoolean("vtype_seeded", false)) return
        Catalog.defaultTypes().forEach { vtDao.insertIgnore(it) }
        prefs.edit().putBoolean("vtype_seeded", true).apply()
    }

    /** Isi jenis dokumen bawaan sekali saja. IGNORE: baris yang sudah ada (mis. hasil restore) tidak ditimpa. */
    suspend fun seedDocTypesIfNeeded() {
        if (prefs.getBoolean("doctype_seeded", false)) return
        Catalog.defaultDocTypes().forEach { dtDao.insertIgnore(it) }
        prefs.edit().putBoolean("doctype_seeded", true).apply()
    }

    /** Sekali saja: pengaturan notifikasi dokumen yang dulu global (Pengaturan) disalin ke tiap dokumen yang sudah ada. */
    suspend fun migrateDocNotifPrefs() {
        if (prefs.getBoolean("doc_notif_migrated", false)) return
        if (prefs.contains("doc_reminder_enabled") || prefs.contains("doc_reminder_hour")) {
            dDao.setAllNotify(prefs.getBoolean("doc_reminder_enabled", true), prefs.getInt("doc_reminder_hour", 9), prefs.getInt("doc_reminder_minute", 0))
        }
        prefs.edit().putBoolean("doc_notif_migrated", true).apply()
    }

    private fun dayMillis(d: LocalDate) = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * Kendaraan baru + odometer awal. Jadwal servis (dari master sesuai jenisnya) dan dokumen default
     * hanya dibuat bila switch-nya aktif di form tambah kendaraan.
     */
    suspend fun addVehicleWithDefaults(v: Vehicle, autoSchedules: Boolean = true, autoDocs: Boolean = true) {
        if (autoSchedules) seedMasterIfNeeded()
        db.withTransaction {
            vDao.insert(if (vDao.count() == 0) v.copy(isActive = true) else v)
            if (v.initialKm > 0) oDao.insert(OdometerLog(vehicleId = v.id, km = v.initialKm))
            val now = System.currentTimeMillis()
            if (autoSchedules) {
                mDao.all().filter { it.vehicleType == "ALL" || it.vehicleType == v.type }.forEach { m ->
                    sDao.insert(MaintenanceSchedule(vehicleId = v.id, name = m.name, intervalKm = m.defaultKm.takeIf { it > 0 }, intervalMonths = m.defaultMonths.takeIf { it > 0 }, lastServiceKm = v.initialKm, lastServiceDate = now,
                        notifyEnabled = m.notifyEnabled, notifyHour = m.notifyHour, notifyMinute = m.notifyMinute, warnKm = m.warnKm, warnDays = m.warnDays))
                }
            }
            if (autoDocs) {
                val today = LocalDate.now()
                // Tanggal jatuh tempo hanya perkiraan; user mengisi tanggal perpanjang terakhir di form dokumen
                dDao.insert(LegalDocument(vehicleId = v.id, type = "PAJAK_TAHUNAN", expiryDate = dayMillis(today.plusYears(1)), intervalValue = 1, intervalUnit = "YEAR"))
                dDao.insert(LegalDocument(vehicleId = v.id, type = "GANTI_PLAT", expiryDate = dayMillis(today.plusYears(5)), intervalValue = 5, intervalUnit = "YEAR"))
            }
        }
    }

    suspend fun updateVehicle(v: Vehicle) = vDao.update(v)
    suspend fun setActive(id: String) = db.withTransaction { vDao.clearActive(); vDao.setActive(id) }
    suspend fun deleteVehicle(id: String) = db.withTransaction {
        srDao.imagePathsForVehicle(id).forEach { ImageStore.delete(it) }
        dDao.attachmentPathsForVehicle(id).forEach { ImageStore.delete(it) }
        vDao.deleteById(id)
        if (vDao.activeId() == null) { vDao.firstId()?.let { vDao.setActive(it) } }
    }

    suspend fun addOdometer(log: OdometerLog) = oDao.insert(log)
    suspend fun updateOdometer(log: OdometerLog) = oDao.update(log)
    suspend fun deleteOdometer(id: String) = oDao.deleteById(id)

    suspend fun addSchedule(s: MaintenanceSchedule) = sDao.insert(s)
    suspend fun updateSchedule(s: MaintenanceSchedule) { sDao.update(s); prefs.edit().remove("svc_sent_${s.id}").apply() }
    suspend fun deleteSchedule(id: String) = sDao.deleteById(id)

    /** Simpan servis. Odometer ikut naik bila KM servis lebih besar dari KM terakhir (tidak pernah diturunkan otomatis). */
    suspend fun recordService(schedule: MaintenanceSchedule, km: Int, dateMillis: Long, cost: Long, notes: String?, imagePath: String?) = db.withTransaction {
        bumpOdometer(schedule.vehicleId, km)
        srDao.insert(ServiceRecord(vehicleId = schedule.vehicleId, scheduleId = schedule.id, title = schedule.name, km = km, date = dateMillis, cost = cost, notes = notes, imagePath = imagePath))
        sDao.update(schedule.copy(lastServiceKm = km, lastServiceDate = dateMillis))
        prefs.edit().remove("svc_sent_${schedule.id}").apply()
    }
    private suspend fun bumpOdometer(vehicleId: String, km: Int) {
        val current = oDao.latestKm(vehicleId)
        if (current == null || km > current) oDao.insert(OdometerLog(vehicleId = vehicleId, km = km))
    }
    suspend fun updateRecord(r: ServiceRecord) = db.withTransaction { srDao.update(r); bumpOdometer(r.vehicleId, r.km) }
    suspend fun deleteRecord(id: String) = srDao.deleteById(id)

    suspend fun addDocument(d: LegalDocument) = dDao.insert(d)
    suspend fun updateDocument(d: LegalDocument) { dDao.update(d); prefs.edit().remove("doc_handled_${d.id}").apply() }
    suspend fun renewDocument(doc: LegalDocument, renewedDate: Long, newExpiry: Long) = db.withTransaction {
        dhDao.insert(DocumentHistory(documentId = doc.id, oldExpiry = doc.expiryDate, newExpiry = newExpiry, renewedAt = renewedDate))
        dDao.update(doc.copy(expiryDate = newExpiry, lastRenewedDate = renewedDate))
        prefs.edit().remove("doc_handled_${doc.id}").apply()
    }
    suspend fun deleteDocument(id: String) = dDao.deleteById(id)

    suspend fun addMaster(m: MasterCategory) = mDao.insert(m)
    suspend fun updateMaster(m: MasterCategory) = mDao.update(m)
    suspend fun deleteMaster(id: String) = mDao.deleteById(id)

    // ---- Master jenis kendaraan ----
    suspend fun addVehicleType(t: VehicleType) = vtDao.insert(t)
    suspend fun updateVehicleType(t: VehicleType) = vtDao.update(t)
    /** false = masih dipakai kendaraan, tidak dihapus. Master kategori jenis itu ikut dihapus. */
    suspend fun deleteVehicleType(code: String): Boolean = db.withTransaction {
        if (vDao.countByType(code) > 0) false else { mDao.deleteByType(code); vtDao.deleteByCode(code); true }
    }

    // ---- Master jenis dokumen ----
    suspend fun addDocType(t: DocumentType) = dtDao.insert(t)
    suspend fun updateDocType(t: DocumentType) = dtDao.update(t)
    /** false = masih dipakai dokumen, tidak dihapus. */
    suspend fun deleteDocType(code: String): Boolean = db.withTransaction {
        if (dDao.countByType(code) > 0) false else { dtDao.deleteByCode(code); true }
    }

    suspend fun allDocuments(): List<LegalDocument> = dDao.all()
    suspend fun allActiveSchedules(): List<MaintenanceSchedule> = sDao.allActive()
}
