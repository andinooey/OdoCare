package com.odocare.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey val id: String = newId(), val name: String, val type: String,
    val brand: String = "", val model: String = "", val year: Int? = null, val plate: String = "",
    val currentKm: Long? = null, val lastOdoAt: Long? = null, val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "odometer_logs",
    foreignKeys = [ForeignKey(Vehicle::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")])
data class OdometerLog(
    @PrimaryKey val id: String = newId(), val vehicleId: String, val km: Long,
    val recordedAt: Long, val note: String = ""
)

@Entity(tableName = "schedules",
    foreignKeys = [ForeignKey(Vehicle::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")])
data class MaintenanceSchedule(
    @PrimaryKey val id: String = newId(), val vehicleId: String, val name: String,
    val category: String = "Umum", val intervalKm: Long? = null, val intervalMonths: Int? = null,
    val lastKm: Long? = null, val lastEpochDay: Long? = null, val warnKm: Long = 500,
    val warnDays: Long = 14, val lastNotified: String? = null, val archived: Boolean = false
)

@Entity(tableName = "service_records",
    foreignKeys = [ForeignKey(Vehicle::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")])
data class ServiceRecord(
    @PrimaryKey val id: String = newId(), val vehicleId: String, val scheduleId: String?,
    val name: String, val epochDay: Long, val km: Long, val cost: Long = 0, val workshop: String = "",
    val notes: String = ""
)

@Entity(tableName = "documents",
    foreignKeys = [ForeignKey(Vehicle::class, ["id"], ["vehicleId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("vehicleId")])
data class LegalDocument(
    @PrimaryKey val id: String = newId(), val vehicleId: String? = null, val name: String,
    val type: String, val number: String = "", val dueEpochDay: Long? = null,
    val lastPaidEpochDay: Long? = null, val amount: Long? = null, val notes: String = "",
    val remAmount: Int = 30, val remUnit: String = "DAY", val remHour: Int = 8, val remMinute: Int = 0,
    val archived: Boolean = false
)

@Entity(tableName = "document_history",
    foreignKeys = [ForeignKey(LegalDocument::class, ["id"], ["documentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("documentId")])
data class DocumentHistory(
    @PrimaryKey val id: String = newId(), val documentId: String, val actionEpochDay: Long,
    val amount: Long? = null, val oldDueEpochDay: Long? = null, val newDueEpochDay: Long? = null
)

@Dao
interface OdoDao {
    @Query("SELECT * FROM vehicles WHERE archived = 0 ORDER BY createdAt") fun vehicles(): Flow<List<Vehicle>>
    @Query("SELECT * FROM vehicles WHERE archived = 0") suspend fun vehiclesNow(): List<Vehicle>
    @Upsert suspend fun upsertVehicle(v: Vehicle)
    @Query("UPDATE vehicles SET archived = 1 WHERE id = :id") suspend fun archiveVehicle(id: String)

    @Insert suspend fun insertLog(l: OdometerLog)
    @Query("SELECT * FROM odometer_logs ORDER BY recordedAt DESC") fun logs(): Flow<List<OdometerLog>>
    @Query("DELETE FROM odometer_logs WHERE id = :id") suspend fun deleteLog(id: String)

    @Query("SELECT * FROM schedules WHERE archived = 0") fun schedules(): Flow<List<MaintenanceSchedule>>
    @Query("SELECT * FROM schedules WHERE archived = 0 AND vehicleId = :vid") suspend fun schedulesFor(vid: String): List<MaintenanceSchedule>
    @Upsert suspend fun upsertSchedule(s: MaintenanceSchedule)
    @Query("UPDATE schedules SET archived = 1 WHERE id = :id") suspend fun archiveSchedule(id: String)

    @Insert suspend fun insertService(r: ServiceRecord)
    @Query("SELECT * FROM service_records ORDER BY epochDay DESC, km DESC") fun services(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM documents WHERE archived = 0") fun documents(): Flow<List<LegalDocument>>
    @Query("SELECT * FROM documents WHERE archived = 0") suspend fun documentsNow(): List<LegalDocument>
    @Upsert suspend fun upsertDocument(d: LegalDocument)
    @Query("UPDATE documents SET archived = 1 WHERE id = :id") suspend fun archiveDocument(id: String)
    @Insert suspend fun insertDocHistory(h: DocumentHistory)
    @Query("SELECT * FROM document_history ORDER BY actionEpochDay DESC") fun docHistory(): Flow<List<DocumentHistory>>
}

@Database(entities = [Vehicle::class, OdometerLog::class, MaintenanceSchedule::class, ServiceRecord::class,
    LegalDocument::class, DocumentHistory::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): OdoDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "odocare.db").build().also { inst = it }
        }
    }
}
