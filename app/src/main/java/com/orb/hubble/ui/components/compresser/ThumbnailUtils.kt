package com.orb.hubble.ui.components.compresser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.util.Log
import androidx.core.graphics.scale

object ThumbnailUtils {

    fun generateBase64Thumbnail(sourcedBitmap: Bitmap, targetSize: Int = 128, quality: Int = 50): String {
        val width = sourcedBitmap.width
        val height = sourcedBitmap.height

        val (newWidth, newHeight) = if (width > height) {
            val ratio = height.toFloat() / width.toFloat()
            targetSize to (targetSize * ratio).toInt().coerceAtLeast(1)
        } else {
            val ratio = width.toFloat() / height.toFloat()
            (targetSize * ratio).toInt().coerceAtLeast(1) to targetSize
        }

        val scaledBitmap = sourcedBitmap.scale(newWidth, newHeight)

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()

        if (scaledBitmap != sourcedBitmap) {
            scaledBitmap.recycle()
        }

        Log.d("Compress", "${Base64.encodeToString(byteArray, Base64.NO_WRAP)}")
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun decodeBase64Thumbnail(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}