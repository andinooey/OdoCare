package com.odocare.app.domain

import com.odocare.app.data.FuelLog

/**
 * Satu ruas jarak antara dua pengisian "tangki penuh" berurutan.
 * liters = bensin yang terisi SETELAH pengisian penuh sebelumnya sampai pengisian penuh ini (termasuk pengisian sebagian di tengah).
 * cost = biaya bensin yang sama.
 */
data class FuelSegment(val vehicleId: String, val endDate: Long, val distanceKm: Int, val liters: Double, val cost: Long)

/** Ringkasan BBM untuk satu rentang. kmPerLiter / costPerKm null bila belum ada ruas penuh-ke-penuh. */
data class FuelStats(
    val count: Int, val totalLiters: Double, val totalCost: Long,
    val kmPerLiter: Double?, val costPerKm: Double?, val measuredKm: Int
)

object FuelEngine {
    /**
     * Hitung ruas per kendaraan. Pengisian diurutkan menurut KM.
     * Pengisian penuh pertama hanya jadi titik awal; bensin sebelum titik itu tidak dihitung (tidak diketahui sisa tangkinya).
     */
    fun segments(logs: List<FuelLog>): List<FuelSegment> {
        val out = mutableListOf<FuelSegment>()
        for ((vid, list) in logs.groupBy { it.vehicleId }) {
            val sorted = list.sortedWith(compareBy<FuelLog>({ it.km }, { it.date }, { it.createdAt }))
            var start: FuelLog? = null
            var liters = 0.0
            var cost = 0L
            for (l in sorted) {
                val from = start
                if (from == null) {
                    if (l.fullTank) { start = l; liters = 0.0; cost = 0L }
                    continue
                }
                liters += l.liters
                cost += l.totalPrice
                if (l.fullTank) {
                    val dist = l.km - from.km
                    // Jarak 0 (dua isi di KM yang sama): terus kumpulkan sampai ada pengisian penuh di KM yang lebih besar
                    if (dist > 0 && liters > 0) {
                        out += FuelSegment(vid, l.date, dist, liters, cost)
                        start = l; liters = 0.0; cost = 0L
                    }
                }
            }
        }
        return out
    }

    /**
     * [logs] = catatan dalam rentang (untuk total biaya & liter). [segments] = ruas yang sudah difilter ke rentang
     * (ruas dihitung dari SEMUA catatan lebih dulu agar pengisian penuh sebelum rentang tetap jadi titik awal).
     */
    fun stats(logs: List<FuelLog>, segments: List<FuelSegment>): FuelStats {
        val dist = segments.sumOf { it.distanceKm }
        val lit = segments.sumOf { it.liters }
        val segCost = segments.sumOf { it.cost }
        return FuelStats(
            count = logs.size, totalLiters = logs.sumOf { it.liters }, totalCost = logs.sumOf { it.totalPrice },
            kmPerLiter = if (dist > 0 && lit > 0) dist / lit else null,
            costPerKm = if (dist > 0) segCost.toDouble() / dist else null,
            measuredKm = dist
        )
    }
}
