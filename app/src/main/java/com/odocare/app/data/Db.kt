package com.odocare.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, val type: String, val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val plate: String = "", val initialKm: Int = 0
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
    val archived: Boolean = false, val createdAt: Long = System.currentTimeMillis(),
    // Notifikasi per kategori: hidup/mati + jam tampil
    val notifyEnabled: Boolean = true, val notifyHour: Int = 9, val notifyMinute: Int = 0
)

@Entity(tableName = "service_records", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class ServiceRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val scheduleId: String?, val title: String,
    val km: Int, val date: Long, val cost: Long = 0L, val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Path foto bukti servis (disalin ke penyimpanan internal aplikasi)
    val imagePath: String? = null
)

@Entity(tableName = "legal_documents", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class LegalDocument(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val type: String, val number: String? = null,
    val expiryDate: Long, val notes: String? = null, val createdAt: Long = System.currentTimeMillis(),
    // Jatuh tempo = lastRenewedDate + interval (jika keduanya diisi)
    val lastRenewedDate: Long? = null, val intervalValue: Int? = null, val intervalUnit: String? = null,
    // Pengingat notifikasi per dokumen, mis. "30:DAY,15:DAY,1:MONTH"
    val reminders: String = "30:DAY,15:DAY"
)

@Entity(tableName = "document_history", foreignKeys = [ForeignKey(entity = LegalDocument::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.CASCADE)], indices = [Index("documentId")])
data class DocumentHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String, val oldExpiry: Long, val newExpiry: Long,
    val renewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "master_categories")
data class MasterCategory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, val vehicleType: String = "ALL", // MOTOR / MOBIL / ALL
    val defaultKm: Int = 0, val defaultMonths: Int = 0,
    val notifyEnabled: Boolean = true, val notifyHour: Int = 9, val notifyMinute: Int = 0
)

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt ASC") fun observeAll(): Flow<List<Vehicle>>
    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1") fun observeActive(): Flow<Vehicle?>
    @Query("SELECT COUNT(*) FROM vehicles") suspend fun count(): Int
    @Query("SELECT * FROM vehicles") suspend fun all(): List<Vehicle>
    @Query("DELETE FROM vehicles") suspend fun deleteAll()
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
    @Query("SELECT km FROM odometer_logs WHERE vehicleId = :vid ORDER BY recordedAt DESC LIMIT 1") suspend fun latestKm(vid: String): Int?
    @Query("SELECT * FROM odometer_logs") fun observeAll(): Flow<List<OdometerLog>>
    @Query("SELECT * FROM odometer_logs") suspend fun all(): List<OdometerLog>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(log: OdometerLog)
    @Update suspend fun update(log: OdometerLog)
    @Query("DELETE FROM odometer_logs WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vid AND archived = 0 ORDER BY createdAt DESC") fun observeForVehicle(vid: String): Flow<List<MaintenanceSchedule>>
    @Query("SELECT * FROM maintenance_schedules WHERE archived = 0") suspend fun allActive(): List<MaintenanceSchedule>
    @Query("SELECT * FROM maintenance_schedules WHERE archived = 0") fun observeAllActive(): Flow<List<MaintenanceSchedule>>
    @Query("SELECT * FROM maintenance_schedules") suspend fun all(): List<MaintenanceSchedule>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(s: MaintenanceSchedule)
    @Update suspend fun update(s: MaintenanceSchedule)
    @Query("DELETE FROM maintenance_schedules WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records WHERE vehicleId = :vid ORDER BY date DESC") fun observeForVehicle(vid: String): Flow<List<ServiceRecord>>
    @Query("SELECT imagePath FROM service_records WHERE vehicleId = :vid AND imagePath IS NOT NULL") suspend fun imagePathsForVehicle(vid: String): List<String>
    @Query("SELECT * FROM service_records") fun observeAll(): Flow<List<ServiceRecord>>
    @Query("SELECT * FROM service_records") suspend fun all(): List<ServiceRecord>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(r: ServiceRecord)
    @Update suspend fun update(r: ServiceRecord)
    @Query("DELETE FROM service_records WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM legal_documents WHERE vehicleId = :vid ORDER BY expiryDate ASC") fun observeForVehicle(vid: String): Flow<List<LegalDocument>>
    @Query("SELECT * FROM legal_documents") suspend fun all(): List<LegalDocument>
    @Query("SELECT * FROM legal_documents ORDER BY expiryDate ASC") fun observeAll(): Flow<List<LegalDocument>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(d: LegalDocument)
    @Update suspend fun update(d: LegalDocument)
    @Query("DELETE FROM legal_documents WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface DocHistoryDao {
    @Query("SELECT * FROM document_history WHERE documentId = :did ORDER BY renewedAt DESC") fun observeForDocument(did: String): Flow<List<DocumentHistory>>
    @Query("SELECT * FROM document_history") suspend fun all(): List<DocumentHistory>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(h: DocumentHistory)
}

@Dao
interface MasterDao {
    @Query("SELECT * FROM master_categories ORDER BY vehicleType ASC, name ASC") fun observeAll(): Flow<List<MasterCategory>>
    @Query("SELECT * FROM master_categories") suspend fun all(): List<MasterCategory>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(m: MasterCategory)
    @Update suspend fun update(m: MasterCategory)
    @Query("DELETE FROM master_categories WHERE id = :id") suspend fun deleteById(id: String)
    @Query("DELETE FROM master_categories") suspend fun deleteAll()
}

@Database(entities = [Vehicle::class, OdometerLog::class, MaintenanceSchedule::class, ServiceRecord::class, LegalDocument::class, DocumentHistory::class, MasterCategory::class], version = 4, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerDao(): OdometerDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun documentDao(): DocumentDao
    abstract fun docHistoryDao(): DocHistoryDao
    abstract fun masterDao(): MasterDao
    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun get(ctx: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "odocare.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE vehicles ADD COLUMN plate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE vehicles ADD COLUMN initialKm INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS master_categories (id TEXT NOT NULL, name TEXT NOT NULL, vehicleType TEXT NOT NULL, defaultKm INTEGER NOT NULL, defaultMonths INTEGER NOT NULL, PRIMARY KEY(id))")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN lastRenewedDate INTEGER")
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN intervalValue INTEGER")
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN intervalUnit TEXT")
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN reminders TEXT NOT NULL DEFAULT '30:DAY,15:DAY'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE maintenance_schedules ADD COLUMN notifyEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE maintenance_schedules ADD COLUMN notifyHour INTEGER NOT NULL DEFAULT 9")
        db.execSQL("ALTER TABLE maintenance_schedules ADD COLUMN notifyMinute INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE master_categories ADD COLUMN notifyEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE master_categories ADD COLUMN notifyHour INTEGER NOT NULL DEFAULT 9")
        db.execSQL("ALTER TABLE master_categories ADD COLUMN notifyMinute INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE service_records ADD COLUMN imagePath TEXT")
    }
}
