package com.odocare.app.notif

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.odocare.app.MainActivity
import com.odocare.app.data.AppDb
import com.odocare.app.domain.*
import kotlinx.coroutines.*
import java.time.*

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("odocare_prefs", Context.MODE_PRIVATE)
    fun getInt(c: Context, k: String, d: Int) = p(c).getInt(k, d)
    fun getBool(c: Context, k: String, d: Boolean) = p(c).getBoolean(k, d)
    fun getStr(c: Context, k: String, d: String) = p(c).getString(k, d) ?: d
    fun put(c: Context, k: String, v: Any) = p(c).edit().apply {
        when (v) { is Int -> putInt(k, v); is Boolean -> putBoolean(k, v); is String -> putString(k, v) }
    }.apply()
}

object NotificationHelper {
    const val CH_SERVIS = "servis"; const val CH_DOKUMEN = "dokumen"; const val CH_ODO = "odometer"

    fun createChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        listOf(CH_SERVIS to "Pengingat Servis", CH_DOKUMEN to "Pengingat Dokumen", CH_ODO to "Pengingat Odometer")
            .forEach { nm.createNotificationChannel(NotificationChannel(it.first, it.second, NotificationManager.IMPORTANCE_DEFAULT)) }
    }

    fun hasPermission(c: Context) = Build.VERSION.SDK_INT < 33 ||
        c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun enabled(c: Context, channel: String) = Prefs.getBool(c, "notif_$channel", true)

    fun show(c: Context, id: Int, channel: String, title: String, text: String, vehicleId: String?): Boolean {
        if (!hasPermission(c) || !enabled(c, channel)) return false
        val open = Intent(c, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            vehicleId?.let { putExtra("vehicleId", it) }
            putExtra("channel", channel)
        }
        val pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text)
            .setContentIntent(pi).setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        c.getSystemService(NotificationManager::class.java).notify(id, n)
        return true
    }
}

object Rescheduler {
    private fun pending(c: Context, key: String, extras: Intent? = null, create: Boolean): PendingIntent? {
        val i = (extras ?: Intent()).setClass(c, AlarmReceiver::class.java).setAction(key)
        val flags = PendingIntent.FLAG_IMMUTABLE or (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(c, key.hashCode(), i, flags)
    }

    fun cancel(c: Context, key: String) {
        pending(c, key, create = false)?.let { c.getSystemService(AlarmManager::class.java).cancel(it); it.cancel() }
    }

    /** Memakai alarm tepat hanya jika sistem mengizinkan; selain itu alarm hemat daya (waktu bisa bergeser). */
    fun scheduleAt(c: Context, key: String, whenMs: Long, channel: String, title: String, text: String, vehicleId: String?) {
        cancel(c, key)
        val extras = Intent().putExtra("key", key).putExtra("channel", channel).putExtra("title", title)
            .putExtra("text", text).putExtra("vehicleId", vehicleId)
        val pi = pending(c, key, extras, true)!!
        val am = c.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
    }

    private fun ms(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    suspend fun rescheduleAll(c: Context) = withContext(Dispatchers.IO) {
        val dao = AppDb.get(c).dao(); val now = LocalDateTime.now()
        // Odometer: satu alarm per kendaraan
        val odoOn = Prefs.getBool(c, "odo_on", false)
        val amount = Prefs.getInt(c, "odo_amount", 7)
        val unit = ReminderUnit.valueOf(Prefs.getStr(c, "odo_unit", "DAY"))
        val h = Prefs.getInt(c, "odo_hour", 8); val m = Prefs.getInt(c, "odo_min", 0)
        dao.vehiclesNow().forEach { v ->
            val key = "odo:${v.id}"; cancel(c, key)
            if (odoOn) {
                val last = Instant.ofEpochMilli(v.lastOdoAt ?: v.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                val next = ReminderEngine.nextOdometerReminder(last, amount, unit, h, m, now)
                scheduleAt(c, key, ms(next), NotificationHelper.CH_ODO, "Waktunya update odometer",
                    "Perbarui kilometer ${v.name}.", v.id)
            }
        }
        // Dokumen: hanya pengingat terdekat yang masih relevan
        dao.documentsNow().forEach { d ->
            val key = "doc:${d.id}"; cancel(c, key)
            val due = d.dueEpochDay?.let { LocalDate.ofEpochDay(it) } ?: return@forEach
            val rule = ReminderRule(d.remAmount, ReminderUnit.valueOf(d.remUnit), d.remHour, d.remMinute)
            ReminderEngine.upcoming(due, listOf(rule), now).firstOrNull()?.let {
                scheduleAt(c, key, ms(it), NotificationHelper.CH_DOKUMEN, "Pengingat dokumen",
                    "${d.name} akan jatuh tempo pada ${due}.", d.vehicleId)
            }
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val key = i.getStringExtra("key") ?: return
        NotificationHelper.show(c, key.hashCode(), i.getStringExtra("channel") ?: NotificationHelper.CH_ODO,
            i.getStringExtra("title") ?: "OdoCare", i.getStringExtra("text") ?: "", i.getStringExtra("vehicleId"))
        val pr = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { Rescheduler.rescheduleAll(c) } finally { pr.finish() } }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pr = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { Rescheduler.rescheduleAll(c) } finally { pr.finish() } }
    }
}
