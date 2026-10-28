package com.odocare.app.domain

/** Master jenis perawatan. Interval hanya acuan umum (pabrikan/bengkel); pengguna wajib menyesuaikan dengan buku manual. */
data class RecommendedService(
    val name: String, val category: String,
    val intervalKm: Long?, val intervalMonths: Int?, val note: String = ""
) {
    fun intervalText(): String = listOfNotNull(
        intervalKm?.let { String.format(java.util.Locale("id", "ID"), "%,d km", it) },
        intervalMonths?.let { "$it bulan" }
    ).joinToString(" atau ")
}

object MaintenanceCatalog {
    const val MOBIL = "Mobil"
    const val MOTOR = "Motor"
    const val LAINNYA = "Lainnya"

    fun forType(type: String): List<RecommendedService> = when (type) { MOBIL -> mobil; MOTOR -> motor; else -> lainnya }

    private val mobil = listOf(
        RecommendedService("Ganti oli mesin & filter oli", "Mesin", 10_000, 6, "Percepat jika sering macet"),
        RecommendedService("Filter udara", "Mesin", 20_000, null, "Lebih cepat di area berdebu"),
        RecommendedService("Filter AC/kabin", "Kabin", 20_000, null, "Sebagian pabrikan tiap 10.000 km"),
        RecommendedService("Busi", "Mesin", 40_000, null, "Biasanya di servis besar"),
        RecommendedService("Cairan pendingin (coolant)", "Mesin", 40_000, 24, "Periksa level tiap 3 bulan"),
        RecommendedService("Minyak rem", "Rem", 40_000, 24, "Periksa level tiap 3 bulan"),
        RecommendedService("Pemeriksaan rem & kampas", "Rem", 10_000, null, "Dicek di tiap servis berkala"),
        RecommendedService("Rotasi & balancing ban", "Ban", 10_000, null),
        RecommendedService("Pemeriksaan kaki-kaki & suspensi", "Kaki-kaki", 20_000, null),
        RecommendedService("Oli transmisi manual", "Transmisi", 50_000, null, "Pemakaian normal"),
        RecommendedService("Oli transmisi matic (AT/CVT)", "Transmisi", 40_000, null, "Patokan 30.000–40.000 km"),
        RecommendedService("Pemeriksaan aki", "Kelistrikan", null, 6)
    )

    private val motor = listOf(
        RecommendedService("Ganti oli mesin", "Mesin", 2_000, 6, "Buku manual umumnya 3.000 km; disarankan lebih cepat"),
        RecommendedService("Oli gardan/gearbox (matic)", "Transmisi", 6_000, null, "Sekitar tiap 3x ganti oli mesin"),
        RecommendedService("Servis & bersihkan CVT (matic)", "Transmisi", 8_000, null),
        RecommendedService("Ganti V-belt & roller (matic)", "Transmisi", 24_000, null, "Tergantung pemakaian"),
        RecommendedService("Filter udara", "Mesin", 12_000, null, "Tipe kertas: ganti, jangan ditiup"),
        RecommendedService("Busi", "Mesin", 8_000, null, "Sebagian motor 6.000–10.000 km"),
        RecommendedService("Pemeriksaan kampas rem", "Rem", 4_000, null),
        RecommendedService("Servis berkala umum", "Umum", 4_000, null, "Cek klep, kelistrikan, dll."),
        RecommendedService("Cairan radiator (motor berradiator)", "Mesin", 12_000, null),
        RecommendedService("Bersihkan & lumasi rantai (motor manual)", "Kaki-kaki", 1_000, null),
        RecommendedService("Pemeriksaan aki", "Kelistrikan", null, 6)
    )

    private val lainnya = listOf(
        RecommendedService("Servis mesin & ganti oli", "Mesin", 5_000, 6, "Sesuaikan buku manual"),
        RecommendedService("Pemeriksaan ban", "Ban", null, 3),
        RecommendedService("Pemeriksaan aki", "Kelistrikan", null, 6)
    )
}
