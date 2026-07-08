package com.orb.hubble.utils

import android.net.Uri
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.TextOverlay
import kotlin.Result

interface VideoExporter {
    suspend fun exportVideo(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        overlayTexts: List<TextOverlay>,
        overlayStickers: List<OverlaySticker>,
        outputPath: String
    ): Result<String>
}