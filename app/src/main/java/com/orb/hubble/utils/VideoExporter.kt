package com.orb.hubble.utils

import android.net.Uri
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.TextOverlay

interface VideoExporter {
    suspend fun exportVideo(
        inputUri: Uri,
        overlayTexts: List<TextOverlay>,
        overlayStickers: List<OverlaySticker>,
        outputPath: String
    ): Result<String>
}