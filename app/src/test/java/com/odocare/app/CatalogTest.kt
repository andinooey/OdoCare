package com.odocare.app

import com.odocare.app.domain.Catalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {

    @Test
    fun testCatalogMobil_Has12Items() {
        val items = Catalog.itemsFor("MOBIL")
        assertEquals(12, items.size)
        assertTrue(items.any { it.name == "Ganti Oli Mesin" })
    }

    @Test
    fun testCatalogMotor_Has11Items() {
        val items = Catalog.itemsFor("MOTOR")
        assertEquals(11, items.size)
        assertTrue(items.any { it.name == "Servis CVT" })
    }

    @Test
    fun testCatalogLainnya_Has3Items() {
        val items = Catalog.itemsFor("LAINNYA")
        assertEquals(3, items.size)
    }
}
