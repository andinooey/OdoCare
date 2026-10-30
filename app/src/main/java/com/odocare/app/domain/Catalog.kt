package com.odocare.app.domain

data class CatalogItem(val name: String, val intervalKm: Int?, val intervalMonths: Int?)

object Catalog {
    private val mobil = listOf(CatalogItem("Ganti Oli Mesin", 10_000, 6), CatalogItem("Ganti Oli Transmisi", 40_000, 24), CatalogItem("Filter Udara", 20_000, 12), CatalogItem("Filter Kabin", 15_000, 12), CatalogItem("Busi", 40_000, 24), CatalogItem("Kampas Rem Depan", 30_000, null), CatalogItem("Kampas Rem Belakang", 40_000, null), CatalogItem("Minyak Rem", 40_000, 24), CatalogItem("Coolant", 40_000, 24), CatalogItem("Ban (rotasi)", 10_000, null), CatalogItem("Aki", null, 36), CatalogItem("Tune Up", 20_000, 12))
    private val motor = listOf(CatalogItem("Ganti Oli Mesin", 2_000, 6), CatalogItem("Ganti Oli Gardan", 8_000, 12), CatalogItem("Filter Udara", 8_000, 12), CatalogItem("Busi", 8_000, 12), CatalogItem("Kampas Rem Depan", 10_000, null), CatalogItem("Kampas Rem Belakang", 12_000, null), CatalogItem("Rantai & Gear", 20_000, null), CatalogItem("Minyak Rem", 20_000, 24), CatalogItem("Coolant", 12_000, 24), CatalogItem("Aki", null, 24), CatalogItem("Servis CVT", 10_000, 12))
    private val lainnya = listOf(CatalogItem("Servis Berkala", 10_000, 6), CatalogItem("Ganti Oli", 5_000, 6), CatalogItem("Pemeriksaan Umum", 5_000, 6))

    fun itemsFor(type: String): List<CatalogItem> = when (type) { "MOBIL" -> mobil; "MOTOR" -> motor; else -> lainnya }
}
