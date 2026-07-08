@file:OptIn(ExperimentalUuidApi::class)

package com.orb.hubble.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
import com.orb.hubble.ui.components.FieldStyle
import com.orb.hubble.ui.components.OverlayTextConfig
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.TextOverlay
import com.orb.hubble.ui.theme.NunitoFontFamily
import com.orb.hubble.utils.SaveToStorageStrategy
import com.orb.hubble.utils.StorageShareSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import androidx.core.graphics.scale
import com.orb.hubble.repository.HubbleRepository
import java.io.File

@HiltViewModel
class CreatePostScreenViewModel @Inject constructor(
    private val mediaExporter: StorageMediaExporter,
    private val videoExporter: VideoMediaExporter,
    private val storageStrategy: SaveToStorageStrategy,
    private val shareSheet: StorageShareSheet,
    private val repository: HubbleRepository
) : ViewModel() {

    private var uploadJob: Job? = null

    private val _state = MutableStateFlow(CreatePostScreenState())
    val state = _state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CreatePostScreenState()
        )

    private val _postState = MutableStateFlow(
        PostState(
            userId = "f0c7cfbc-604b-437e-bcce-2f6e17e063b7",
            pageId = "25ff15a5-4728-42b4-a2e9-171662b949f5",
            type = "video"
        )
    )

    val postState = _postState.asStateFlow()

    private val _thumbnails = MutableStateFlow<List<Bitmap>>(emptyList())
    val thumbnails: StateFlow<List<Bitmap>> = _thumbnails.asStateFlow()

    private val _isLoadingThumbnails = MutableStateFlow(false)
    val isLoadingThumbnails = _isLoadingThumbnails.asStateFlow()

    fun createMediaPost(filePath: String, fileName: String, postState: PostState) {
        uploadJob = repository
            .createPost(filePath, fileName, postState)
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
                    val deleted = File(filePath).delete()
                    if (deleted) {
                        Log.d("Storage", "Successfully deleted cached file: $filePath")
                    } else {
                        Log.e("Storage", "Failed to delete cached file: $filePath")
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

    fun createTextPost(postState: PostState) {
        uploadJob = repository
            .createTextPost(postState)
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
                    else -> "Something went wrong"
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
        positionX: Float,
        positionY: Float,
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
                            positionX = positionX,
                            positionY = positionY,
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
        renderOffset: Offset,
        rotation: Float,
        scale: Float,
        parentSize: Size,
        normalizedWidth: Float
    ) {
        _state.update {
            val (width, height) = parentSize
            it.copy(
                overlayStickers = it.overlayStickers.map { overlaySticker ->

                    if (overlaySticker.id == id) {
                        overlaySticker.copy(
                            offsetRatioX = offset.x / width,
                            offsetRatioY = offset.y / height,
                            positionX = renderOffset.x,
                            positionY = renderOffset.y,
                            rotation = rotation,
                            scale = scale,
                            normalizedWidth = normalizedWidth
                        )
                    } else overlaySticker
                }
            )
        }
    }

    fun generateThumbnails(
        context: Context,
        videoUri: Uri,
        thumbnailCount: Int = 9
    ) {
        if (_isLoadingThumbnails.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingThumbnails.value = true
            _thumbnails.value = emptyList()

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val durationMs =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLong() ?: 0L
                val interval = durationMs / thumbnailCount

                for (i in 0 until thumbnailCount) {
                    val timeUs = i * interval * 1000
                    val bitmap =
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    bitmap?.let {
                        val scaled = it.scale(150, 150, false)
                        _thumbnails.update { currentList ->
                            currentList + scaled
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
                _isLoadingThumbnails.value = false
            }
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

    fun uploadPost(mediaUri: Uri, context: Context, startMs: Long, endMs: Long) {
        viewModelScope.launch {
            try {
                val isVideo = context.contentResolver.getType(mediaUri)?.contains("video") == true
                val isImage = context.contentResolver.getType(mediaUri)?.contains("image") == true
                val fileName = generateFileName(isVideo)

                val isUneditedVideo = if (isVideo) {
                    val overlayTexts = state.value.overlayTexts
                    val overlayStickers = state.value.overlayStickers

                    val originalDuration = getLocalVideoDuration(context, mediaUri)

                    overlayTexts.isEmpty() && overlayStickers.isEmpty() && (originalDuration > 0 && (endMs - startMs) >= (originalDuration - 100))
                } else {
                    false
                }

                if (isUneditedVideo) {
                    Log.d("Export", "Skipping Transformer - Uploading Original")

                    val cachedFile = File(storageStrategy.getFilePath(fileName))
                    context.contentResolver.openInputStream(mediaUri)?.use { input ->
                        cachedFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    createMediaPost(
                        filePath = cachedFile.absolutePath,
                        fileName = fileName,
                        postState = _postState.value.copy(type = "video")
                        )

                } else {
                    val outputPath = storageStrategy.getFilePath(fileName)

                    if (isVideo) {
                        videoExporter
                            .exportVideo(
                                inputUri = mediaUri,
                                startMs = startMs,
                                endMs = endMs,
                                overlayTexts = state.value.overlayTexts,
                                overlayStickers = state.value.overlayStickers,
                                outputPath = outputPath
                            )
                            .onSuccess { path ->
                                createMediaPost(
                                    filePath = path,
                                    fileName = fileName,
                                    postState = _postState.value.copy(type = "video")
                                )
                                //shareSheet.shareFile(path)
                            }
                            .onFailure { exception ->
                                Log.e("Export", "Video export failed", exception)
                            }
                    } else if (isImage) {
                        val backgroundMediaBytes = getBytesFromUri(mediaUri, context)

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
                                createMediaPost(
                                    filePath = it,
                                    fileName = fileName,
                                    postState = _postState.value.copy(type = "image")
                                )
                                //shareSheet.shareFile(it)
                            }
                            .onFailure {
                                it.printStackTrace()
                            }
                    } else {
                        createTextPost(
                            postState = _postState.value.copy(type = "text")
                        )
                        //call createTextPost here
                    }
                }
            } catch (e: Exception) {
                Log.e("Uri loading Error", "Failed to load image: ${e.message}")
            }

        }
    }

    private fun generateFileName(isVideo: Boolean): String {
        return if (isVideo) {
            "media_${Uuid.random()}.mp4"
            //"media_video.mp4"
        } else {
            "media_${Uuid.random()}.jpg"
            //"media_image.jpg"
        }
    }

    private fun getLocalVideoDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    private suspend fun getBytesFromUri(uri: Uri, context: Context): ByteArray =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: byteArrayOf()
        }

    fun updateDescription(value: String) {
        _postState.update {
            it.copy(
                description = value
            )
        }
    }

    fun setPostType(type: String) {
        _postState.update {
            it.copy(
                type = type
            )
        }
    }
}

data class CreatePostScreenState(
    val screenSize: IntSize = IntSize.Zero,
    val mediaSize: IntSize = IntSize.Zero,
    val isLeavingWithoutSaving: Boolean = false,
    val overlayTexts: List<TextOverlay> = emptyList(),
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

data class PostState(
    val userId: String,
    val pageId: String,
    val description: String = "",
    val content: String = "",
    val contentText: String = "",
    val contentUrl: String = "",
    val thumbnailUrl: String = "",
    val thumbnailAt: String = "1",
    val type: String,
    val tags: List<String> = emptyList()
)

sealed interface OverlayInteraction {
    data object None : OverlayInteraction
    data class Selected(val overlayId: String) : OverlayInteraction
    data class Editing(val overlayId: String) : OverlayInteraction
}