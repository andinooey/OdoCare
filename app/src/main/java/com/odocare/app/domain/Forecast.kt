package com.odocare.app.domain

import com.odocare.app.data.MaintenanceSchedule
import com.odocare.app.data.OdometerLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Laju pemakaian kendaraan. typical = median (dipakai untuk batas atas), fast = persentil 75 (batas bawah, condong lebih awal). */
data class UsageRate(val typical: Double, val fast: Double, val windowDays: Int, val points: Int)

/** Perkiraan servis terdekat sebagai rentang hari. byKm = true bila acuannya KM, false bila batas bulan. */
data class NextServiceForecast(val name: String, val daysLow: Int, val daysHigh: Int, val byKm: Boolean, val targetKm: Int?, val rate: UsageRate)

sealed interface ForecastResult {
    data class Ready(val forecast: NextServiceForecast) : ForecastResult
    /** KM terakhir sudah lama tidak diperbarui: ajak pengguna memperbarui, jangan menebak. */
    object Stale : ForecastResult
    /** Data belum cukup: tidak ada perkiraan. */
    object NotEnoughData : ForecastResult
}

/**
 * Perkiraan hari menuju servis berikutnya. Hanya petunjuk tambahan: status Aman / Segera / Lewat dan notifikasi
 * tetap dihitung dari batas pasti (sisa km / bulan) oleh [Engines], tidak bergantung pada perkiraan ini.
 *
 * Aturan agar tidak menyesatkan:
 * - Minimal [MIN_POINTS] hari berbeda yang tercatat dalam [WINDOW_DAYS] hari terakhir, rentangnya minimal [MIN_SPAN_DAYS] hari.
 * - Pakai median laju antar catatan (tahan terhadap perjalanan jauh sesekali), bukan rata-rata seumur hidup.
 * - Hasil berupa rentang: batas bawah dari laju cepat (persentil 75), batas atas dari laju median.
 * - Bila KM terakhir lebih tua dari [STALE_DAYS] hari: tidak ada perkiraan, ajak perbarui KM.
 * - Komponen yang sudah Lewat atau belum punya KM dilewati (sudah ditangani status).
 * - Bila batas bulan jatuh tempo lebih cepat, batas bulan yang dipakai (pasti, bukan perkiraan).
 */
object Forecast {
    private const val DAY = 86_400_000L
    const val WINDOW_DAYS = 60
    const val MIN_POINTS = 3
    const val MIN_SPAN_DAYS = 14
    const val STALE_DAYS = 14
    private const val MIN_KM_PER_DAY = 1.0
    private const val MAX_KM_PER_DAY = 1500.0

    fun usageRate(logs: List<OdometerLog>, now: Long = System.currentTimeMillis()): UsageRate? {
        val zone = ZoneId.systemDefault()
        // Satu titik per hari (KM tertinggi hari itu), hanya dalam jendela terakhir
        val points = logs.filter { now - it.recordedAt <= WINDOW_DAYS * DAY }
            .groupBy { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() }
            .map { (d, l) -> d to l.maxOf { it.km } }
            .sortedBy { it.first }
        if (points.size < MIN_POINTS) return null
        if (ChronoUnit.DAYS.between(points.first().first, points.last().first) < MIN_SPAN_DAYS) return null
        val rates = points.zipWithNext().mapNotNull { (a, b) ->
            val days = ChronoUnit.DAYS.between(a.first, b.first).toInt()
            val dk = b.second - a.second
            if (days >= 1 && dk >= 0) dk.toDouble() / days else null
        }.sorted()
        if (rates.size < 2) return null
        val n = rates.size
        val median = if (n % 2 == 1) rates[n / 2] else (rates[n / 2 - 1] + rates[n / 2]) / 2.0
        val p75 = rates[ceil(0.75 * (n - 1)).toInt()]
        if (median < MIN_KM_PER_DAY || median > MAX_KM_PER_DAY) return null
        return UsageRate(typical = median, fast = maxOf(p75, median), windowDays = WINDOW_DAYS, points = points.size)
    }

    fun next(
        schedules: List<MaintenanceSchedule>, currentKm: Int?, logs: List<OdometerLog>,
        now: Long = System.currentTimeMillis(), today: LocalDate = LocalDate.now()
    ): ForecastResult {
        val lastRecorded = logs.maxOfOrNull { it.recordedAt } ?: return ForecastResult.NotEnoughData
        if (now - lastRecorded > STALE_DAYS * DAY) return ForecastResult.Stale
        val rate = usageRate(logs, now) ?: return ForecastResult.NotEnoughData
        val km = currentKm ?: return ForecastResult.NotEnoughData

        var best: NextServiceForecast? = null
        for (s in schedules) {
            val view = Engines.scheduleView(s, km, today)
            if (view.status == ServiceStatus.LATE || view.status == ServiceStatus.NO_ODOMETER) continue
            val kmRem = view.kmRemaining?.takeIf { it > 0 }
            val lowKm = kmRem?.let { ceil(it / rate.fast).toInt() }
            val highKm = kmRem?.let { ceil(it / rate.typical).toInt() }
            val dDays = view.daysRemaining?.takeIf { it >= 0 }?.toInt()
            val lows = listOfNotNull(lowKm, dDays)
            val highs = listOfNotNull(highKm, dDays)
            if (lows.isEmpty()) continue
            val low = lows.minOrNull()!!
            val high = maxOf(highs.minOrNull()!!, low)
            val byKm = lowKm != null && (dDays == null || lowKm <= dDays)
            val target = if (s.intervalKm != null && s.lastServiceKm != null && s.intervalKm > 0) s.lastServiceKm + s.intervalKm else null
            val cand = NextServiceForecast(s.name, low, high, byKm, if (byKm) target else null, rate)
            val b = best
            if (b == null || cand.daysLow < b.daysLow || (cand.daysLow == b.daysLow && cand.daysHigh < b.daysHigh)) best = cand
        }
        return best?.let { ForecastResult.Ready(it) } ?: ForecastResult.NotEnoughData
    }

    /** Teks rentang hari yang jujur soal ketidakpastian. */
    fun describe(low: Int, high: Int): String = when {
        high <= 0 -> "hari ini"
        low >= 60 -> {
            val a = Math.round(low / 30.0).toInt(); val b = Math.round(high / 30.0).toInt()
            if (a == b) "sekitar $a bulan lagi" else "sekitar $a–$b bulan lagi"
        }
        low == high -> "sekitar $low hari lagi"
        low <= 0 -> "dalam $high hari"
        else -> "sekitar $low–$high hari lagi"
    }
}
