package com.odocare.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Foto bukti servis disimpan di penyimpanan internal aplikasi (filesDir/service_images), sudah dikecilkan. */
object ImageStore {
    private const val MAX_SIDE = 1600

    private fun dir(ctx: Context) = File(ctx.filesDir, "service_images").apply { mkdirs() }

    /** Salin gambar pilihan user ke penyimpanan app. Mengembalikan path file, atau null bila gagal. */
    fun save(ctx: Context, uri: Uri): String? {
        return try {
            val resolver = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

            // Hormati orientasi EXIF (foto kamera sering tersimpan miring)
            val rotation = resolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            val longest = maxOf(bmp.width, bmp.height)
            val scale = if (longest > MAX_SIDE) MAX_SIDE.toFloat() / longest else 1f
            if (rotation != 0f || scale != 1f) {
                val m = Matrix().apply { postRotate(rotation); postScale(scale, scale) }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            val file = File(dir(ctx), "img_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Muat bitmap dengan sampling agar hemat memori. */
    fun load(path: String, maxSide: Int): Bitmap? {
        val f = File(path)
        if (!f.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) { if (path != null) try { File(path).delete() } catch (_: Exception) {} }
}
