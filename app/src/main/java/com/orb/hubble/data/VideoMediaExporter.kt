package com.orb.hubble.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.StickerSource
import com.orb.hubble.ui.screens.TextOverlay
import com.orb.hubble.utils.VideoExporter
import dagger.hilt.android.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.graphics.createBitmap
import com.orb.hubble.utils.StorageShareSheet
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VideoMediaExporter(
    private val context: Context
): VideoExporter {
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    override suspend fun exportVideo(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        overlayTexts: List<TextOverlay>,
        overlayStickers: List<OverlaySticker>,
        outputPath: String
    ): Result<String> = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val overlays = mutableListOf<TextureOverlay>()

            overlayStickers.forEach { sticker ->
                val bitmap = loadStickerBitmap(sticker) ?: return@forEach
                val m3X = (sticker.offsetRatioX * 2) - 1
                val m3Y = 1 - (sticker.offsetRatioY * 2)

                val settings = StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(m3X, m3Y)
                    .setRotationDegrees(-sticker.rotation)
                    .setScale(sticker.scale, sticker.scale)
                    .build()

                overlays.add(BitmapOverlay.createStaticBitmapOverlay(bitmap, settings))
            }

            overlayTexts.forEach { textOverlay ->
                val textBitmap = createBitmapFromText(textOverlay)
                val m3X = (textOverlay.offsetRatioX * 2) - 1
                val m3Y = 1 - (textOverlay.offsetRatioY * 2)

                val settings = StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(m3X, m3Y)
                    .setRotationDegrees(-textOverlay.rotation)
                    .build()

                overlays.add(BitmapOverlay.createStaticBitmapOverlay(textBitmap, settings))
            }

            val transformerListener: Transformer.Listener =
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
//                        continuation.resumeWith(Result.success(Result.success(outputPath)))
                        continuation.resume(Result.success(outputPath))
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        continuation.resume(Result.failure(exportException))
                    }
                }

            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H265)
                .addListener(transformerListener)
                .build()

            val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(clippingConfig)
                .build()

            val overlayEffect = OverlayEffect(ImmutableList.copyOf(overlays))
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(ImmutableList.of(), listOf(overlayEffect)))
                .build()

            Handler(Looper.getMainLooper()).post {
                try {
                    transformer.start(editedMediaItem, outputPath)
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }

            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post { transformer.cancel() }
            }
        }
    }

    private fun loadStickerBitmap(sticker: OverlaySticker): Bitmap? {
        return when (sticker.source) {
            is StickerSource.Resource -> BitmapFactory.decodeResource(context.resources, sticker.source.resId)
            is StickerSource.Uri -> context.contentResolver.openInputStream(sticker.source.uri)?.use { BitmapFactory.decodeStream(it) }
            else -> null
        }
    }

    private fun createBitmapFromText(text: TextOverlay): Bitmap {
        val paint = TextPaint().apply {
            textSize = 64f
            color = text.config.textStyle.color.value.toInt()
        }
        val width = paint.measureText(text.text).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, 100)
        val canvas  = Canvas(bitmap)
        canvas.drawText(text.text, 0f, 80f, paint)
        return bitmap
    }
}