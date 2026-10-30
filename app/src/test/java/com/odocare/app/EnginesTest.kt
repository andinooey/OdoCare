package com.odocare.app

import com.odocare.app.data.LegalDocument
import com.odocare.app.data.MaintenanceSchedule
import com.odocare.app.domain.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EnginesTest {

    private val today = LocalDate.of(2024, 5, 20)
    private val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // --- TEST JADWAL SERVIS ---

    @Test
    fun testScheduleView_Normal() {
        // Jarak masih 5000 km, waktu masih 6 bulan
        val schedule = MaintenanceSchedule(
            vehicleId = "1", name = "Oli",
            intervalKm = 10000, lastServiceKm = 10000,
            intervalMonths = 6, lastServiceDate = todayMillis
        )
        val view = Engines.scheduleView(schedule, currentKm = 15000, today = today)
        assertEquals(ServiceStatus.NORMAL, view.status)
    }

    @Test
    fun testScheduleView_Soon() {
        // Jarak sisa 400 km (≤ 500 km masuk Soon)
        val schedule = MaintenanceSchedule(
            vehicleId = "1", name = "Oli",
            intervalKm = 10000, lastServiceKm = 10000
        )
        val view = Engines.scheduleView(schedule, currentKm = 19600, today = today)
        assertEquals(ServiceStatus.SOON, view.status)
    }

    @Test
    fun testScheduleView_Due() {
        // Jarak sisa 100 km (≤ 500 km), tapi waktu pas hari ini (0 hari)
        val schedule = MaintenanceSchedule(
            vehicleId = "1", name = "Oli",
            intervalKm = 10000, lastServiceKm = 10000,
            intervalMonths = 1, lastServiceDate = today.minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        val view = Engines.scheduleView(schedule, currentKm = 19900, today = today)
        assertEquals(ServiceStatus.DUE, view.status)
    }

    @Test
    fun testScheduleView_Late() {
        // Sudah lewat 200 km
        val schedule = MaintenanceSchedule(
            vehicleId = "1", name = "Oli",
            intervalKm = 10000, lastServiceKm = 10000
        )
        val view = Engines.scheduleView(schedule, currentKm = 20200, today = today)
        assertEquals(ServiceStatus.LATE, view.status)
    }

    @Test
    fun testScheduleView_NoOdometer() {
        // Ada interval km, tapi currentKm null
        val schedule = MaintenanceSchedule(
            vehicleId = "1", name = "Oli",
            intervalKm = 10000, lastServiceKm = 10000
        )
        val view = Engines.scheduleView(schedule, currentKm = null, today = today)
        assertEquals(ServiceStatus.NO_ODOMETER, view.status)
    }

    // --- TEST DOKUMEN ---

    @Test
    fun testDocumentView_Safe() {
        val doc = LegalDocument(vehicleId = "1", type = "SIM_A", expiryDate = today.plusMonths(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        val view = Engines.documentView(doc, today)
        assertEquals(DocStatus.SAFE, view.status)
    }

    @Test
    fun testDocumentView_Soon() {
        val doc = LegalDocument(vehicleId = "1", type = "SIM_A", expiryDate = today.plusDays(15).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        val view = Engines.documentView(doc, today)
        assertEquals(DocStatus.SOON, view.status)
    }

    @Test
    fun testDocumentView_DueToday() {
        val doc = LegalDocument(vehicleId = "1", type = "SIM_A", expiryDate = todayMillis)
        val view = Engines.documentView(doc, today)
        assertEquals(DocStatus.DUE_TODAY, view.status)
    }

    @Test
    fun testDocumentView_Late() {
        val doc = LegalDocument(vehicleId = "1", type = "SIM_A", expiryDate = today.minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        val view = Engines.documentView(doc, today)
        assertEquals(DocStatus.LATE, view.status)
    }
}
