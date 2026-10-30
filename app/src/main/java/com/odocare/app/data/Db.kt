package com.odocare.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, val type: String, val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "odometer_logs", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class OdometerLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val km: Int, val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "maintenance_schedules", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class MaintenanceSchedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val name: String,
    val intervalKm: Int? = null, val intervalMonths: Int? = null,
    val lastServiceKm: Int? = null, val lastServiceDate: Long? = null,
    val archived: Boolean = false, val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "service_records", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class ServiceRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val scheduleId: String?, val title: String,
    val km: Int, val date: Long, val cost: Long = 0L, val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "legal_documents", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class LegalDocument(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val type: String, val number: String? = null,
    val expiryDate: Long, val notes: String? = null, val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "document_history", foreignKeys = [ForeignKey(entity = LegalDocument::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.CASCADE)], indices = [Index("documentId")])
data class DocumentHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String, val oldExpiry: Long, val newExpiry: Long,
    val renewedAt: Long = System.currentTimeMillis()
)

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt ASC") fun observeAll(): Flow<List<Vehicle>>
    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1") fun observeActive(): Flow<Vehicle?>
    @Query("SELECT COUNT(*) FROM vehicles") suspend fun count(): Int
    @Query("SELECT id FROM vehicles WHERE isActive = 1 LIMIT 1") suspend fun activeId(): String?
    @Query("SELECT id FROM vehicles ORDER BY createdAt ASC LIMIT 1") suspend fun firstId(): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(v: Vehicle)
    @Update suspend fun update(v: Vehicle)
    @Query("UPDATE vehicles SET isActive = 0") suspend fun clearActive()
    @Query("UPDATE vehicles SET isActive = 1 WHERE id = :id") suspend fun setActive(id: String)
    @Query("DELETE FROM vehicles WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_logs WHERE vehicleId = :vid ORDER BY recordedAt DESC") fun observeForVehicle(vid: String): Flow<List<OdometerLog>>
    @Query("SELECT km FROM odometer_logs WHERE vehicleId = :vid ORDER BY recordedAt DESC LIMIT 1") fun observeLatest(vid: String): Flow<Int?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(log: OdometerLog)
    @Update suspend fun update(log: OdometerLog)
    @Query("DELETE FROM odometer_logs WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vid AND archived = 0 ORDER BY createdAt DESC") fun observeForVehicle(vid: String): Flow<List<MaintenanceSchedule>>
    @Query("SELECT * FROM maintenance_schedules WHERE archived = 0") suspend fun allActive(): List<MaintenanceSchedule>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(s: MaintenanceSchedule)
    @Update suspend fun update(s: MaintenanceSchedule)
    @Query("DELETE FROM maintenance_schedules WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface ServiceRecordDao { @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(r: ServiceRecord) }

@Dao
interface DocumentDao {
    @Query("SELECT * FROM legal_documents WHERE vehicleId = :vid ORDER BY expiryDate ASC") fun observeForVehicle(vid: String): Flow<List<LegalDocument>>
    @Query("SELECT * FROM legal_documents") suspend fun all(): List<LegalDocument>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(d: LegalDocument)
    @Update suspend fun update(d: LegalDocument)
    @Query("DELETE FROM legal_documents WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface DocHistoryDao {
    @Query("SELECT * FROM document_history WHERE documentId = :did ORDER BY renewedAt DESC") fun observeForDocument(did: String): Flow<List<DocumentHistory>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(h: DocumentHistory)
}

@Database(entities = [Vehicle::class, OdometerLog::class, MaintenanceSchedule::class, ServiceRecord::class, LegalDocument::class, DocumentHistory::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerDao(): OdometerDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun documentDao(): DocumentDao
    abstract fun docHistoryDao(): DocHistoryDao
    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun get(ctx: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "odocare.db").fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
