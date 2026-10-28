package com.odocare.app.domain

import org.junit.Assert.*
import org.junit.Test

class CatalogTest {
    @Test fun setiapJenisPunyaDaftar() {
        listOf(MaintenanceCatalog.MOBIL, MaintenanceCatalog.MOTOR, MaintenanceCatalog.LAINNYA)
            .forEach { assertTrue(MaintenanceCatalog.forType(it).isNotEmpty()) }
    }
    @Test fun mobilDanMotorBerbeda() =
        assertNotEquals(MaintenanceCatalog.forType("Mobil").map { it.name }, MaintenanceCatalog.forType("Motor").map { it.name })
    @Test fun intervalValidDanNamaUnik() {
        listOf("Mobil", "Motor", "Lainnya").forEach { t ->
            val l = MaintenanceCatalog.forType(t)
            assertEquals(l.size, l.map { it.name }.toSet().size)
            l.forEach { assertTrue(it.name, it.intervalKm != null || it.intervalMonths != null)
                assertTrue((it.intervalKm ?: 1) > 0 && (it.intervalMonths ?: 1) > 0) }
        }
    }
}
