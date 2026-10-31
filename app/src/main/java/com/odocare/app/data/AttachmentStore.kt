package com.odocare.app.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Lampiran dokumen (foto atau file seperti PDF) disalin ke filesDir/doc_files. Foto dikecilkan lewat [ImageStore]. */
object AttachmentStore {
    private const val MAX_BYTES = 20L * 1024 * 1024

    private fun dir(ctx: Context) = File(ctx.filesDir, "doc_files").apply { mkdirs() }

    private fun displayName(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) { null }

    /** Mengembalikan (path, nama tampilan), atau null bila gagal / file terlalu besar (maks. 20 MB). */
    fun save(ctx: Context, uri: Uri): Pair<String, String>? {
        return try {
            val name = displayName(ctx, uri) ?: "lampiran"
            val mime = ctx.contentResolver.getType(uri) ?: ""
            if (mime.startsWith("image/")) {
                ImageStore.save(ctx, uri)?.let { it to name }
            } else {
                val ext = name.substringAfterLast('.', "").lowercase().filter { it.isLetterOrDigit() }.take(8).ifEmpty { "bin" }
                val file = File(dir(ctx), "doc_${UUID.randomUUID()}.$ext")
                var total = 0L
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            val n = input.read(buf); if (n < 0) break
                            total += n
                            if (total > MAX_BYTES) { file.delete(); return null }
                            out.write(buf, 0, n)
                        }
                    }
                } ?: return null
                file.absolutePath to name
            }
        } catch (e: Exception) { null }
    }

    fun isImage(path: String?): Boolean = path != null && path.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif")

    /** Buka lampiran dengan aplikasi lain (galeri, PDF viewer, dst). Mengembalikan false bila tidak ada aplikasi yang bisa membukanya. */
    fun open(ctx: Context, path: String): Boolean {
        val f = File(path)
        if (!f.exists()) return false
        return try {
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
            val view = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            ctx.startActivity(Intent.createChooser(view, "Buka lampiran").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) { false } catch (e: Exception) { false }
    }
}
