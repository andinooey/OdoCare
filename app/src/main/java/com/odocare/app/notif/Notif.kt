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
import kotlinx.coroutines.runBlocking

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
        val notif = NotificationCompat.Builder(ctx, channel).setSmallIcon(R.drawable.ic_launcher).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pi).build()
        try { NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), notif) } catch (_: SecurityException) {}
    }

    fun scheduleDaily(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("odocare_daily_reminder", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build())
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val db = AppDb.get(applicationContext)
        val today = LocalDate.now()
        runBlocking {
            db.documentDao().all().forEach { d ->
                val days = ChronoUnit.DAYS.between(today, Instant.ofEpochMilli(d.expiryDate).atZone(ZoneId.systemDefault()).toLocalDate())
                if (days in 0..30) Notif.show(applicationContext, Notif.CH_DOCUMENTS, "Dokumen mendekati jatuh tempo", "${d.type.replace('_', ' ')} jatuh tempo dalam $days hari")
                else if (days < 0) Notif.show(applicationContext, Notif.CH_DOCUMENTS, "Dokumen sudah terlambat", "${d.type.replace('_', ' ')} terlambat ${-days} hari")
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
