package com.odocare.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EnginesTest {
    private val today = LocalDate.of(2026, 10, 28)
    private fun km(cur: Long?, last: Long? = 25000, interval: Long = 5000) =
        ServiceEngine.evaluate(ServiceInput(interval, last, null, null), cur, today)

    @Test fun targetDanSisaKm() { val r = km(28500); assertEquals(30000L, r.targetKm); assertEquals(1500L, r.remainingKm) }
    @Test fun normal() = assertEquals(ServiceStatus.NORMAL, km(26000).status)
    @Test fun segera() = assertEquals(ServiceStatus.SEGERA_SERVIS, km(29600).status)
    @Test fun jatuhTempo() = assertEquals(ServiceStatus.JATUH_TEMPO, km(30000).status)
    @Test fun terlambat() { val r = km(30400); assertEquals(ServiceStatus.TERLAMBAT, r.status); assertEquals(-400L, r.remainingKm) }
    @Test fun kmBelumDiperbarui() = assertEquals(ServiceStatus.KM_BELUM_DIPERBARUI, km(null).status)

    @Test fun gabunganWaktuLebihDulu() {
        val r = ServiceEngine.evaluate(ServiceInput(5000, 25000, 6, LocalDate.of(2026, 4, 20)), 25500, today)
        assertEquals(LocalDate.of(2026, 10, 20), r.targetDate)
        assertEquals(ServiceStatus.TERLAMBAT, r.status)
    }
    @Test fun gabunganKmLebihDulu() {
        val r = ServiceEngine.evaluate(ServiceInput(5000, 25000, 6, LocalDate.of(2026, 9, 1)), 30100, today)
        assertEquals(ServiceStatus.TERLAMBAT, r.status)
    }

    @Test fun dokumen() {
        assertEquals(DocStatus.BELUM_DIATUR, DocumentEngine.status(null, today))
        assertEquals(DocStatus.AMAN, DocumentEngine.status(today.plusDays(90), today))
        assertEquals(DocStatus.SEGERA, DocumentEngine.status(today.plusDays(10), today))
        assertEquals(DocStatus.HARI_INI, DocumentEngine.status(today, today))
        assertEquals(DocStatus.TERLAMBAT, DocumentEngine.status(today.minusDays(1), today))
    }

    @Test fun contohSpesifikasi() {
        val t = ReminderEngine.reminderTime(LocalDate.of(2026, 12, 15), ReminderRule(2, ReminderUnit.MONTH, 8, 30))
        assertEquals(LocalDateTime.of(2026, 10, 15, 8, 30), t)
    }
    @Test fun akhirBulanDanKabisat() {
        val m = ReminderRule(1, ReminderUnit.MONTH, 8, 0)
        assertEquals(LocalDate.of(2026, 2, 28), ReminderEngine.reminderTime(LocalDate.of(2026, 3, 31), m).toLocalDate())
        assertEquals(LocalDate.of(2028, 2, 29), ReminderEngine.reminderTime(LocalDate.of(2028, 3, 31), m).toLocalDate())
        val y = ReminderRule(1, ReminderUnit.YEAR, 8, 0)
        assertEquals(LocalDate.of(2027, 2, 28), ReminderEngine.reminderTime(LocalDate.of(2028, 2, 29), y).toLocalDate())
    }
    @Test fun deduplikasiDanFilterLewat() {
        val now = LocalDateTime.of(2026, 10, 28, 12, 0)
        val rules = listOf(ReminderRule(7, ReminderUnit.DAY, 8, 0), ReminderRule(7, ReminderUnit.DAY, 8, 0),
            ReminderRule(60, ReminderUnit.DAY, 8, 0))
        val r = ReminderEngine.upcoming(LocalDate.of(2026, 11, 20), rules, now)
        assertEquals(1, r.size)
        assertEquals(LocalDateTime.of(2026, 11, 13, 8, 0), r[0])
    }
    @Test fun validasi() {
        assertNotNull(ReminderEngine.validate("-3", ReminderUnit.DAY, 8, 0))
        assertNotNull(ReminderEngine.validate("abc", ReminderUnit.DAY, 8, 0))
        assertNotNull(ReminderEngine.validate("999", ReminderUnit.YEAR, 8, 0))
        assertNotNull(ReminderEngine.validate("5", ReminderUnit.DAY, 25, 0))
        assertNull(ReminderEngine.validate("30", ReminderUnit.DAY, 8, 0))
    }
    @Test fun odometerBerikutnya() {
        val now = LocalDateTime.of(2026, 10, 28, 12, 0)
        val n = ReminderEngine.nextOdometerReminder(LocalDateTime.of(2026, 10, 27, 20, 0), 7, ReminderUnit.DAY, 8, 0, now)
        assertEquals(LocalDateTime.of(2026, 11, 3, 8, 0), n)
        val lama = ReminderEngine.nextOdometerReminder(LocalDateTime.of(2026, 9, 1, 9, 0), 7, ReminderUnit.DAY, 8, 0, now)
        assertEquals(LocalDateTime.of(2026, 10, 29, 8, 0), lama) // tidak mengirim semua pengingat lama
    }
    @Test fun pengeluaran() {
        assertEquals(240000L, ExpenseCalc.total(100000, 150000, 10000, 20000))
        assertNotNull(ExpenseCalc.validate(0, 0, 0, 5000))
        assertNotNull(ExpenseCalc.validate(-1, 0, 0, 0))
        assertNull(ExpenseCalc.validate(100000, 0, 0, 5000))
    }
}
