package com.orb.hubble.ui.utils

import androidx.compose.ui.geometry.Offset
import com.orb.hubble.ui.components.FieldStyle
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.TextOverlay

data class ScaledOverlayText(
    val text: String,
    val scaledOffset: Offset,
    val scaledFontSizePx: Float,
    val roundedCorner: Float,
    val containerColor: Int,
    val fieldStyle: FieldStyle,
    val constraintWidth: Int,
    val textPaddingX: Float,
    val textPaddingY: Float,
    val rotation: Float,
    val scale: Float,
    val textColor: Int,
    val cornerRadius: Float,
    val bitmapScale: Float,
    val originalTextOverlay: TextOverlay
)

data class ScaledOverlaySticker(
    val resId: Int,
    val scaledOffset: Offset,
    val constraintWidth: Int,
    val rotation: Float,
    val scale: Float,
    val originalOverlaySticker: OverlaySticker
)