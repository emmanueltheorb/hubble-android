package com.orb.hubble.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withTranslation
import com.orb.hubble.R
import com.orb.hubble.ui.components.FieldStyle
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.StickerSource
import com.orb.hubble.ui.screens.TextOverlay
import com.orb.hubble.ui.utils.MediaRenderCalculator
import com.orb.hubble.ui.utils.ScaleFactors
import com.orb.hubble.ui.utils.ScaledOverlaySticker
import com.orb.hubble.ui.utils.ScaledOverlayText
import com.orb.hubble.utils.MediaExporter
import com.orb.hubble.utils.SaveToStorageStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class StorageMediaExporter @Inject constructor(
    private val context: Context
) : MediaExporter {

    private val mediaRenderCalculator = MediaRenderCalculator(
        density = context.resources.displayMetrics.density
    )

    override suspend fun exportMedia(
        backgroundMediaBytes: ByteArray,
        overlayTexts: List<TextOverlay>,
        overlayStickers: List<OverlaySticker>,
        screenSize: IntSize,
        templateSize: IntSize,
        saveToStorageStrategy: SaveToStorageStrategy,
        fileName: String
    ) = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        var outputBitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeByteArray(
                backgroundMediaBytes,
                0,
                backgroundMediaBytes.size
            )
            outputBitmap = renderMedia(
                background = bitmap,
                overlayTexts = overlayTexts,
                overlayStickers = overlayStickers,
                templateSize = templateSize,
                screenSize = screenSize
            )

            val filePath = saveToStorageStrategy.getFilePath(fileName)
            val file = File(filePath)
            FileOutputStream(file).use { out ->
                outputBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    out
                )
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Result.failure(e)
        } finally {
            bitmap?.recycle()
            outputBitmap?.recycle()
        }
    }

    private suspend fun renderMedia(
        background: Bitmap,
        overlayTexts: List<TextOverlay>,
        overlayStickers: List<OverlaySticker>,
        templateSize: IntSize,
        screenSize: IntSize
    ): Bitmap = withContext(Dispatchers.Default) {
        val output = background.copy(
            Bitmap.Config.ARGB_8888,
            true
        )

        val canvas = Canvas(output)

        val scaleFactors = mediaRenderCalculator.calculateScaleFactors(
            bitmapWidth = background.width,
            bitmapHeight = background.height,
            templateSize = templateSize
        )

        val scaledOverlayStickers = overlayStickers.map {
            mediaRenderCalculator.calculateScaledStickerOverlay(
                overlaySticker = it,
                scaleFactors = scaleFactors,
                templateSize = templateSize
            )
        }

        val scaledOverlayTexts = overlayTexts.map {
            mediaRenderCalculator.calculateScaledTextOverlay(
                overlayText = it,
                scaleFactors = scaleFactors,
                templateSize = templateSize
            )
        }

        // Draw stickers first (so text appears on top)
        scaledOverlayStickers.forEach { sticker ->
            drawSticker(canvas, sticker, scaleFactors, templateSize)
        }

        scaledOverlayTexts.forEach { scaledOverlayText ->
            drawText(canvas, scaledOverlayText)
        }

        output
    }

    private fun drawSticker(
        canvas: Canvas,
        sticker: ScaledOverlaySticker,
        scaleFactors: ScaleFactors,
        templateSize: IntSize
    ) {
        // 1. Load the raw bitmap (might be 3000px wide)
        val rawBitmap = when (sticker.originalOverlaySticker.source) {
            is StickerSource.Resource -> BitmapFactory.decodeResource(context.resources, sticker.originalOverlaySticker.source.resId)
            is StickerSource.Uri -> context.contentResolver.openInputStream(sticker.originalOverlaySticker.source.uri)?.use { BitmapFactory.decodeStream(it) }
            else -> null
        } ?: return

        // 2. Calculate the SCALE needed to make the raw bitmap match the UI size
        // sticker.constraintWidth was calculated in Step 3 based on normalizedWidth
        val targetWidth = sticker.constraintWidth.toFloat()
        val renderScale = targetWidth / rawBitmap.width.toFloat()

        // Calculate target height to maintain aspect ratio
        val targetHeight = rawBitmap.height * renderScale

        // 3. Fix Alignment: Convert Top-Left (stored in scaledOffset) to Center
        val centerX = sticker.scaledOffset.x + (targetWidth / 2f)
        val centerY = sticker.scaledOffset.y + (targetHeight / 2f)

        canvas.withTranslation(centerX, centerY) {
            rotate(sticker.rotation)
            // DO NOT use sticker.scale here! The 'renderScale' already accounts
            // for the final visual size (which includes the user's zoom).
            scale(renderScale, renderScale)

            // 4. Draw Centered relative to the translation point
            val left = -rawBitmap.width / 2f
            val top = -rawBitmap.height / 2f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                if (sticker.originalOverlaySticker.alpha < 1f) {
                    alpha = (sticker.originalOverlaySticker.alpha * 255).toInt()
                }
                isFilterBitmap = true
            }

            drawBitmap(rawBitmap, left, top, paint)
        }

        rawBitmap.recycle()
    }

    private fun drawText(canvas: Canvas, overlayText: ScaledOverlayText) {
        val nunitoTypeFace = ResourcesCompat.getFont(context, R.font.nunito_regular)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textSize = overlayText.scaledFontSizePx
            typeface = nunitoTypeFace
            color = overlayText.textColor
        }

        val fillLayout = StaticLayout.Builder.obtain(
            overlayText.text,
            0,
            overlayText.text.length,
            TextPaint(fillPaint),
            overlayText.constraintWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()

        val textHeight = fillLayout.height.toFloat()
        val textWidth = (0 until fillLayout.lineCount)
            .maxOfOrNull { fillLayout.getLineWidth(it) }
            ?: 0f

        val boxWidth = textWidth + overlayText.textPaddingX * 2
        val boxHeight = textHeight + overlayText.textPaddingY * 2

        val centerX = overlayText.scaledOffset.x + boxWidth / 2f
        val centerY = overlayText.scaledOffset.y + boxHeight / 2f

        canvas.withTranslation(centerX, centerY) {
            scale(overlayText.scale, overlayText.scale)
            rotate(overlayText.rotation)

            val textCenteringOffset = (overlayText.constraintWidth - textWidth) / 2f

            // Draw rounded background if needed
            if (overlayText.fieldStyle != FieldStyle.Default) {
                val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = overlayText.containerColor
                }

                val strokePaint = if (overlayText.fieldStyle == FieldStyle.Border) {
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 2f * overlayText.bitmapScale
                        color = overlayText.containerColor
                    }
                } else null

                val left = -boxWidth / 2f
                val top = -boxHeight / 2f
                val right = boxWidth / 2f
                val bottom = boxHeight / 2f

                // Draw background
                when (overlayText.fieldStyle) {
                    FieldStyle.Filled -> {
                        drawRoundRect(
                            left, top, right, bottom,
                            overlayText.cornerRadius, overlayText.cornerRadius,
                            backgroundPaint
                        )
                    }
                    FieldStyle.SoftFilled -> {
                        backgroundPaint.alpha = (255 * 0.3f).toInt()
                        drawRoundRect(
                            left, top, right, bottom,
                            overlayText.cornerRadius, overlayText.cornerRadius,
                            backgroundPaint
                        )
                    }
                    FieldStyle.Border -> {
                        strokePaint?.let { strokePaint ->
                            drawRoundRect(
                                left, top, right, bottom,
                                overlayText.cornerRadius, overlayText.cornerRadius,
                                strokePaint
                            )
                        }
                    }
                    else -> {
                        // No background for Default style
                    }
                }
            }

            translate(
                -boxWidth / 2f + overlayText.textPaddingX - textCenteringOffset,
                -boxHeight / 2f + overlayText.textPaddingY
            )

            fillLayout.draw(this)
        }
    }
}

private inline fun Canvas.withTranslation2(
    x: Float,
    y: Float,
    block: Canvas.() -> Unit
) {
    val checkpoint = save()
    translate(x, y)
    try {
        block()
    } finally {
        restoreToCount(checkpoint)
    }
}