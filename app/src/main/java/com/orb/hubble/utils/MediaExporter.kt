package com.orb.hubble.utils

import androidx.compose.ui.unit.IntSize
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.TextOverlay
import kotlin.Result
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface MediaExporter {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun exportMedia(
        backgroundMediaBytes: ByteArray,
        overlayTexts: List<TextOverlay>,
        overlayStickers: List<OverlaySticker>,
        screenSize: IntSize,
        templateSize: IntSize,
        saveToStorageStrategy: SaveToStorageStrategy,
        fileName: String = "post_${Uuid.random()}.jpg"
    ): Result<String>
}

interface SaveToStorageStrategy {
    fun getFilePath(fileName: String): String
}