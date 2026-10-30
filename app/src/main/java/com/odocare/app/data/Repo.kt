package com.odocare.app.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class Repo(app: Context) {
    private val db = AppDb.get(app)
    private val vDao = db.vehicleDao(); private val oDao = db.odometerDao()
    private val sDao = db.scheduleDao(); private val srDao = db.serviceRecordDao()
    private val dDao = db.documentDao(); private val dhDao = db.docHistoryDao()

    val vehicles: Flow<List<Vehicle>> = vDao.observeAll()
    val activeVehicle: Flow<Vehicle?> = vDao.observeActive()

    fun odometer(vid: String): Flow<List<OdometerLog>> = oDao.observeForVehicle(vid)
    fun latestOdometer(vid: String): Flow<Int?> = oDao.observeLatest(vid)
    fun schedules(vid: String): Flow<List<MaintenanceSchedule>> = sDao.observeForVehicle(vid)
    fun documents(vid: String): Flow<List<LegalDocument>> = dDao.observeForVehicle(vid)

    suspend fun addVehicle(v: Vehicle) { if (vDao.count() == 0) vDao.insert(v.copy(isActive = true)) else vDao.insert(v) }
    suspend fun updateVehicle(v: Vehicle) = vDao.update(v)
    suspend fun setActive(id: String) { vDao.clearActive(); vDao.setActive(id) }
    suspend fun deleteVehicle(id: String) = db.withTransaction {
        vDao.deleteById(id)
        if (vDao.activeId() == null) { vDao.firstId()?.let { vDao.setActive(it) } }
    }

    suspend fun addOdometer(log: OdometerLog) = oDao.insert(log)
    suspend fun updateOdometer(log: OdometerLog) = oDao.update(log)
    suspend fun deleteOdometer(id: String) = oDao.deleteById(id)

    suspend fun addSchedule(s: MaintenanceSchedule) = sDao.insert(s)
    suspend fun updateSchedule(s: MaintenanceSchedule) = sDao.update(s)
    suspend fun deleteSchedule(id: String) = sDao.deleteById(id)

    suspend fun recordService(schedule: MaintenanceSchedule, km: Int, dateMillis: Long, cost: Long, notes: String?) = db.withTransaction {
        oDao.insert(OdometerLog(vehicleId = schedule.vehicleId, km = km, recordedAt = dateMillis))
        srDao.insert(ServiceRecord(vehicleId = schedule.vehicleId, scheduleId = schedule.id, title = schedule.name, km = km, date = dateMillis, cost = cost, notes = notes))
        sDao.update(schedule.copy(lastServiceKm = km, lastServiceDate = dateMillis))
    }

    suspend fun addDocument(d: LegalDocument) = dDao.insert(d)
    suspend fun updateDocument(d: LegalDocument) = dDao.update(d)
    suspend fun renewDocument(doc: LegalDocument, newExpiry: Long) = db.withTransaction {
        dhDao.insert(DocumentHistory(documentId = doc.id, oldExpiry = doc.expiryDate, newExpiry = newExpiry))
        dDao.update(doc.copy(expiryDate = newExpiry))
    }
    suspend fun deleteDocument(id: String) = dDao.deleteById(id)
    suspend fun allDocuments(): List<LegalDocument> = dDao.all()
    suspend fun allActiveSchedules(): List<MaintenanceSchedule> = sDao.allActive()
}
