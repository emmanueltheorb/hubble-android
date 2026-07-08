package com.orb.hubble.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import com.orb.hubble.data.CreatePostScreenViewModel
import com.orb.hubble.ui.components.DraggableContainer
import com.orb.hubble.ui.components.OverlayStickers
import com.orb.hubble.ui.components.OverlayTextConfig
import com.orb.hubble.ui.components.OverlayTextField
import com.orb.hubble.ui.components.TextColorPicker
import com.orb.hubble.ui.components.getVideoDuration
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Typography
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    modifier: Modifier = Modifier,
    viewModel: CreatePostScreenViewModel = hiltViewModel(),
    mediaUri: String
) {
    val uiState by viewModel.state.collectAsState()
    val postState by viewModel.postState.collectAsState()
    val editing = remember { mutableStateOf(false) }
    val addSticker = remember { mutableStateOf(false) }
    val screenHeight = LocalWindowInfo.current.containerSize.height.dp
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val screenSize = LocalWindowInfo.current.containerSize

    val context = LocalContext.current
    val uri = remember { mediaUri.toUri() }
    val isVideo = remember(uri) {
        val mimeType = context.contentResolver.getType(uri)
        mimeType?.startsWith("video/", ignoreCase = true) == true
    }

    val thumbnails by viewModel.thumbnails.collectAsState()

    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var startTime by remember { mutableFloatStateOf(0f) }
    var endTime by remember { mutableFloatStateOf(90f) }

    val maxVideoDurationSec = (videoDurationMs / 1000f).coerceAtLeast(1f)

    var mediaIntrinsicSize by remember { mutableStateOf(Size.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    val videoSizeGetModifier = Modifier
        .wrapContentSize()
        .onSizeChanged {
            viewModel.updateContainerSize(it)
        }

    LaunchedEffect(screenSize) {
        viewModel.updateScreenSize(screenSize)
    }

    LaunchedEffect(uri) {
        if (isVideo) {
            val durationMs = getVideoDuration(context, uri)
            videoDurationMs = durationMs

            startTime = 0f
            endTime = 90f.coerceAtMost(durationMs / 1000f)

            viewModel.generateThumbnails(context, uri)
            viewModel.setPostType(type = "video")
        } else {
            viewModel.setPostType(type = "image")
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(
                context,
                uiState.errorMessage,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(uiState.isUploadComplete) {
        if (uiState.isUploadComplete) {
            Toast.makeText(
                context,
                "Upload complete",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .onSizeChanged {
                containerSize = Size(it.width.toFloat(), it.height.toFloat())
                // Optionally track if the screen size changes
                viewModel.updateScreenSize(it)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(top = 50.dp)
        ) {
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        thumbnails.forEach { bitmap ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn()
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    RangeSlider(
                        value = startTime..endTime,
                        modifier = Modifier.fillMaxSize(),
                        onValueChange = { range ->
                            val newStart = range.start.coerceIn(0f, maxVideoDurationSec)
                            val newEnd = range.endInclusive.coerceIn(0f, maxVideoDurationSec)

                            if (newEnd >= newStart) {
                                val difference = newEnd - newStart
                                if (difference > 90) {
                                    if (range.start != startTime) {
                                        startTime = newStart
                                        endTime = (newStart + 90).coerceAtMost(maxVideoDurationSec)
                                    } else {
                                        endTime = newEnd
                                        startTime = (newEnd - 90).coerceAtLeast(0f)
                                    }
                                } else {
                                    startTime = newStart
                                    endTime = newEnd
                                }
                            }
                        },
                        valueRange = 0f..1f.coerceAtLeast(maxVideoDurationSec),
                        steps = 0,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            Box {
                if (isVideo) {
                    VideoEditorPlayer(
                        modifier = videoSizeGetModifier,
                        uri = uri,
                        startTimeMs = (startTime * 1000).toLong(),
                        endTimeMs = (endTime * 1000).toLong()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged {
                                viewModel.updateContainerSize(it)
                            },
                        onSuccess = { state ->
                            mediaIntrinsicSize = state.painter.intrinsicSize
                        }
                    )
                }
                DraggableContainer(
                    children = uiState.overlayTexts,
                    stickers = uiState.overlayStickers,
                    overlayInteraction = uiState.overlayInteraction,
                    onChildTransformChanged = { id, offset, rotation, scale ->
                        // 3. Calculate Image Rect
                        val contentRect = calculateContentRect(containerSize, mediaIntrinsicSize)
                        if (contentRect != Rect.Zero) {
                            // Calculate offset relative to the image top-left
                            // Calculate how far the child is into the image as a PERCENTAGE (0.0 to 1.0)
                            val relativeRatioX = (offset.x - contentRect.left) / contentRect.width
                            val relativeRatioY = (offset.y - contentRect.top) / contentRect.height

                            viewModel.transformOverlayText(
                                id = id,
                                offset = offset,
                                positionX = relativeRatioX,
                                positionY = relativeRatioY,
                                rotation = rotation,
                                scale = scale
                            )
                        }
                    },
                    onStickerTransformChanged = { id, offset, rotation, scale, parentSize, currentStickerSize ->
                        val contentRect = calculateContentRect(containerSize, mediaIntrinsicSize)

                        if (contentRect != Rect.Zero && contentRect.width > 0f) {
                            val relativeRatioX = (offset.x - contentRect.left) / contentRect.width
                            val relativeRatioY = (offset.y - contentRect.top) / contentRect.height

                            val normalizedWidth = (currentStickerSize.width * scale) / contentRect.width

                            viewModel.transformOverlaySticker(
                                id = id,
                                offset = offset,
                                renderOffset = Offset(x = relativeRatioX, y = relativeRatioY),
                                rotation = rotation,
                                scale = scale,
                                parentSize = parentSize,
                                normalizedWidth = normalizedWidth
                            )
                        }

//                    viewModel.transformOverlaySticker(
//                        id = id,
//                        offset = offset,
//                        rotation = rotation,
//                        scale = scale,
//                        parentSize = parentSize,
//                        currentStickerBaseSize = currentStickerSize
//                    )
                    },
                    onChildClick = {
                        viewModel.selectOverlay(it)
                        editing.value = true
                    },
                    modifier = Modifier
                        .matchParentSize()
                )
            }

        }
        TextButton(
            onClick = {
                viewModel.addText()
                editing.value = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = -(18).dp,
                    y = 18.dp
                )
        ) {
            Text(
                text = "Add Text",
                style = Typography.bodyMedium,
                color = Color.White
            )
        }
        TextButton(
            onClick = {
                addSticker.value = true
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    y = 18.dp
                )
        ) {
            Text(
                text = "Add Sticker",
                style = Typography.bodyMedium,
                color = Color.White
            )
        }
        TextField(
            value = postState.description,
            onValueChange = { value ->
                viewModel.updateDescription(value = value)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(
                    y = (-38).dp
                )
        )
        TextButton(
            onClick = {
                viewModel.uploadPost(
                    uri,
                    context,
                    startMs = (startTime * 1000).toLong(),
                    endMs = (endTime * 1000).toLong()
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(
                    y = (-18).dp
                )
        ) {
            Text(
                text = "Save",
                style = Typography.bodyMedium,
                color = Color.White
            )
        }
        if (addSticker.value) {
            ModalBottomSheet(
                sheetState = sheetState,
                sheetMaxWidth = screenHeight,
                containerColor = Black1,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                onDismissRequest = { addSticker.value = false }
            ) {
                val view = LocalView.current
                (view.parent as? DialogWindowProvider)?.window?.let { window ->
                    SideEffect {
                        WindowCompat.getInsetsController(
                            window,
                            view
                        ).isAppearanceLightNavigationBars = false
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight)
                ) {
                    OverlayStickers(
                        onStickerClicked = { sticker ->
                            viewModel.addSticker(sticker)
                            addSticker.value = false
                        }
                    )
                }
            }
        }
        if (editing.value) {
            TextEditor(
                overlayText = uiState.selectedTextOverlay,
                onTextChange = { id, text ->
                    viewModel.updateText(id, text)
                },
                onColorChange = { id, newColor ->
                    viewModel.changeColor(id = id, color = newColor)
                },
                onDoneEditing = {
                    if (uiState.selectedTextOverlay.text.isEmpty()) {
                        viewModel.deleteOverlay(uiState.selectedTextOverlay.id)
                    }
                    viewModel.doneEditing()
                    editing.value = false
                }
            )
        }
        if (uiState.isUploading) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = uiState.progress,
                    animationSpec = tween(durationMillis = 100),
                    label = "File upload progress bar"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(16.dp)
                )
                Text(
                    text = "${(uiState.progress * 100).roundToInt()}"
                )
                Button(
                    onClick = { viewModel.cancelUpload() }
                ) {
                    Text(
                        text = "Cancel upload"
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoEditorPlayer(
    modifier: Modifier,
    uri: Uri,
    startTimeMs: Long,
    endTimeMs: Long
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(uri, startTimeMs, endTimeMs) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startTimeMs)
                    .setEndPositionMs(endTimeMs)
                    .build()
            )
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {

        onDispose {
            exoPlayer.release()
        }
    }

    Box {
        ContentFrame(
            player = exoPlayer
        )
//        Box(
//            modifier = Modifier
//                .matchParentSize()
//                .pointerInput(Unit) {
//                    awaitPointerEventScope {
//                        while (true) {
//                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
//                            event.changes.forEach { it.consume() }
//                        }
//                    }
//                }
//        )
    }

//    AndroidView(
//        factory = { context ->
//            VideoView(context).apply {
//                setVideoURI(uri)
//                setOnPreparedListener { mediaPlayer ->
//                    mediaPlayer.isLooping = true
//                    start()
//                }
//            }
//        },
//        modifier = modifier
//    )
}

@Composable
fun TextEditor(
    modifier: Modifier = Modifier,
    overlayText: TextOverlay,
    onTextChange: (String, String) -> Unit,
    onColorChange: (String, Color) -> Unit,
    onDoneEditing: () -> Unit
) {

    // Debug: Check if overlayText is updating
    LaunchedEffect(overlayText.text) {
        Log.d("TextEditor", "Text changed to: ${overlayText.text}")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        OverlayTextField(
            text = overlayText.text,
            onTextChange = {
                onTextChange(overlayText.id, it)
            },
            textFieldStyle = overlayText.config.fieldStyle,
            fontSize = overlayText.config.textStyle.fontSize,
            fontFamily = overlayText.config.textStyle.fontFamily ?: FontFamily.Default,
            textAlign = overlayText.config.textStyle.textAlign,
            textColor = overlayText.config.textStyle.color,
            containerColor = overlayText.config.containerColor
        )
        TextButton(
            onClick = onDoneEditing,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = -(18).dp,
                    y = (-18).dp
                )
        ) {
            Text(
                text = "Done",
                style = Typography.bodyMedium
            )
        }
        TextColorPicker(
            currentColor = overlayText.config.containerColor,
            onColorClicked = { newColor ->
                onColorChange(overlayText.id, newColor)
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = 18.dp,
                    y = 18.dp
                )
        )
    }
}

fun calculateContentRect(containerSize: Size, mediaSize: Size): Rect {
    if (containerSize == Size.Zero || mediaSize == Size.Zero) return Rect.Zero

    val scale = min(
        containerSize.width / mediaSize.width,
        containerSize.height / mediaSize.height
    )

    val displayedWidth = mediaSize.width * scale
    val displayedHeight = mediaSize.height * scale

    val offsetX = (containerSize.width - displayedWidth) / 2
    val offsetY = (containerSize.height - displayedHeight) / 2

    return Rect(
        offset = Offset(offsetX, offsetY),
        size = Size(displayedWidth, displayedHeight)
    )
}

sealed class StickerSource {
    data class Resource(val resId: Int) : StickerSource()
    data class Uri(val uri: android.net.Uri) : StickerSource()
    data class Url(val url: String) : StickerSource()
    data class Asset(val path: String) : StickerSource()
}

data class OverlaySticker(
    val id: String,
    val source: StickerSource,
    val offsetRatioX: Float = 0f,
    val offsetRatioY: Float = 0f,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val normalizedWidth: Float = 0.2f
)

data class TextOverlay(
    val id: String,
    val text: String,
    val fontSize: TextUnit = 36.sp,
    val offsetRatioX: Float = 0f,
    val offsetRatioY: Float = 0f,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val config: OverlayTextConfig
)