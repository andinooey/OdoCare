package com.odocare.app.domain

import com.odocare.app.data.DocumentType
import com.odocare.app.data.MasterCategory
import com.odocare.app.data.VehicleType

/** Isi awal master kategori service (bisa diubah user di menu Master Kategori). */
object Catalog {
    private fun m(type: String, idx: Int, name: String, km: Int, months: Int) =
        MasterCategory(id = "seed_${type}_$idx", name = name, vehicleType = type, defaultKm = km, defaultMonths = months)

    /** Jenis kendaraan bawaan. Kode MOTOR / MOBIL dipertahankan agar data lama tetap cocok. */
    fun defaultTypes(): List<VehicleType> = listOf(
        VehicleType(code = "MOTOR", name = "Motor", icon = "MOTOR", createdAt = 1L),
        VehicleType(code = "MOBIL", name = "Mobil", icon = "CAR", createdAt = 2L)
    )

    /** Jenis dokumen bawaan. Kode lama (PAJAK_TAHUNAN, SIM_A, dst) dipertahankan agar dokumen yang sudah ada tetap cocok. */
    fun defaultDocTypes(): List<DocumentType> = listOf(
        DocumentType("PAJAK_TAHUNAN", "Pajak Tahunan", "ALL", 1, "YEAR", 1L),
        DocumentType("GANTI_PLAT", "Ganti Plat 5 Tahunan", "ALL", 5, "YEAR", 2L),
        DocumentType("STNK_5_TAHUNAN", "STNK 5 Tahunan", "ALL", 5, "YEAR", 3L),
        DocumentType("SIM_A", "SIM A", "MOBIL", 5, "YEAR", 4L),
        DocumentType("SIM_C", "SIM C", "MOTOR", 5, "YEAR", 5L),
        DocumentType("ASURANSI", "Asuransi", "ALL", 1, "YEAR", 6L),
        DocumentType("GARANSI", "Garansi", "ALL", 0, "YEAR", 7L),
        DocumentType("LAINNYA", "Lainnya", "ALL", 0, "YEAR", 8L)
    )

    fun seed(): List<MasterCategory> = listOf(
        m("MOTOR", 1, "Oli Mesin", 2_000, 6), m("MOTOR", 2, "Oli Gardan", 8_000, 0), m("MOTOR", 3, "Filter Udara", 8_000, 0),
        m("MOTOR", 4, "Busi", 8_000, 0), m("MOTOR", 5, "Kampas Rem", 10_000, 0), m("MOTOR", 6, "Rantai/V-Belt", 20_000, 0),
        m("MOTOR", 7, "Ban", 20_000, 0), m("MOTOR", 8, "Aki", 24_000, 0),
        m("MOBIL", 1, "Oli Mesin", 5_000, 6), m("MOBIL", 2, "Filter Oli", 10_000, 0), m("MOBIL", 3, "Filter Udara", 20_000, 0),
        m("MOBIL", 4, "Busi", 20_000, 0), m("MOBIL", 5, "Kampas Rem", 30_000, 0), m("MOBIL", 6, "Oli Transmisi", 40_000, 0),
        m("MOBIL", 7, "Minyak Rem", 40_000, 0), m("MOBIL", 8, "Ban", 40_000, 0), m("MOBIL", 9, "Aki", 30_000, 0)
    )
}
