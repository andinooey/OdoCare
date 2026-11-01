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
    val plate: String = "", val initialKm: Int = 0,
    // Harga BBM per liter untuk kendaraan ini (0 = belum diatur). Dipakai menghitung liter otomatis di form BBM.
    val fuelPricePerLiter: Long = 0L
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
    val notifyEnabled: Boolean = true, val notifyHour: Int = 9, val notifyMinute: Int = 0,
    // Batas "Segera" (juga pemicu notifikasi): sisa km / sisa hari. 0 = otomatis (20% interval km / 30 hari)
    val warnKm: Int = 0, val warnDays: Int = 0
)

@Entity(tableName = "service_records", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class ServiceRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val scheduleId: String?, val title: String,
    val km: Int, val date: Long, val cost: Long = 0L, val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Path foto bukti servis (disalin ke penyimpanan internal aplikasi)
    val imagePath: String? = null,
    // Rincian biaya: 0 dan 0 = tidak dirinci (cost = total yang diketik). Bila dirinci, cost = partsCost + laborCost.
    val partsCost: Long = 0L, val laborCost: Long = 0L
) {
    val hasBreakdown: Boolean get() = partsCost > 0 || laborCost > 0
}

/** Catatan isi BBM. Harga per liter tidak disimpan, dihitung dari totalPrice / liters. */
@Entity(tableName = "fuel_logs", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class FuelLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val date: Long, val km: Int,
    val liters: Double, val totalPrice: Long, val fullTank: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    // Harga per liter SAAT transaksi dibuat (snapshot). Tidak ikut berubah bila harga BBM kendaraan diubah kemudian.
    val literPrice: Long = 0L
) {
    /** Harga per liter transaksi ini: snapshot bila ada, selain itu dihitung dari total / liter. */
    val pricePerLiter: Long get() = if (literPrice > 0) literPrice else if (liters > 0) Math.round(totalPrice / liters) else 0L
}

@Entity(tableName = "legal_documents", foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("vehicleId")])
data class LegalDocument(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String, val type: String, val number: String? = null,
    val expiryDate: Long, val notes: String? = null, val createdAt: Long = System.currentTimeMillis(),
    // Jatuh tempo = lastRenewedDate + interval (jika keduanya diisi)
    val lastRenewedDate: Long? = null, val intervalValue: Int? = null, val intervalUnit: String? = null,
    // Pengingat notifikasi per dokumen, mis. "30:DAY,15:DAY,1:MONTH"
    val reminders: String = "30:DAY,15:DAY",
    // Notifikasi per dokumen: hidup/mati + jam tampil
    val notifyEnabled: Boolean = true, val notifyHour: Int = 9, val notifyMinute: Int = 0,
    // Lampiran foto / dokumen (disalin ke penyimpanan internal aplikasi)
    val attachmentPath: String? = null, val attachmentName: String? = null
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
    val notifyEnabled: Boolean = true, val notifyHour: Int = 9, val notifyMinute: Int = 0,
    val warnKm: Int = 0, val warnDays: Int = 0
)

/** Master jenis kendaraan (Motor, Mobil, dst). code dipakai di Vehicle.type dan MasterCategory.vehicleType. */
@Entity(tableName = "vehicle_types")
data class VehicleType(
    @PrimaryKey val code: String,
    val name: String, val icon: String = "CAR",
    val createdAt: Long = System.currentTimeMillis()
)

/** Master jenis dokumen (Pajak Tahunan, SIM, Asuransi, dst). code dipakai di LegalDocument.type; vehicleType: ALL atau kode jenis kendaraan. */
@Entity(tableName = "document_types")
data class DocumentType(
    @PrimaryKey val code: String,
    val name: String, val vehicleType: String = "ALL",
    // Masa berlaku bawaan (0 = tanpa bawaan), dipakai untuk mengisi form dokumen otomatis
    val defaultValue: Int = 0, val defaultUnit: String = "YEAR",
    val createdAt: Long = System.currentTimeMillis()
)

/** Riwayat notifikasi yang sudah muncul. isRead = false berarti belum dibuka di layar Notifikasi. */
@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channel: String, val title: String, val body: String,
    val createdAt: Long = System.currentTimeMillis(), val isRead: Boolean = false,
    /** Tujuan saat diketuk: "service", "documents", atau "odometer"; null = tidak ada. */
    val target: String? = null, val vehicleId: String? = null
)

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY createdAt DESC, id DESC") fun observeAll(): Flow<List<AppNotification>>
    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = 0") fun observeUnread(): Flow<Int>
    @Insert suspend fun insert(n: AppNotification)
    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id") suspend fun markRead(id: Long)
    @Query("UPDATE app_notifications SET isRead = 1") suspend fun markAllRead()
    @Query("DELETE FROM app_notifications WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM app_notifications") suspend fun deleteAll()
    /** Simpan paling banyak 200 notifikasi terbaru. */
    @Query("DELETE FROM app_notifications WHERE id NOT IN (SELECT id FROM app_notifications ORDER BY createdAt DESC, id DESC LIMIT 200)") suspend fun trim()
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt ASC") fun observeAll(): Flow<List<Vehicle>>
    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1") fun observeActive(): Flow<Vehicle?>
    @Query("SELECT COUNT(*) FROM vehicles") suspend fun count(): Int
    @Query("SELECT COUNT(*) FROM vehicles WHERE type = :code") suspend fun countByType(code: String): Int
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
interface FuelDao {
    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vid ORDER BY date DESC, km DESC") fun observeForVehicle(vid: String): Flow<List<FuelLog>>
    @Query("SELECT * FROM fuel_logs") fun observeAll(): Flow<List<FuelLog>>
    @Query("SELECT * FROM fuel_logs") suspend fun all(): List<FuelLog>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(f: FuelLog)
    @Update suspend fun update(f: FuelLog)
    @Query("DELETE FROM fuel_logs WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM legal_documents WHERE vehicleId = :vid ORDER BY expiryDate ASC") fun observeForVehicle(vid: String): Flow<List<LegalDocument>>
    @Query("SELECT * FROM legal_documents") suspend fun all(): List<LegalDocument>
    @Query("SELECT attachmentPath FROM legal_documents WHERE vehicleId = :vid AND attachmentPath IS NOT NULL") suspend fun attachmentPathsForVehicle(vid: String): List<String>
    @Query("SELECT * FROM legal_documents ORDER BY expiryDate ASC") fun observeAll(): Flow<List<LegalDocument>>
    @Query("SELECT COUNT(*) FROM legal_documents WHERE type = :code") suspend fun countByType(code: String): Int
    @Query("UPDATE legal_documents SET notifyEnabled = :enabled, notifyHour = :hour, notifyMinute = :minute") suspend fun setAllNotify(enabled: Boolean, hour: Int, minute: Int)
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
    @Query("DELETE FROM master_categories WHERE vehicleType = :code") suspend fun deleteByType(code: String)
    @Query("DELETE FROM master_categories") suspend fun deleteAll()
}

@Dao
interface VehicleTypeDao {
    @Query("SELECT * FROM vehicle_types ORDER BY createdAt ASC, name ASC") fun observeAll(): Flow<List<VehicleType>>
    @Query("SELECT * FROM vehicle_types") suspend fun all(): List<VehicleType>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(t: VehicleType)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(t: VehicleType)
    @Update suspend fun update(t: VehicleType)
    @Query("DELETE FROM vehicle_types WHERE code = :code") suspend fun deleteByCode(code: String)
    @Query("DELETE FROM vehicle_types") suspend fun deleteAll()
}

@Dao
interface DocumentTypeDao {
    @Query("SELECT * FROM document_types ORDER BY createdAt ASC, name ASC") fun observeAll(): Flow<List<DocumentType>>
    @Query("SELECT * FROM document_types") suspend fun all(): List<DocumentType>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(t: DocumentType)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(t: DocumentType)
    @Update suspend fun update(t: DocumentType)
    @Query("DELETE FROM document_types WHERE code = :code") suspend fun deleteByCode(code: String)
    @Query("DELETE FROM document_types") suspend fun deleteAll()
}

@Database(entities = [Vehicle::class, OdometerLog::class, MaintenanceSchedule::class, ServiceRecord::class, LegalDocument::class, DocumentHistory::class, MasterCategory::class, VehicleType::class, DocumentType::class, AppNotification::class, FuelLog::class], version = 13, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerDao(): OdometerDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun documentDao(): DocumentDao
    abstract fun docHistoryDao(): DocHistoryDao
    abstract fun masterDao(): MasterDao
    abstract fun vehicleTypeDao(): VehicleTypeDao
    abstract fun documentTypeDao(): DocumentTypeDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun fuelDao(): FuelDao
    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun get(ctx: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "odocare.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13).build().also { INSTANCE = it }
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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE maintenance_schedules ADD COLUMN warnKm INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE maintenance_schedules ADD COLUMN warnDays INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE master_categories ADD COLUMN warnKm INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE master_categories ADD COLUMN warnDays INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN notifyEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN notifyHour INTEGER NOT NULL DEFAULT 9")
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN notifyMinute INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS vehicle_types (code TEXT NOT NULL, name TEXT NOT NULL, icon TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(code))")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN attachmentPath TEXT")
        db.execSQL("ALTER TABLE legal_documents ADD COLUMN attachmentName TEXT")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS document_types (code TEXT NOT NULL, name TEXT NOT NULL, vehicleType TEXT NOT NULL, defaultValue INTEGER NOT NULL, defaultUnit TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(code))")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS app_notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, channel TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, createdAt INTEGER NOT NULL, isRead INTEGER NOT NULL)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_notifications ADD COLUMN target TEXT")
        db.execSQL("ALTER TABLE app_notifications ADD COLUMN vehicleId TEXT")
    }
}

/** v11: rincian biaya servis (suku cadang + jasa) dan tabel catatan BBM. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE service_records ADD COLUMN partsCost INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE service_records ADD COLUMN laborCost INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS `fuel_logs` (`id` TEXT NOT NULL, `vehicleId` TEXT NOT NULL, `date` INTEGER NOT NULL, `km` INTEGER NOT NULL, `liters` REAL NOT NULL, `totalPrice` INTEGER NOT NULL, `fullTank` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_logs_vehicleId` ON `fuel_logs` (`vehicleId`)")
    }
}

/** v12: harga BBM per liter per kendaraan. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE vehicles ADD COLUMN fuelPricePerLiter INTEGER NOT NULL DEFAULT 0")
    }
}

/** v13: snapshot harga per liter di tiap catatan BBM. Catatan lama diisi dari total / liter agar tidak berubah. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE fuel_logs ADD COLUMN literPrice INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE fuel_logs SET literPrice = CAST(ROUND(totalPrice / liters) AS INTEGER) WHERE liters > 0")
    }
}
