package com.odocare.app.domain
import com.odocare.app.data.LegalDocument
import com.odocare.app.data.MaintenanceSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Label mengikuti istilah aplikasi: Aman / Segera / Lewat (DUE dipertahankan agar kompatibel, tidak dipakai lagi)
enum class ServiceStatus(val label: String) { NORMAL("Aman"), SOON("Segera"), DUE("Segera"), LATE("Lewat"), NO_ODOMETER("Km Belum Diperbarui") }
enum class DocStatus(val label: String) { SAFE("Aman"), SOON("Segera"), DUE_TODAY("Jatuh Tempo Hari Ini"), LATE("Lewat") }

/** progress = fraksi interval yang sudah terpakai (km atau waktu, ambil yang terbesar). >= 1 berarti lewat. */
data class ScheduleView(val schedule: MaintenanceSchedule, val status: ServiceStatus, val kmRemaining: Int?, val daysRemaining: Long?, val progress: Float = 0f)
data class DocumentView(val doc: LegalDocument, val status: DocStatus, val daysRemaining: Long)

object Engines {
    private fun millisToLocalDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    /**
     * Segera  : sisa km < 20% interval km, ATAU jatuh tempo bulan < 30 hari.
     * Lewat   : sisa km < 0 ATAU jatuh tempo bulan sudah lewat.
     * Diambil yang paling mendesak antara km dan waktu.
     */
    fun scheduleView(s: MaintenanceSchedule, currentKm: Int?, today: LocalDate = LocalDate.now()): ScheduleView {
        val ik = s.intervalKm?.takeIf { it > 0 }
        val lk = s.lastServiceKm
        val im = s.intervalMonths?.takeIf { it > 0 }
        val ld = s.lastServiceDate

        val kmRemaining: Int? = if (ik != null && lk != null && currentKm != null) lk + ik - currentKm else null
        val kmUsed: Float = if (ik != null && lk != null && currentKm != null) (currentKm - lk).toFloat() / ik else 0f

        var daysRemaining: Long? = null
        var dayUsed = 0f
        if (im != null && ld != null) {
            val last = millisToLocalDate(ld)
            val due = last.plusMonths(im.toLong())
            daysRemaining = ChronoUnit.DAYS.between(today, due)
            val total = ChronoUnit.DAYS.between(last, due).coerceAtLeast(1)
            dayUsed = ChronoUnit.DAYS.between(last, today).toFloat() / total
        }

        if (ik != null && currentKm == null && daysRemaining == null) return ScheduleView(s, ServiceStatus.NO_ODOMETER, null, null, 0f)

        val kmRank = if (kmRemaining == null || ik == null) 0 else when { kmRemaining < 0 -> 3; kmRemaining < ik * 0.2 -> 1; else -> 0 }
        val dayRank = if (daysRemaining == null) 0 else when { daysRemaining < 0 -> 3; daysRemaining < 30 -> 1; else -> 0 }
        val status = when (maxOf(kmRank, dayRank)) { 3 -> ServiceStatus.LATE; 1 -> ServiceStatus.SOON; else -> ServiceStatus.NORMAL }
        return ScheduleView(s, status, kmRemaining, daysRemaining, maxOf(kmUsed, dayUsed).coerceAtLeast(0f))
    }

    fun documentView(d: LegalDocument, today: LocalDate = LocalDate.now()): DocumentView {
        val days = ChronoUnit.DAYS.between(today, millisToLocalDate(d.expiryDate))
        val status = when { days < 0 -> DocStatus.LATE; days == 0L -> DocStatus.DUE_TODAY; days <= 30 -> DocStatus.SOON; else -> DocStatus.SAFE }
        return DocumentView(d, status, days)
    }
}
