package com.orb.hubble.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.orb.hubble.data.CreatePostScreenViewModel
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.ui.components.DraggableContainer
import com.orb.hubble.ui.components.MessagesSection
import com.orb.hubble.ui.components.OverlayStickers
import com.orb.hubble.ui.components.OverlayTextConfig
import com.orb.hubble.ui.components.OverlayTextField
import com.orb.hubble.ui.components.TextColorPicker
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Black2
import com.orb.hubble.ui.theme.Typography
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    mediaUri: String
) {
    val uiState by viewModel.state.collectAsState()
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
        uri.path?.contains("video/", true) ?: false
    }
    val videoSizeGetModifier = Modifier
        .fillMaxSize()
        .onSizeChanged {
            viewModel.updateContainerSize(it)
        }

    LaunchedEffect(screenSize) {
        viewModel.updateScreenSize(screenSize)
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
            .onSizeChanged {
                // Optionally track if the screen size changes
                viewModel.updateScreenSize(it)
            },
        contentAlignment = Alignment.Center
    ) {
        Box {
            if (isVideo) {
                VideoPlayer(
                    modifier = videoSizeGetModifier,
                    uri = uri
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
                        }
                )
            }
            DraggableContainer(
                children = uiState.overlayTexts,
                stickers = uiState.overlayStickers,
                overlayInteraction = uiState.overlayInteraction,
                onChildTransformChanged = { id, offset, rotation, scale ->
                    viewModel.transformOverlayText(
                        id = id,
                        offset = offset,
                        rotation = rotation,
                        scale = scale
                    )
                },
                onStickerTransformChanged = { id, offset, rotation, scale, parentSize, currentStickerSize ->
                    viewModel.transformOverlaySticker(
                        id = id,
                        offset = offset,
                        rotation = rotation,
                        scale = scale,
                        parentSize = parentSize,
                        currentStickerBaseSize = currentStickerSize
                    )
                },
                onChildClick = {
                    viewModel.selectOverlay(it)
                    editing.value = true
                },
                modifier = Modifier
                    .matchParentSize()
            )
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
        TextButton(
            onClick = {
                viewModel.saveMedia(uri, context)
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

@Composable
fun VideoPlayer(
    modifier: Modifier,
    uri: Uri
) {
    val context = LocalContext.current

    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(uri)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    start()
                }
            }
        },
        modifier = modifier
    )
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
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val config: OverlayTextConfig
)