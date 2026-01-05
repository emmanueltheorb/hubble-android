package com.orb.hubble.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.StickerSource
import com.orb.hubble.ui.screens.TextOverlay
import kotlin.math.roundToInt

class MediaRenderCalculator(
    private val density: Float
) {
    companion object {
        private const val TEXT_PADDING_DP = 8f
        private const val ROUNDED_CORNER_DP = 16f
    }

    fun calculateScaleFactors(
        bitmapWidth: Int,
        bitmapHeight: Int,
        templateSize: IntSize
    ): ScaleFactors {
        val scaleX = if (templateSize.width > 0) bitmapWidth.toFloat() / templateSize.width else 1f
        val scaleY =
            if (templateSize.height > 0) bitmapHeight.toFloat() / templateSize.height else 1f

        val bitmapScale = (scaleX + scaleY) / 2

        return ScaleFactors(
            scaleX = scaleX,
            scaleY = scaleY,
            bitmapScale = bitmapScale
        )
    }

    fun calculateScaledTextOverlay(
        overlayText: TextOverlay,
        scaleFactors: ScaleFactors,
        templateSize: IntSize
    ): ScaledOverlayText {
        val (scaleX, scaleY, bitmapScale) = scaleFactors

        val scaledOffset = Offset(
            x = (overlayText.offsetRatioX * templateSize.width) * scaleX,
            y = (overlayText.offsetRatioY * templateSize.height) * scaleY
        )

        val textPaddingPx = TEXT_PADDING_DP * density
        val textPaddingBitmapX = textPaddingPx * scaleX
        val textPaddingBitmapY = textPaddingPx * scaleY

        val scaledFontSize = overlayText.fontSize * bitmapScale

        val paddingDp = TEXT_PADDING_DP * 2
        val paddingPx = paddingDp * density
        val constraintWidth =
            ((templateSize.width / overlayText.scale) * scaleX - paddingPx * scaleX)
                .roundToInt()
                .coerceAtLeast(1)

        return ScaledOverlayText(
            text = overlayText.text,
            scaledOffset = scaledOffset,
            scaledFontSizePx = scaledFontSize.toPx(),
            roundedCorner = 13f,
            containerColor = overlayText.config.containerColor.toArgb(),
            fieldStyle = overlayText.config.fieldStyle,
            constraintWidth = constraintWidth,
            textPaddingX = textPaddingBitmapX,
            textPaddingY = textPaddingBitmapY,
            rotation = overlayText.rotation,
            scale = overlayText.scale,
            textColor = overlayText.config.textStyle.color.toArgb(),
            cornerRadius = ROUNDED_CORNER_DP * density * bitmapScale,
            bitmapScale = bitmapScale,
            originalTextOverlay = overlayText
        )
    }

    fun calculateScaledStickerOverlay(
        overlaySticker: OverlaySticker,
        scaleFactors: ScaleFactors,
        templateSize: IntSize
    ): ScaledOverlaySticker {
        val (scaleX, scaleY, bitmapScale) = scaleFactors

        // 1. Calculate the target width in pixels on the final image
        // using the normalized width we saved in Step 2.
        val targetStickerWidthPx = templateSize.width * overlaySticker.normalizedWidth

        // 2. Calculate coordinates
        // The overlaySticker.offset is Top-Left.
        // We need to find the Center point for the Canvas rotation logic.
        // CenterX = TopLeftX + (Width / 2)
        val topLeftX = (overlaySticker.offsetRatioX * templateSize.width) * scaleX
        val topLeftY = (overlaySticker.offsetRatioY * templateSize.height) * scaleY

        // Note: We use targetStickerWidthPx for centering offset
        // Assuming aspect ratio is preserved, Height is derived later,
        // but for positioning center, we can defer or calculate geometric center.
        // For now, let's pass the TopLeft derived offset and handle centering in drawSticker

        val scaledOffset = Offset(x = topLeftX, y = topLeftY)

        return when (val source = overlaySticker.source) {
            is StickerSource.Resource -> ScaledOverlaySticker(
                resId = source.resId,
                scaledOffset = scaledOffset,
                constraintWidth = targetStickerWidthPx.toInt(), // Pass target width here
                rotation = overlaySticker.rotation,
                scale = overlaySticker.scale,
                originalOverlaySticker = overlaySticker
            )
            // Handle other cases...
            else -> throw NotImplementedError()
        }
    }
}

data class ScaleFactors(
    val scaleX: Float,
    val scaleY: Float,
    val bitmapScale: Float
)