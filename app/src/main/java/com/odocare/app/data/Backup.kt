package com.odocare.app.data

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private fun JSONObject.intOrNull(k: String): Int? = if (isNull(k)) null else getInt(k)
private fun JSONObject.longOrNull(k: String): Long? = if (isNull(k)) null else getLong(k)
private fun JSONObject.strOrNull(k: String): String? = if (isNull(k)) null else getString(k)
private fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }
private fun nz(v: Any?): Any = v ?: JSONObject.NULL

object Backup {
    suspend fun export(db: AppDb): String {
        val root = JSONObject()
        root.put("app", "odocare").put("version", 3).put("exportedAt", System.currentTimeMillis())
        root.put("vehicles", JSONArray().also { a -> db.vehicleDao().all().forEach { v ->
            a.put(JSONObject().put("id", v.id).put("name", v.name).put("type", v.type).put("isActive", v.isActive).put("createdAt", v.createdAt).put("plate", v.plate).put("initialKm", v.initialKm))
        } })
        root.put("odometer", JSONArray().also { a -> db.odometerDao().all().forEach { o ->
            a.put(JSONObject().put("id", o.id).put("vehicleId", o.vehicleId).put("km", o.km).put("recordedAt", o.recordedAt))
        } })
        root.put("schedules", JSONArray().also { a -> db.scheduleDao().all().forEach { s ->
            a.put(JSONObject().put("id", s.id).put("vehicleId", s.vehicleId).put("name", s.name).put("intervalKm", nz(s.intervalKm)).put("intervalMonths", nz(s.intervalMonths))
                .put("lastServiceKm", nz(s.lastServiceKm)).put("lastServiceDate", nz(s.lastServiceDate)).put("archived", s.archived).put("createdAt", s.createdAt)
                .put("notifyEnabled", s.notifyEnabled).put("notifyHour", s.notifyHour).put("notifyMinute", s.notifyMinute))
        } })
        root.put("records", JSONArray().also { a -> db.serviceRecordDao().all().forEach { r ->
            a.put(JSONObject().put("id", r.id).put("vehicleId", r.vehicleId).put("scheduleId", nz(r.scheduleId)).put("title", r.title).put("km", r.km).put("date", r.date).put("cost", r.cost).put("notes", nz(r.notes)).put("createdAt", r.createdAt).put("imagePath", nz(r.imagePath)))
        } })
        root.put("documents", JSONArray().also { a -> db.documentDao().all().forEach { d ->
            a.put(JSONObject().put("id", d.id).put("vehicleId", d.vehicleId).put("type", d.type).put("number", nz(d.number)).put("expiryDate", d.expiryDate).put("notes", nz(d.notes)).put("createdAt", d.createdAt)
                .put("lastRenewedDate", nz(d.lastRenewedDate)).put("intervalValue", nz(d.intervalValue)).put("intervalUnit", nz(d.intervalUnit)).put("reminders", d.reminders))
        } })
        root.put("documentHistory", JSONArray().also { a -> db.docHistoryDao().all().forEach { h ->
            a.put(JSONObject().put("id", h.id).put("documentId", h.documentId).put("oldExpiry", h.oldExpiry).put("newExpiry", h.newExpiry).put("renewedAt", h.renewedAt))
        } })
        root.put("masters", JSONArray().also { a -> db.masterDao().all().forEach { m ->
            a.put(JSONObject().put("id", m.id).put("name", m.name).put("vehicleType", m.vehicleType).put("defaultKm", m.defaultKm).put("defaultMonths", m.defaultMonths)
                .put("notifyEnabled", m.notifyEnabled).put("notifyHour", m.notifyHour).put("notifyMinute", m.notifyMinute))
        } })
        return root.toString(2)
    }

    /** Parse dulu seluruhnya; data lama baru dihapus jika file valid. Melempar Exception bila file tidak valid. */
    suspend fun restore(db: AppDb, json: String) {
        val root = JSONObject(json.trim().removePrefix("\uFEFF"))
        require(root.optString("app") == "odocare") { "Bukan file backup OdoCare." }

        val vehicles = root.getJSONArray("vehicles").mapObjects { Vehicle(it.getString("id"), it.getString("name"), it.getString("type"), it.optBoolean("isActive", false), it.optLong("createdAt", System.currentTimeMillis()), it.optString("plate", ""), it.optInt("initialKm", 0)) }
        val odometer = root.getJSONArray("odometer").mapObjects { OdometerLog(it.getString("id"), it.getString("vehicleId"), it.getInt("km"), it.getLong("recordedAt")) }
        val schedules = root.getJSONArray("schedules").mapObjects { MaintenanceSchedule(it.getString("id"), it.getString("vehicleId"), it.getString("name"), it.intOrNull("intervalKm"), it.intOrNull("intervalMonths"), it.intOrNull("lastServiceKm"), it.longOrNull("lastServiceDate"), it.optBoolean("archived", false), it.optLong("createdAt", System.currentTimeMillis()), it.optBoolean("notifyEnabled", true), it.optInt("notifyHour", 9), it.optInt("notifyMinute", 0)) }
        val records = root.getJSONArray("records").mapObjects { ServiceRecord(it.getString("id"), it.getString("vehicleId"), it.strOrNull("scheduleId"), it.getString("title"), it.getInt("km"), it.getLong("date"), it.optLong("cost", 0L), it.strOrNull("notes"), it.optLong("createdAt", System.currentTimeMillis()), it.strOrNull("imagePath")) }
        val documents = root.getJSONArray("documents").mapObjects { LegalDocument(id = it.getString("id"), vehicleId = it.getString("vehicleId"), type = it.getString("type"), number = it.strOrNull("number"), expiryDate = it.getLong("expiryDate"), notes = it.strOrNull("notes"), createdAt = it.optLong("createdAt", System.currentTimeMillis()), lastRenewedDate = it.longOrNull("lastRenewedDate"), intervalValue = it.intOrNull("intervalValue"), intervalUnit = it.strOrNull("intervalUnit"), reminders = it.optString("reminders", "30:DAY,15:DAY")) }
        val history = root.getJSONArray("documentHistory").mapObjects { DocumentHistory(it.getString("id"), it.getString("documentId"), it.getLong("oldExpiry"), it.getLong("newExpiry"), it.optLong("renewedAt", System.currentTimeMillis())) }
        val masters = root.getJSONArray("masters").mapObjects { MasterCategory(it.getString("id"), it.getString("name"), it.optString("vehicleType", "ALL"), it.optInt("defaultKm", 0), it.optInt("defaultMonths", 0), it.optBoolean("notifyEnabled", true), it.optInt("notifyHour", 9), it.optInt("notifyMinute", 0)) }

        db.withTransaction {
            db.vehicleDao().deleteAll()   // anak-anaknya ikut terhapus (CASCADE)
            db.masterDao().deleteAll()
            vehicles.forEach { db.vehicleDao().insert(it) }
            odometer.forEach { db.odometerDao().insert(it) }
            schedules.forEach { db.scheduleDao().insert(it) }
            records.forEach { db.serviceRecordDao().insert(it) }
            documents.forEach { db.documentDao().insert(it) }
            history.forEach { db.docHistoryDao().insert(it) }
            masters.forEach { db.masterDao().insert(it) }
        }
    }
}

/** Snapshot otomatis di penyimpanan internal aplikasi: maksimal 30 terakhir. */
object Snapshots {
    private const val MAX = 30
    private fun dir(ctx: Context) = File(ctx.filesDir, "snapshots").apply { mkdirs() }

    fun save(ctx: Context, json: String) {
        File(dir(ctx), "snap_${System.currentTimeMillis()}.json").writeText(json)
        list(ctx).drop(MAX).forEach { it.second.delete() }
    }

    /** (waktu, file) terbaru dulu */
    fun list(ctx: Context): List<Pair<Long, File>> =
        (dir(ctx).listFiles() ?: emptyArray()).mapNotNull { f -> f.name.removePrefix("snap_").removeSuffix(".json").toLongOrNull()?.let { it to f } }.sortedByDescending { it.first }
}
