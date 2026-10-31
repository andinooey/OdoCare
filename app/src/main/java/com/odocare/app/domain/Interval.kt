package com.odocare.app.domain

import java.time.LocalDate

/** Satuan interval yang dipakai di seluruh aplikasi: hari / bulan / tahun. */
enum class IntervalUnit(val label: String) {
    DAY("Hari"), MONTH("Bulan"), YEAR("Tahun");

    fun plus(d: LocalDate, n: Int): LocalDate = when (this) { DAY -> d.plusDays(n.toLong()); MONTH -> d.plusMonths(n.toLong()); YEAR -> d.plusYears(n.toLong()) }
    fun minus(d: LocalDate, n: Int): LocalDate = when (this) { DAY -> d.minusDays(n.toLong()); MONTH -> d.minusMonths(n.toLong()); YEAR -> d.minusYears(n.toLong()) }

    companion object {
        fun parse(s: String?): IntervalUnit? = values().firstOrNull { it.name == s }
    }
}

/** Satu pengingat: "N satuan sebelum jatuh tempo". */
data class Reminder(val value: Int, val unit: IntervalUnit) {
    fun dateFor(expiry: LocalDate): LocalDate = unit.minus(expiry, value)
    fun label(): String = "$value ${unit.label.lowercase()} sebelum"
}

/** Disimpan sebagai teks "30:DAY,15:DAY,1:MONTH" di kolom legal_documents.reminders */
object Reminders {
    const val DEFAULT = "30:DAY,15:DAY"

    fun parse(text: String?): List<Reminder> = (text ?: "").split(',').mapNotNull { part ->
        val bits = part.trim().split(':')
        val v = bits.getOrNull(0)?.trim()?.toIntOrNull()
        val u = IntervalUnit.parse(bits.getOrNull(1)?.trim())
        if (v != null && v > 0 && u != null) Reminder(v, u) else null
    }.distinct()

    fun encode(list: List<Reminder>): String = list.joinToString(",") { "${it.value}:${it.unit.name}" }

    fun summary(text: String?): String {
        val list = parse(text)
        return if (list.isEmpty()) "Hanya di hari jatuh tempo" else list.joinToString(", ") { "${it.value} ${it.unit.label.lowercase()}" } + " sebelum"
    }
}
