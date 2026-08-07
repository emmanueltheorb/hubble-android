package com.orb.hubble.ui.components.compresser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class ImageCompressor(
    private val context: Context
) {
    suspend fun generateBase64Thumbnail(
        filePath: String,
        targetSize: Int = 128,
        quality: Int = 50,
        isVideo: Boolean
    ): String? = withContext(Dispatchers.IO) {
        val file = File(filePath)

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val sourcedBitmap = if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, contentUri)
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                retriever.release()
            }
        } else {
            // Decode directly from the stream, avoiding full byte array memory allocation
            context.contentResolver.openInputStream(contentUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        }

        if (sourcedBitmap == null) return@withContext null

        withContext(Dispatchers.Default) {
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

            if (scaledBitmap != sourcedBitmap) {
                scaledBitmap.recycle()
            }

            //Log.d("Compress", "${Base64.encodeToString(byteArray, Base64.NO_WRAP)}")
            sourcedBitmap.recycle()

            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }
    }

    suspend fun generateBase64Thumbnail(
        contentUri: Uri,
        targetSize: Int = 128,
        quality: Int = 50,
        isVideo: Boolean
    ): String? {
        return withContext(Dispatchers.IO) {
            val sourcedBitmap = if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, contentUri)
                    retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } finally {
                    retriever.release()
                }
            } else {
                context.contentResolver.openInputStream(contentUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }

            if (sourcedBitmap == null) return@withContext null

            withContext(Dispatchers.Default) {
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

                sourcedBitmap.recycle()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            }
        }
    }

    suspend fun generateBase64Thumbnail(
        sourcedBitmap: Bitmap,
        targetSize: Int = 128,
        quality: Int = 50
    ): String? {
        return withContext(Dispatchers.Default) {

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

            sourcedBitmap.recycle()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }
    }

    suspend fun decodeBase64Thumbnail(
        base64String: String
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

//    suspend fun compressImage(
//        contentUri: Uri,
//        compressionThreshold: Long
//    ): ByteArray? {
//        return withContext(Dispatchers.IO) {
//            val mimeType = context.contentResolver.getType(contentUri)
//            val inputBytes = context
//                .contentResolver
//                .openInputStream(contentUri)
//                ?.use { inputStream ->
//                    inputStream.readBytes()
//                } ?: return@withContext null
//
//            ensureActive()
//
//            withContext(Dispatchers.Default) {
//                val bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)
//
//                ensureActive()
//
//                val compressFormat = when (mimeType) {
//                    "image/png" -> Bitmap.CompressFormat.PNG
//                    "image/jpeg" -> Bitmap.CompressFormat.JPEG
//                    "image/webp" -> if (Build.VERSION.SDK_INT >= 30) {
//                        Bitmap.CompressFormat.WEBP_LOSSLESS
//                    } else Bitmap.CompressFormat.WEBP
//
//                    else -> Bitmap.CompressFormat.JPEG
//                }
//
//                var outputBytes: ByteArray
//                var quality = 90
//
//                do {
//                    ByteArrayOutputStream().use { outputStream ->
//                        bitmap.compress(compressFormat, quality, outputStream)
//                        outputBytes = outputStream.toByteArray()
//                        quality -= (quality * 0.1).roundToInt()
//                    }
//                } while (
//                    isActive &&
//                    outputBytes.size > compressionThreshold &&
//                    quality > 5 &&
//                    compressFormat != Bitmap.CompressFormat.PNG
//                )
//
//                outputBytes
//            }
//        }
//    }
}