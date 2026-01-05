@file:OptIn(ExperimentalUuidApi::class)

package com.orb.hubble.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orb.hubble.repository.PostRepository
import com.orb.hubble.ui.components.FieldStyle
import com.orb.hubble.ui.components.OverlayTextConfig
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.TextOverlay
import com.orb.hubble.ui.theme.NunitoFontFamily
import com.orb.hubble.utils.MediaExporter
import com.orb.hubble.utils.SaveToStorageStrategy
import com.orb.hubble.utils.StorageShareSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import okio.FileNotFoundException
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@HiltViewModel
class CreatePostScreenViewModel @Inject constructor(
    private val mediaExporter: StorageMediaExporter,
    private val videoExporter: VideoMediaExporter,
    private val storageStrategy: SaveToStorageStrategy,
    private val shareSheet: StorageShareSheet,
    private val postRepository: PostRepository
) : ViewModel() {
    private var hasLoadedInitialData = false

    private var uploadJob: Job? = null

    private val _state = MutableStateFlow(CreatePostScreenState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                /** Load initial data here **/
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CreatePostScreenState()
        )

    fun uploadFile(filePath: String, fileName: String) {
        uploadJob = postRepository
            .uploadFile(filePath, fileName)
            .onStart {
                _state.update {
                    it.copy(
                        isUploading = true,
                        isUploadComplete = false,
                        errorMessage = null,
                        progress = 0f
                    )
                }
            }
            .onEach { progressUpdate ->
                _state.update {
                    val calculatedProgress = if (progressUpdate.totalBytes > 0) {
                        progressUpdate.bytesSent / progressUpdate.totalBytes.toFloat()
                    } else 0f
                    it.copy(
                        progress = calculatedProgress
                    )
                }
            }
            .onCompletion { cause ->
                if (cause == null) {
                    _state.update {
                        it.copy(
                            isUploading = false,
                            isUploadComplete = true
                        )
                    }
                } else if (cause is CancellationException) {
                    _state.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = "The upload was cancelled!",
                            isUploadComplete = false,
                            progress = 0f
                        )
                    }
                }
            }
            .catch { cause ->
                Log.e("UploadError", "Full exception: ", cause)
                val message = when (cause) {
                    is OutOfMemoryError -> "File too large!"
                    is FileNotFoundException -> "File not found!"
                    is UnresolvedAddressException -> "No internet!"
                    else -> "${cause.message}"
                }
                _state.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = message
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun cancelUpload() {
        uploadJob?.cancel()
    }

    fun transformOverlayText(
        id: String,
        offset: Offset,
        rotation: Float,
        scale: Float
    ) {
        _state.update {
            val (width, height) = it.mediaSize
            it.copy(
                overlayTexts = it.overlayTexts.map { overlayText ->
                    if (overlayText.id == id) {
                        overlayText.copy(
                            offsetRatioX = offset.x / width,
                            offsetRatioY = offset.y / height,
                            rotation = rotation,
                            scale = scale
                        )
                    } else overlayText
                }
            )
        }
    }

    fun transformOverlaySticker(
        id: String,
        offset: Offset,
        rotation: Float,
        scale: Float,
        parentSize: Size,
        currentStickerBaseSize: Size
    ) {
        _state.update {
            val (width, height) = parentSize
            it.copy(
                overlayStickers = it.overlayStickers.map { overlaySticker ->
                    // Calculate the current visual width relative to the parent width
                    val relativeWidth = (currentStickerBaseSize.width * scale) / width

                    if (overlaySticker.id == id) {
                        overlaySticker.copy(
                            offsetRatioX = offset.x / width,
                            offsetRatioY = offset.y / height,
                            rotation = rotation,
                            scale = scale,
                            normalizedWidth = relativeWidth // Store the essential size data
                        )
                    } else overlaySticker
                }
            )
        }
    }

    fun addText() {
        val id = Uuid.random().toString()

        val overlayText = TextOverlay(
            id = id,
            text = "",
            offsetRatioX = 0.45f,
            offsetRatioY = 0.48f,
            config = OverlayTextConfig(
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontFamily = NunitoFontFamily,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                ),
                fieldStyle = FieldStyle.Filled,
                containerColor = Color.White
            )
        )

        _state.update {
            it.copy(
                overlayTexts = it.overlayTexts + overlayText,
                overlayInteraction = OverlayInteraction.Selected(id),
                selectedTextOverlay = overlayText
            )
        }
    }

    fun addSticker(sticker: OverlaySticker) {
        _state.update {
            it.copy(
                overlayStickers = it.overlayStickers + sticker
            )
        }
    }

    fun deleteOverlay(id: String) {
        _state.update {
            it.copy(
                overlayTexts = it.overlayTexts.filter { overlayText ->
                    overlayText.id != id
                }
            )
        }
    }

    fun selectOverlay(id: String) {
        _state.update {
            it.copy(
                overlayInteraction = OverlayInteraction.Selected(id),
                selectedTextOverlay = it.overlayTexts.first { overlayText ->
                    overlayText.id == id
                }
            )
        }
    }

    fun updateText(id: String, text: String) {
        _state.update {
//            it.copy(
//                overlayTexts = it.overlayTexts.map { overlayText ->
//                    if (overlayText.id == id) {
//                        overlayText.copy(
//                            text = text
//                        )
//                    } else overlayText
//                }
//            )
            val updatedOverlayTexts = it.overlayTexts.map { overlayText ->
                if (overlayText.id == id) {
                    overlayText.copy(text = text)
                } else overlayText
            }

            // Also update selectedTextOverlay if it's the same id
            val updatedSelectedOverlay = if (it.selectedTextOverlay.id == id) {
                it.selectedTextOverlay.copy(text = text)
            } else it.selectedTextOverlay

            it.copy(
                overlayTexts = updatedOverlayTexts,
                selectedTextOverlay = updatedSelectedOverlay
            )
        }
    }

    fun changeColor(id: String, color: Color) {
        _state.update {
            val updatedOverlayTexts = it.overlayTexts.map { overlayText ->
                if (overlayText.id == id) {
                    overlayText.copy(
                        config = overlayText.config.copy(containerColor = color)
                    )
                } else overlayText
            }

            // Also update selectedTextOverlay if it's the same id
            val updatedSelectedOverlay = if (it.selectedTextOverlay.id == id) {
                it.selectedTextOverlay.copy(
                    config = it.selectedTextOverlay.config.copy(containerColor = color)
                )
            } else it.selectedTextOverlay

            it.copy(
                overlayTexts = updatedOverlayTexts,
                selectedTextOverlay = updatedSelectedOverlay
            )
        }
    }

    fun updateTextConfig(id: String, config: OverlayTextConfig) {
        _state.update {
            it.copy(
                overlayTexts = it.overlayTexts.map { overlayText ->
                    if (overlayText.id == id) {
                        overlayText.copy(config = config)
                    } else overlayText
                }
            )
        }
    }

    fun editOverlay(id: String) {
        _state.update {
            it.copy(
                overlayInteraction = OverlayInteraction.Editing(id)
            )
        }
    }

    fun doneEditing() {
        _state.update {
            it.copy(
                overlayInteraction = OverlayInteraction.None
            )
        }
    }

    fun updateScreenSize(size: IntSize) {
        _state.update {
            it.copy(
                screenSize = size
            )
        }
    }

    fun updateContainerSize(size: IntSize) {
        _state.update {
            it.copy(
                mediaSize = size
            )
        }
    }

    fun saveMedia(mediaUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val backgroundMediaBytes = getBytesFromUri(mediaUri, context)
                val isVideo = context.contentResolver.getType(mediaUri)?.contains("video") == true
                val fileName = generateFileName(isVideo)
                val outputPath = storageStrategy.getFilePath(fileName)

                if (isVideo) {
                    videoExporter
                        .exportVideo(
                            inputUri = mediaUri,
                            overlayTexts = state.value.overlayTexts,
                            overlayStickers = state.value.overlayStickers,
                            outputPath = outputPath
                        )
                        .onSuccess { path ->
                            uploadFile(filePath = path, fileName = fileName)
                            //shareSheet.shareFile(path)
                        }
                        .onFailure { exception ->
                            Log.e("Export", "Video export failed", exception)
                        }
                } else {
                    mediaExporter
                        .exportMedia(
                            backgroundMediaBytes = backgroundMediaBytes,
                            overlayTexts = state.value.overlayTexts,
                            overlayStickers = state.value.overlayStickers,
                            templateSize = state.value.mediaSize,
                            screenSize = state.value.screenSize,
                            saveToStorageStrategy = storageStrategy,
                            fileName = fileName
                        )
                        .onSuccess {
                            uploadFile(filePath = it, fileName = fileName)
                            //shareSheet.shareFile(it)
                        }
                        .onFailure {
                            it.printStackTrace()
                        }

                }
            } catch (e: Exception) {
                Log.e("Uri loading Error", "Failed to load image: ${e.message}")
            }

        }
    }

    private fun generateFileName(isVideo: Boolean): String {
        return if (isVideo) {
            "media_${System.currentTimeMillis()}.mp4"
        } else {
            "media_${System.currentTimeMillis()}.jpg"
        }
    }

    private suspend fun getBytesFromUri(uri: Uri, context: Context): ByteArray =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: byteArrayOf()
        }
}

data class CreatePostScreenState(
    val screenSize: IntSize = IntSize.Zero,
    val mediaSize: IntSize = IntSize.Zero,
    val isLeavingWithoutSaving: Boolean = false,
    val overlayTexts: List<TextOverlay> = listOf(
        TextOverlay(
            id = "1",
            text = "Guh",
            offsetRatioX = 0.5f,
            offsetRatioY = 0.5f,
            config = OverlayTextConfig(
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontFamily = NunitoFontFamily,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                ),
                fieldStyle = FieldStyle.Filled,
                containerColor = Color(0xFF0056FE)
            )
        )
    ),
    val overlayStickers: List<OverlaySticker> = emptyList(),
    val selectedTextOverlay: TextOverlay = TextOverlay(
        id = Uuid.random().toString(),
        text = "",
        offsetRatioX = 0.45f,
        offsetRatioY = 0.48f,
        config = OverlayTextConfig(
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontFamily = NunitoFontFamily,
                textAlign = TextAlign.Center,
                color = Color.Black
            ),
            fieldStyle = FieldStyle.Filled,
            containerColor = Color.White
        )
    ),
    val overlayInteraction: OverlayInteraction = OverlayInteraction.None,
    val hasLeftEditor: Boolean = false,
    val isUploading: Boolean = false,
    val isUploadComplete: Boolean = false,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

sealed interface OverlayInteraction {
    data object None : OverlayInteraction
    data class Selected(val overlayId: String) : OverlayInteraction
    data class Editing(val overlayId: String) : OverlayInteraction
}