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
            mgr.createNotificationChannel(NotificationChannel(CH_ODOMETER, "Odometer", NotificationManager.IMPORTANCE_LOW))
        }
    }

    fun show(ctx: Context, channel: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(ctx, channel).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pi).build()
        try { NotificationManagerCompat.from(ctx).notify((title + body).hashCode(), notif) } catch (_: SecurityException) {}
    }

    const val DEFAULT_DAYS = "30, 15"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("odocare", Context.MODE_PRIVATE)

    fun parseDays(text: String): List<Int> = text.split(',', ' ', ';').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..365 }.distinct().sortedDescending()
    fun reminderDaysText(ctx: Context): String = prefs(ctx).getString("reminder_days", DEFAULT_DAYS) ?: DEFAULT_DAYS
    fun reminderDays(ctx: Context): List<Int> = parseDays(reminderDaysText(ctx))
    fun setReminderDays(ctx: Context, text: String) { prefs(ctx).edit().putString("reminder_days", parseDays(text).joinToString(", ")).apply() }

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
    override suspend fun doWork(): Result {
        val db = AppDb.get(applicationContext)
        val today = LocalDate.now()
        run {
            val reminderDays = Notif.reminderDays(applicationContext).toSet()
            val vehicleNames = db.vehicleDao().all().associate { it.id to it.name }
            db.documentDao().all().forEach { d ->
                val days = ChronoUnit.DAYS.between(today, Instant.ofEpochMilli(d.expiryDate).atZone(ZoneId.systemDefault()).toLocalDate())
                val label = d.type.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } + (vehicleNames[d.vehicleId]?.let { " ($it)" } ?: "")
                // Hanya di hari pengingat yang diatur dan di hari jatuh tempo; yang sudah lewat dilewati
                if (days == 0L) Notif.show(applicationContext, Notif.CH_DOCUMENTS, "Jatuh tempo hari ini", "$label jatuh tempo hari ini")
                else if (days > 0 && days.toInt() in reminderDays) Notif.show(applicationContext, Notif.CH_DOCUMENTS, "Dokumen mendekati jatuh tempo", "$label jatuh tempo dalam $days hari")
            }
            db.scheduleDao().allActive().forEach { s ->
                if (s.intervalMonths != null && s.lastServiceDate != null) {
                    val days = ChronoUnit.DAYS.between(today, Instant.ofEpochMilli(s.lastServiceDate).atZone(ZoneId.systemDefault()).toLocalDate().plusMonths(s.intervalMonths.toLong()))
                    if (days < 0) Notif.show(applicationContext, Notif.CH_SERVICE, "Servis terlambat", "${s.name} sudah lewat ${-days} hari")
                    else if (days <= 14) Notif.show(applicationContext, Notif.CH_SERVICE, "Servis mendekati jadwal", "${s.name} dalam $days hari")
                }
            }
        }
        return Result.success()
    }
}
