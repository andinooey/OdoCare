package com.odocare.app.notif

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.*
import androidx.core.content.ContextCompat
import androidx.work.*
import com.odocare.app.MainActivity
import com.odocare.app.R
import com.odocare.app.data.AppDb
import com.odocare.app.domain.Engines
import com.odocare.app.domain.IntervalUnit
import com.odocare.app.domain.Reminders
import com.odocare.app.domain.ServiceStatus
import com.odocare.app.ui.fmtKm
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object Notif {
    const val CH_SERVICE = "odocare_service"; const val CH_DOCUMENTS = "odocare_documents"; const val CH_ODOMETER = "odocare_odometer"

    fun setupChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = ctx.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel(CH_SERVICE, "Pengingat Servis", NotificationManager.IMPORTANCE_DEFAULT))
            mgr.createNotificationChannel(NotificationChannel(CH_DOCUMENTS, "Dokumen", NotificationManager.IMPORTANCE_DEFAULT))
            mgr.createNotificationChannel(NotificationChannel(CH_ODOMETER, "Odometer", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun show(ctx: Context, channel: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(ctx, channel).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pi).build()
        try { NotificationManagerCompat.from(ctx).notify((title + body).hashCode(), notif) } catch (_: SecurityException) {}
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("odocare", Context.MODE_PRIVATE)

    // ---- Pengingat update KM: "setiap N hari/bulan/tahun" pada jam tertentu, bisa dimatikan ----
    fun kmReminderValue(ctx: Context): Int = prefs(ctx).getInt("km_reminder_value", 1)
    fun kmReminderUnit(ctx: Context): IntervalUnit = IntervalUnit.parse(prefs(ctx).getString("km_reminder_unit", "MONTH")) ?: IntervalUnit.MONTH
    fun kmReminderEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("km_reminder_enabled", kmReminderValue(ctx) > 0)
    fun kmReminderHour(ctx: Context): Int = prefs(ctx).getInt("km_reminder_hour", 9)
    fun kmReminderMinute(ctx: Context): Int = prefs(ctx).getInt("km_reminder_minute", 0)
    fun setKmReminder(ctx: Context, enabled: Boolean, value: Int, unit: IntervalUnit, hour: Int, minute: Int) {
        prefs(ctx).edit().putBoolean("km_reminder_enabled", enabled).putInt("km_reminder_value", value.coerceAtLeast(0))
            .putString("km_reminder_unit", unit.name).putInt("km_reminder_hour", hour).putInt("km_reminder_minute", minute).apply()
    }

    // ---- Pengingat pajak & dokumen: on/off + jam (jarak hari tiap dokumen tetap diatur di form dokumen) ----
    fun docReminderEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("doc_reminder_enabled", true)
    fun docReminderHour(ctx: Context): Int = prefs(ctx).getInt("doc_reminder_hour", 9)
    fun docReminderMinute(ctx: Context): Int = prefs(ctx).getInt("doc_reminder_minute", 0)
    fun setDocReminder(ctx: Context, enabled: Boolean, hour: Int, minute: Int) {
        prefs(ctx).edit().putBoolean("doc_reminder_enabled", enabled).putInt("doc_reminder_hour", hour).putInt("doc_reminder_minute", minute).apply()
    }

    /**
     * Worker berkala (tiap 15 menit, minimum WorkManager). Setiap pengingat punya jam sendiri; worker mengirim bila jamnya
     * sudah lewat dan belum terkirim hari itu, jadi tetap terkirim (agak telat) walau HP sempat Doze.
     */
    fun schedulePolling(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork("odocare_daily_reminder")   // jadwal lama (sekali sehari jam 09.00)
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork("odocare_reminder_poll", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private fun toDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val db = AppDb.get(ctx)
        val prefs = ctx.getSharedPreferences("odocare", Context.MODE_PRIVATE)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayEpoch = today.toEpochDay()
        val nowMinutes = now.hour * 60 + now.minute
        fun reached(hour: Int, minute: Int) = nowMinutes >= hour * 60 + minute
        val vehicles = db.vehicleDao().all()
        val vehicleNames = vehicles.associate { it.id to it.name }

        // 1) Pajak & dokumen: pengingat per dokumen (mis. 30 & 15 hari sebelum) + hari jatuh tempo, dikirim pada jam yang dipilih.
        //    Hari pemicu yang terlewat (HP mati) tetap dikejar maksimal 3 hari ke belakang, tiap pemicu hanya sekali.
        if (Notif.docReminderEnabled(ctx) && reached(Notif.docReminderHour(ctx), Notif.docReminderMinute(ctx))) {
            db.documentDao().all().forEach { d ->
                val expiry = toDate(d.expiryDate)
                val days = ChronoUnit.DAYS.between(today, expiry)
                if (days < 0) return@forEach
                val triggers = Reminders.parse(d.reminders).map { it.dateFor(expiry).toEpochDay() } + expiry.toEpochDay()
                val key = "doc_handled_${d.id}"
                val handled = prefs.getLong(key, Long.MIN_VALUE)
                val trigger = triggers.filter { it in (todayEpoch - 3)..todayEpoch && it > handled }.maxOrNull() ?: return@forEach
                val label = d.type.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } + (vehicleNames[d.vehicleId]?.let { " ($it)" } ?: "")
                if (days == 0L) Notif.show(ctx, Notif.CH_DOCUMENTS, "Jatuh tempo hari ini", "$label jatuh tempo hari ini")
                else Notif.show(ctx, Notif.CH_DOCUMENTS, "Dokumen mendekati jatuh tempo", "$label jatuh tempo dalam $days hari")
                prefs.edit().putLong(key, trigger).apply()
            }
        }

        // 2) Perawatan komponen: tiap kategori punya on/off + jam sendiri. Dikirim saat status Segera/Lewat (berdasarkan km dan/atau bulan),
        //    diulang tiap 3 hari selama belum diservis, atau langsung bila statusnya berubah.
        val latestKm = db.odometerDao().all().groupBy { it.vehicleId }.mapValues { (_, l) -> l.maxByOrNull { it.recordedAt }!!.km }
        db.scheduleDao().allActive().forEach { s ->
            if (!s.notifyEnabled || !reached(s.notifyHour, s.notifyMinute)) return@forEach
            val view = Engines.scheduleView(s, latestKm[s.vehicleId], today)
            if (view.status != ServiceStatus.SOON && view.status != ServiceStatus.DUE && view.status != ServiceStatus.LATE) return@forEach
            val key = "svc_sent_${s.id}"
            val last = prefs.getString(key, null)?.split('|')
            val lastStatus = last?.getOrNull(0)
            val lastDay = last?.getOrNull(1)?.toLongOrNull() ?: Long.MIN_VALUE
            if (lastStatus == view.status.name && todayEpoch - lastDay < 3) return@forEach

            val parts = mutableListOf<String>()
            view.kmRemaining?.let { parts += if (it < 0) "lewat ${fmtKm(-it)} km" else "sisa ${fmtKm(it)} km" }
            view.daysRemaining?.let { parts += if (it < 0) "lewat ${-it} hari" else "sisa $it hari" }
            val name = s.name + (vehicleNames[s.vehicleId]?.let { " ($it)" } ?: "")
            val detail = if (parts.isEmpty()) "" else ": " + parts.joinToString(", ")
            if (view.status == ServiceStatus.LATE) Notif.show(ctx, Notif.CH_SERVICE, "Servis terlambat", "$name sudah lewat jadwal$detail")
            else Notif.show(ctx, Notif.CH_SERVICE, "Servis mendekati jadwal", "$name segera diservis$detail")
            prefs.edit().putString(key, "${view.status.name}|$todayEpoch").apply()
        }

        // 3) Pengingat update KM: bila KM terakhir dicatat sudah >= N hari/bulan/tahun lalu, pada jam yang dipilih.
        //    Diulang tiap interval selama KM belum diperbarui.
        val kmValue = Notif.kmReminderValue(ctx)
        if (Notif.kmReminderEnabled(ctx) && kmValue > 0 && reached(Notif.kmReminderHour(ctx), Notif.kmReminderMinute(ctx))) {
            val unit = Notif.kmReminderUnit(ctx)
            val lastByVehicle = db.odometerDao().all().groupBy { it.vehicleId }.mapValues { (_, l) -> l.maxOf { it.recordedAt } }
            vehicles.forEach { v ->
                val lastDate = toDate(lastByVehicle[v.id] ?: v.createdAt)
                val due = unit.plus(lastDate, kmValue)
                if (!today.isBefore(due)) {
                    val key = "km_sent_${v.id}"
                    val sent = prefs.getLong(key, -1L)
                    val shouldSend = sent < due.toEpochDay() || !today.isBefore(unit.plus(LocalDate.ofEpochDay(sent), kmValue))
                    if (shouldSend) {
                        val ago = ChronoUnit.DAYS.between(lastDate, today)
                        Notif.show(ctx, Notif.CH_ODOMETER, "Update KM ${v.name}", "KM terakhir dicatat $ago hari lalu. Yuk perbarui odometer.")
                        prefs.edit().putLong(key, todayEpoch).apply()
                    }
                }
            }
        }
        return Result.success()
    }
}
