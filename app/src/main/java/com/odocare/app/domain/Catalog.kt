package com.odocare.app.domain

import com.odocare.app.data.MasterCategory

/** Isi awal master kategori service (bisa diubah user di menu Master Kategori). */
object Catalog {
    private fun m(type: String, idx: Int, name: String, km: Int, months: Int) =
        MasterCategory(id = "seed_${type}_$idx", name = name, vehicleType = type, defaultKm = km, defaultMonths = months)

    fun seed(): List<MasterCategory> = listOf(
        m("MOTOR", 1, "Oli Mesin", 2_000, 6), m("MOTOR", 2, "Oli Gardan", 8_000, 0), m("MOTOR", 3, "Filter Udara", 8_000, 0),
        m("MOTOR", 4, "Busi", 8_000, 0), m("MOTOR", 5, "Kampas Rem", 10_000, 0), m("MOTOR", 6, "Rantai/V-Belt", 20_000, 0),
        m("MOTOR", 7, "Ban", 20_000, 0), m("MOTOR", 8, "Aki", 24_000, 0),
        m("MOBIL", 1, "Oli Mesin", 5_000, 6), m("MOBIL", 2, "Filter Oli", 10_000, 0), m("MOBIL", 3, "Filter Udara", 20_000, 0),
        m("MOBIL", 4, "Busi", 20_000, 0), m("MOBIL", 5, "Kampas Rem", 30_000, 0), m("MOBIL", 6, "Oli Transmisi", 40_000, 0),
        m("MOBIL", 7, "Minyak Rem", 40_000, 0), m("MOBIL", 8, "Ban", 40_000, 0), m("MOBIL", 9, "Aki", 30_000, 0)
    )
}
