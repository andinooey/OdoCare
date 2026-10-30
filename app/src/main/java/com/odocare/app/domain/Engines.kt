package com.odocare.app.domain
import com.odocare.app.data.LegalDocument
import com.odocare.app.data.MaintenanceSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ServiceStatus(val label: String) { NORMAL("Normal"), SOON("Segera Servis"), DUE("Jatuh Tempo"), LATE("Terlambat"), NO_ODOMETER("Km Belum Diperbarui") }
enum class DocStatus(val label: String) { SAFE("Aman"), SOON("Segera Jatuh Tempo"), DUE_TODAY("Jatuh Tempo Hari Ini"), LATE("Terlambat") }
data class ScheduleView(val schedule: MaintenanceSchedule, val status: ServiceStatus, val kmRemaining: Int?, val daysRemaining: Long?)
data class DocumentView(val doc: LegalDocument, val status: DocStatus, val daysRemaining: Long)

object Engines {
    private fun millisToLocalDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    
    fun scheduleView(s: MaintenanceSchedule, currentKm: Int?, today: LocalDate = LocalDate.now()): ScheduleView {
        var kmTrigger: Int? = null
        if (s.intervalKm != null && s.lastServiceKm != null && currentKm != null) kmTrigger = (s.lastServiceKm + s.intervalKm) - currentKm
        var dayTrigger: Long? = null
        if (s.intervalMonths != null && s.lastServiceDate != null) dayTrigger = ChronoUnit.DAYS.between(today, millisToLocalDate(s.lastServiceDate).plusMonths(s.intervalMonths.toLong()))
        if (s.intervalKm != null && currentKm == null && dayTrigger == null) return ScheduleView(s, ServiceStatus.NO_ODOMETER, null, dayTrigger)
        val kmRank = kmTrigger?.let { k -> when { k < 0 -> 3; k <= 500 -> 2; k <= 2000 -> 1; else -> 0 } } ?: 0
        val dayRank = dayTrigger?.let { d -> when { d < 0 -> 3; d == 0L -> 2; d <= 30 -> 1; else -> 0 } } ?: 0
        val worst = maxOf(kmRank, dayRank)
        val status = when (worst) { 3 -> ServiceStatus.LATE; 2 -> ServiceStatus.DUE; 1 -> ServiceStatus.SOON; else -> ServiceStatus.NORMAL }
        return ScheduleView(s, status, kmTrigger, dayTrigger)
    }

    fun documentView(d: LegalDocument, today: LocalDate = LocalDate.now()): DocumentView {
        val days = ChronoUnit.DAYS.between(today, millisToLocalDate(d.expiryDate))
        val status = when { days < 0 -> DocStatus.LATE; days == 0L -> DocStatus.DUE_TODAY; days <= 30 -> DocStatus.SOON; else -> DocStatus.SAFE }
        return DocumentView(d, status, days)
    }
}
