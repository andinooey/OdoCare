package com.odocare.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.odocare.app.data.LegalDocument
import com.odocare.app.data.ServiceRecord
import com.odocare.app.data.Vehicle
import com.odocare.app.domain.Reminders
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Exports {
    /** Tulis ke cache lalu buka menu bagikan. Butuh FileProvider (lihat AndroidManifest). */
    fun shareFile(ctx: Context, fileName: String, mime: String, bytes: ByteArray) {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val f = File(dir, fileName).also { it.writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun msToDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun csvField(s: String): String = if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    /** CSV dengan BOM UTF-8 agar terbuka benar di Excel. */
    fun csvBytes(records: List<ServiceRecord>, vehicles: List<Vehicle>): ByteArray {
        val byId = vehicles.associateBy { it.id }
        val sb = StringBuilder("Tanggal,Kendaraan,Plat,Kategori,KM,Biaya,Catatan\r\n")
        records.sortedByDescending { it.date }.forEach { r ->
            val v = byId[r.vehicleId]
            sb.append(listOf(msToDate(r.date).toString(), v?.name ?: "", v?.plate ?: "", r.title, r.km.toString(), r.cost.toString(), r.notes ?: "").joinToString(",") { csvField(it) }).append("\r\n")
        }
        return byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun icsEscape(s: String) = s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")

    /** Event seharian di tanggal jatuh tempo + VALARM pukul 09.00 untuk tiap pengingat dokumen (dan hari-H). */
    fun icsText(vehicles: List<Vehicle>, docs: List<LegalDocument>, labelOf: (String) -> String): String {
        val sb = StringBuilder()
        fun line(s: String) { sb.append(s).append("\r\n") }
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(ZonedDateTime.now(ZoneOffset.UTC))
        line("BEGIN:VCALENDAR"); line("VERSION:2.0"); line("PRODID:-//OdoCare//ID//EN"); line("CALSCALE:GREGORIAN")
        docs.forEach { d ->
            val date = msToDate(d.expiryDate)
            val vName = vehicles.firstOrNull { it.id == d.vehicleId }?.name ?: ""
            val title = "${labelOf(d.type)} $vName".trim()
            line("BEGIN:VEVENT"); line("UID:${d.id}@odocare"); line("DTSTAMP:$stamp")
            line("DTSTART;VALUE=DATE:${date.format(DateTimeFormatter.BASIC_ISO_DATE)}")
            line("DTEND;VALUE=DATE:${date.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)}")
            line("SUMMARY:" + icsEscape("Jatuh tempo: $title"))
            // Pengingat per dokumen (hari/bulan/tahun sebelum jatuh tempo) + hari-H
            val offsets = (Reminders.parse(d.reminders).map { ChronoUnit.DAYS.between(it.dateFor(date), date).toInt() } + 0).filter { it >= 0 }.distinct()
            offsets.forEach { n ->
                line("BEGIN:VALARM"); line("ACTION:DISPLAY"); line("DESCRIPTION:" + icsEscape(if (n == 0) "Hari ini jatuh tempo: $title" else "$title jatuh tempo $n hari lagi"))
                // Event seharian mulai 00.00; alarm 09.00 pada H-n = (n-1) hari + 15 jam sebelum mulai
                if (n == 0) line("TRIGGER;RELATED=START:PT9H") else line("TRIGGER:-P${n - 1}DT15H")
                line("END:VALARM")
            }
            line("END:VEVENT")
        }
        line("END:VCALENDAR")
        return sb.toString()
    }
}
