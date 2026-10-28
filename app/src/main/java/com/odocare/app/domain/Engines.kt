package com.odocare.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class ServiceStatus(val label: String, val urgency: Int) {
    NORMAL("✓ Normal", 0),
    SEGERA_SERVIS("! Segera Servis", 1),
    JATUH_TEMPO("‼ Jatuh Tempo", 2),
    TERLAMBAT("✖ Terlambat", 3),
    SELESAI("✓ Selesai", 0),
    KM_BELUM_DIPERBARUI("? Kilometer Belum Diperbarui", 0)
}

data class ServiceInput(
    val intervalKm: Long?, val lastKm: Long?,
    val intervalMonths: Int?, val lastDate: LocalDate?,
    val warnKm: Long = 500, val warnDays: Long = 14
)

data class ServiceResult(
    val status: ServiceStatus, val targetKm: Long?, val remainingKm: Long?,
    val targetDate: LocalDate?, val remainingDays: Long?
)

object ServiceEngine {
    fun evaluate(i: ServiceInput, currentKm: Long?, today: LocalDate): ServiceResult {
        val ik = i.intervalKm; val lk = i.lastKm
        val im = i.intervalMonths; val ld = i.lastDate
        val targetKm = if (ik != null && lk != null) lk + ik else null
        val remKm = if (targetKm != null && currentKm != null) targetKm - currentKm else null
        val targetDate = if (im != null && ld != null) ld.plusMonths(im.toLong()) else null
        val remDays = targetDate?.let { ChronoUnit.DAYS.between(today, it) }
        val kmStatus = remKm?.let {
            when { it < 0 -> ServiceStatus.TERLAMBAT; it == 0L -> ServiceStatus.JATUH_TEMPO
                it <= i.warnKm -> ServiceStatus.SEGERA_SERVIS; else -> ServiceStatus.NORMAL }
        }
        val dateStatus = remDays?.let {
            when { it < 0 -> ServiceStatus.TERLAMBAT; it == 0L -> ServiceStatus.JATUH_TEMPO
                it <= i.warnDays -> ServiceStatus.SEGERA_SERVIS; else -> ServiceStatus.NORMAL }
        }
        val status = listOfNotNull(kmStatus, dateStatus).maxByOrNull { it.urgency }
            ?: ServiceStatus.KM_BELUM_DIPERBARUI
        return ServiceResult(status, targetKm, remKm, targetDate, remDays)
    }
}

enum class DocStatus(val label: String) {
    AMAN("✓ Aman"), SEGERA("! Segera Jatuh Tempo"), HARI_INI("‼ Jatuh Tempo Hari Ini"),
    TERLAMBAT("✖ Terlambat"), BELUM_DIATUR("? Belum Diatur")
}

object DocumentEngine {
    fun status(due: LocalDate?, today: LocalDate, soonDays: Long = 30): DocStatus {
        if (due == null) return DocStatus.BELUM_DIATUR
        val d = ChronoUnit.DAYS.between(today, due)
        return when { d < 0 -> DocStatus.TERLAMBAT; d == 0L -> DocStatus.HARI_INI
            d <= soonDays -> DocStatus.SEGERA; else -> DocStatus.AMAN }
    }
}

enum class ReminderUnit(val label: String) { DAY("Hari"), MONTH("Bulan"), YEAR("Tahun") }

data class ReminderRule(
    val amount: Int, val unit: ReminderUnit, val hour: Int, val minute: Int, val enabled: Boolean = true
)

object ReminderEngine {
    fun maxAmount(unit: ReminderUnit) = when (unit) { ReminderUnit.DAY -> 3650; ReminderUnit.MONTH -> 1200; ReminderUnit.YEAR -> 100 }

    /** Mengembalikan pesan kesalahan (Bahasa Indonesia) atau null jika valid. */
    fun validate(amountText: String, unit: ReminderUnit, hour: Int, minute: Int): String? {
        val a = amountText.trim().toIntOrNull() ?: return "Jumlah waktu harus berupa angka bulat."
        if (a <= 0) return "Jumlah waktu harus lebih dari 0."
        if (a > maxAmount(unit)) return "Jumlah terlalu besar (maksimal ${maxAmount(unit)} ${unit.label.lowercase()})."
        if (hour !in 0..23 || minute !in 0..59) return "Jam atau menit tidak valid."
        return null
    }

    private fun minus(d: LocalDate, a: Int, u: ReminderUnit) = when (u) {
        ReminderUnit.DAY -> d.minusDays(a.toLong())
        ReminderUnit.MONTH -> d.minusMonths(a.toLong())   // otomatis memakai tanggal valid terakhir
        ReminderUnit.YEAR -> d.minusYears(a.toLong())
    }

    private fun plus(d: LocalDateTime, a: Int, u: ReminderUnit) = when (u) {
        ReminderUnit.DAY -> d.plusDays(a.toLong())
        ReminderUnit.MONTH -> d.plusMonths(a.toLong())
        ReminderUnit.YEAR -> d.plusYears(a.toLong())
    }

    fun reminderTime(due: LocalDate, r: ReminderRule): LocalDateTime =
        minus(due, r.amount, r.unit).atTime(r.hour, r.minute)

    /** Hanya pengingat yang masih di masa depan; waktu identik di-deduplikasi. */
    fun upcoming(due: LocalDate, rules: List<ReminderRule>, now: LocalDateTime): List<LocalDateTime> =
        rules.filter { it.enabled }.map { reminderTime(due, it) }.filter { it.isAfter(now) }.distinct().sorted()

    /** Dihitung dari waktu update terakhir; jika sudah lewat, geser ke kemunculan jam berikutnya. */
    fun nextOdometerReminder(lastUpdate: LocalDateTime, amount: Int, unit: ReminderUnit,
                             hour: Int, minute: Int, now: LocalDateTime): LocalDateTime {
        val base = plus(lastUpdate, amount, unit).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (base.isAfter(now)) return base
        val today = now.toLocalDate().atTime(hour, minute)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }
}

object ExpenseCalc {
    fun total(jasa: Long, part: Long, extra: Long, diskon: Long): Long = jasa + part + extra - diskon
    fun validate(jasa: Long, part: Long, extra: Long, diskon: Long): String? = when {
        listOf(jasa, part, extra, diskon).any { it < 0 } -> "Nominal tidak boleh negatif."
        total(jasa, part, extra, diskon) < 0 -> "Diskon melebihi total biaya."
        else -> null
    }
}
