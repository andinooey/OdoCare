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
import com.odocare.app.domain.IntervalUnit
import com.odocare.app.domain.Reminders
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

    // ---- Pengingat update KM: "setiap N hari/bulan/tahun" (N = 0 berarti nonaktif) ----
    fun kmReminderValue(ctx: Context): Int = prefs(ctx).getInt("km_reminder_value", 1)
    fun kmReminderUnit(ctx: Context): IntervalUnit = IntervalUnit.parse(prefs(ctx).getString("km_reminder_unit", "MONTH")) ?: IntervalUnit.MONTH
    fun setKmReminder(ctx: Context, value: Int, unit: IntervalUnit) {
        prefs(ctx).edit().putInt("km_reminder_value", value.coerceAtLeast(0)).putString("km_reminder_unit", unit.name).apply()
    }

    /** Dijalankan tiap hari sekitar pukul 09.00. Dijadwalkan ulang setiap aplikasi dibuka. */
    fun scheduleDaily(ctx: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(9, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMin = Duration.between(now, next).toMinutes()
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).setInitialDelay(delayMin, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("odocare_daily_reminder", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private fun toDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val db = AppDb.get(ctx)
        val prefs = ctx.getSharedPreferences("odocare", Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val vehicles = db.vehicleDao().all()
        val vehicleNames = vehicles.associate { it.id to it.name }

        // 1) Dokumen: pengingat per dokumen + hari jatuh tempo.
        //    Jendela (terakhir jalan, hari ini] agar hari yang terlewat (HP mati/Doze) tetap terkejar, maksimal 3 hari ke belakang.
        val lastRun = prefs.getLong("worker_last_run_day", todayEpoch - 1)
        val from = maxOf(lastRun + 1, todayEpoch - 3)
        db.documentDao().all().forEach { d ->
            val expiry = toDate(d.expiryDate)
            val triggerDays = Reminders.parse(d.reminders).map { it.dateFor(expiry).toEpochDay() } + expiry.toEpochDay()
            val days = ChronoUnit.DAYS.between(today, expiry)
            if (days >= 0 && triggerDays.any { it in from..todayEpoch }) {
                val label = d.type.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } + (vehicleNames[d.vehicleId]?.let { " ($it)" } ?: "")
                if (days == 0L) Notif.show(ctx, Notif.CH_DOCUMENTS, "Jatuh tempo hari ini", "$label jatuh tempo hari ini")
                else Notif.show(ctx, Notif.CH_DOCUMENTS, "Dokumen mendekati jatuh tempo", "$label jatuh tempo dalam $days hari")
            }
        }
        prefs.edit().putLong("worker_last_run_day", todayEpoch).apply()

        // 2) Jadwal servis berbasis bulan
        db.scheduleDao().allActive().forEach { s ->
            if (s.intervalMonths != null && s.lastServiceDate != null) {
                val days = ChronoUnit.DAYS.between(today, toDate(s.lastServiceDate).plusMonths(s.intervalMonths.toLong()))
                if (days < 0) Notif.show(ctx, Notif.CH_SERVICE, "Servis terlambat", "${s.name} sudah lewat ${-days} hari")
                else if (days <= 14) Notif.show(ctx, Notif.CH_SERVICE, "Servis mendekati jadwal", "${s.name} dalam $days hari")
            }
        }

        // 3) Pengingat update KM: bila KM terakhir dicatat sudah >= N hari/bulan/tahun lalu.
        //    Diulang tiap interval selama KM belum diperbarui.
        val kmValue = Notif.kmReminderValue(ctx)
        if (kmValue > 0) {
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
