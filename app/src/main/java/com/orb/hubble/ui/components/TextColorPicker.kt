package com.orb.hubble.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.StickerSource
import com.orb.hubble.ui.screens.TextOverlay
import com.orb.hubble.ui.theme.Grey1
import com.orb.hubble.ui.utils.ScaleFactors
import com.orb.hubble.ui.utils.ScaledOverlaySticker
import com.orb.hubble.ui.utils.ScaledOverlayText
import com.orb.hubble.ui.utils.toPx
import kotlin.math.roundToInt

@Composable
fun TextColorPicker(
    modifier: Modifier = Modifier,
    currentColor: Color,
    onColorClicked: (Color) -> Unit
) {
    var showColors by remember { mutableStateOf(false) }
    val colors =  listOf(
        Color(0xFFFFFFFF),
        Color(0xFFFF0000), // Red
        Color(0xFFFF8000), // Orange
        Color(0xFFFFFF00), // Yellow
        Color(0xFF80FF00), // Lime
        Color(0xFF00FF00), // Green
        Color(0xFF00FF80), // Spring Green
        Color(0xFF00FFFF), // Cyan
        Color(0xFF0080FF), // Azure
        Color(0xFF0000FF), // Blue
        Color(0xFF8000FF), // Purple
        Color(0xFFFF00FF), // Magenta
        Color(0xFFFF0080) // Rose
    )

    Surface(
        modifier = modifier
            .size(25.dp)
            .clickable(onClick = {
                showColors = true
            }),
        color = currentColor,
        shape = CircleShape,
    ) {
        DropdownMenu(
            expanded = showColors,
            onDismissRequest = { showColors = false },
            containerColor = Color.Transparent
        ) {
            ColorRow(
                colorList = colors,
                onColorClicked = onColorClicked
            )
        }
    }
}

@Composable
fun ColorRow(
    modifier: Modifier = Modifier,
    colorList: List<Color>,
    onColorClicked: (Color) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Grey1
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            colorList.forEach { color ->
                Surface(
                    modifier = modifier
                        .size(25.dp)
                        .clickable(onClick = {
                            onColorClicked(color)
                        }),
                    color = color,
                    shape = CircleShape,
                    content = @Composable{}
                )
            }
        }
    }
}

//@Composable
//fun DraggableContainer(
//    children: List<TextOverlay>,
//    onChildTransformChanged: (id: String, offset: Offset, rotation: Float, scale: Float) -> Unit,
//    onChildClick: (String) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val density = LocalDensity.current
//    BoxWithConstraints(modifier) {
//        val parentWidth = constraints.maxWidth
//        val parentHeight = constraints.maxHeight
//
//        children.forEach { child ->
//            if (child.text.isNotEmpty()) {
//                var childWidth by remember(child.id) {
//                    mutableIntStateOf(0)
//                }
//                var childHeight by remember(child.id) {
//                    mutableIntStateOf(0)
//                }
//
//                val transformableState =
//                    rememberTransformableState { scaleChange, panChange, rotationChange ->
//                        val newRotation = child.rotation + rotationChange
//
//                        val angle = newRotation * PI.toFloat() / 180f
//                        val cos = cos(angle)
//                        val sin = sin(angle)
//
//                        val rotatedPanX = panChange.x * cos - panChange.y * sin
//                        val rotatedPanY = panChange.x * sin + panChange.y * cos
//
//                        val newScale = (child.scale * scaleChange).coerceIn(0.5f, 4f)
//
//                        val scaledWidth = childWidth * child.scale
//                        val scaledHeight = childHeight * child.scale
//
//                        val visualWidth = abs(scaledWidth * cos) + abs(scaledHeight * sin)
//                        val visualHeight = abs(scaledWidth * sin) + abs(scaledHeight * cos)
//
//                        val scaleOffsetX = (scaledWidth - childWidth) / 2
//                        val scaleOffsetY = (scaledHeight - childHeight) / 2
//
//                        val rotationOffsetX = (visualWidth - scaledWidth) / 2
//                        val rotationOffsetY = (visualHeight - scaledHeight) / 2
//
//                        val minX = scaleOffsetX + rotationOffsetX
//                        val maxX = parentWidth - childWidth - scaleOffsetX - rotationOffsetX
//                        val minY = scaleOffsetY + rotationOffsetY
//                        val maxY = parentHeight - childHeight - scaleOffsetY - rotationOffsetY
//
//                        val newOffset = Offset(
//                            x = (child.offsetRatioX * parentWidth + child.scale * rotatedPanX).coerceIn(
//                                minimumValue = minOf(minX, maxX),
//                                maximumValue = maxOf(minX, maxX)
//                            ),
//                            y = (child.offsetRatioY * parentHeight + child.scale * rotatedPanY).coerceIn(
//                                minimumValue = minOf(minY, maxY),
//                                maximumValue = maxOf(minY, maxY)
//                            ),
//                        )
//
//                        onChildTransformChanged(
//                            child.id,
//                            newOffset,
//                            newRotation,
//                            newScale
//                        )
//                    }
//
//                Box(
//                    modifier = Modifier
//                        .onSizeChanged {
//                            childWidth = it.width
//                            childHeight = it.height
//                        }
//                        .graphicsLayer {
//                            translationX = child.offsetRatioX * parentWidth
//                            translationY = child.offsetRatioY * parentHeight
//                            rotationZ = child.rotation
//                            scaleX = child.scale
//                            scaleY = child.scale
//                        }
//                        .transformable(transformableState)
//                ) {
//                    OverlayText(
//                        text = child.text,
//                        onClick = {
//                            onChildClick(child.id)
//                        },
//                        maxWidth = with(density) {
//                            (parentWidth / child.scale).toDp()
//                        },
//                        maxHeight = with(density) {
//                            (parentHeight / child.scale).toDp()
//                        },
//                        textStyle = child.config.textStyle,
//                        containerColor = child.config.containerColor
//                    )
//                }
//            }
//        }
//    }
//}



//package com.orb.hubble.ui.screens
//
//import android.net.Uri
//import android.util.Log
//import android.widget.VideoView
//import androidx.annotation.DrawableRes
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.offset
//import androidx.compose.material3.BottomSheetDefaults
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.ModalBottomSheet
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.rememberModalBottomSheetState
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.SideEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Rect
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalSoftwareKeyboardController
//import androidx.compose.ui.platform.LocalView
//import androidx.compose.ui.platform.LocalWindowInfo
//import androidx.compose.ui.text.font.FontFamily
//import androidx.compose.ui.unit.TextUnit
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.compose.ui.window.DialogWindowProvider
//import coil3.compose.AsyncImage
//import coil3.request.ImageRequest
//import coil3.request.crossfade
//import androidx.core.net.toUri
//import androidx.core.view.WindowCompat
//import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
//import com.orb.hubble.data.CreatePostScreenViewModel
//import com.orb.hubble.ui.components.DraggableContainer
//import com.orb.hubble.ui.components.MessagesSection
//import com.orb.hubble.ui.components.OverlayStickers
//import com.orb.hubble.ui.components.OverlayTextConfig
//import com.orb.hubble.ui.components.OverlayTextField
//import com.orb.hubble.ui.components.TextColorPicker
//import com.orb.hubble.ui.theme.Black1
//import com.orb.hubble.ui.theme.Black2
//import com.orb.hubble.ui.theme.Typography
//import kotlin.math.min
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CreatePostScreen(
//    modifier: Modifier = Modifier,
//    viewModel: CreatePostScreenViewModel = hiltViewModel(),
//    mediaUri: String
//) {
//    val uiState by viewModel.state.collectAsState()
//    val editing = remember { mutableStateOf(false) }
//    val addSticker = remember { mutableStateOf(false) }
//    val screenHeight = LocalWindowInfo.current.containerSize.height.dp
//    val sheetState = rememberModalBottomSheetState(
//        skipPartiallyExpanded = true
//    )
//
//    val context = LocalContext.current
//    val uri = remember { mediaUri.toUri() }
//    val isVideo = remember(uri) {
//        uri.path?.contains("video/", true) ?: false
//    }
//
//    var mediaIntrinsicSize by remember { mutableStateOf(Size.Zero) }
//    var containerSize by remember { mutableStateOf(Size.Zero) }
//
//    Box(
//        modifier = modifier
//            .fillMaxSize()
//            .background(Color.Black)
//            .onSizeChanged {
//                // Capture the container (screen) size
//                containerSize = Size(it.width.toFloat(), it.height.toFloat())
//                viewModel.updateContainerSize(it)
//            },
//        contentAlignment = Alignment.Center
//    ) {
//        Box {
//            if (isVideo) {
//                VideoPlayer(
//                    modifier = Modifier.fillMaxSize(),
//                    uri = uri,
//                    onVideoSizeChanged = { w, h ->
//                        mediaIntrinsicSize = Size(w.toFloat(), h.toFloat())
//                    }
//                )
//            } else {
//                AsyncImage(
//                    model = ImageRequest.Builder(context)
//                        .data(mediaUri)
//                        .crossfade(true)
//                        .build(),
//                    contentDescription = "Selected image",
//                    modifier = Modifier.fillMaxSize(),
//                    onSuccess = { state ->
//                        mediaIntrinsicSize = state.painter.intrinsicSize
//                    }
//                )
//            }
//            DraggableContainer(
//                children = uiState.overlayTexts,
//                stickers = uiState.overlayStickers,
//                overlayInteraction = uiState.overlayInteraction,
//                onChildTransformChanged = { id, offset, rotation, scale ->
//                    // 3. Calculate Image Rect
//                    val contentRect = calculateContentRect(containerSize, mediaIntrinsicSize)
//                    if (contentRect != Rect.Zero) {
//                        // Calculate offset relative to the image top-left
//                        val relativeX = offset.x - contentRect.left
//                        val relativeY = offset.y - contentRect.top
//
//                        // Normalize to Ratio (0.0 - 1.0 is inside image, <0 or >1 is outside)
//                        val ratioX = relativeX / contentRect.width
//                        val ratioY = relativeY / contentRect.height
//
//                        viewModel.transformOverlayText(
//                            id = id,
//                            ratioX = ratioX, // Change VM signature to accept ratios directly if easier
//                            ratioY = ratioY,
//                            rotation = rotation,
//                            scale = scale
//                        )
//                    }
//                },
//                onStickerTransformChanged = { id, offset, rotation, scale, parentSize, currentStickerSize ->
//                    val contentRect = calculateContentRect(containerSize, mediaIntrinsicSize)
//
//                    if (contentRect != Rect.Zero) {
//                        val relativeX = offset.x - contentRect.left
//                        val relativeY = offset.y - contentRect.top
//
//                        viewModel.transformOverlaySticker(
//                            id = id,
//                            offset = Offset(relativeX, relativeY),
//                            rotation = rotation,
//                            scale = scale,
//                            parentSize = contentRect.size, // Pass the IMAGE size, not screen size
//                            currentStickerBaseSize = currentStickerSize
//                        )
//                    }
//                },
//                onChildClick = {
//                    viewModel.selectOverlay(it)
//                    editing.value = true
//                },
//                modifier = Modifier
//                    .matchParentSize()
//            )
//        }
//        TextButton(
//            onClick = {
//                viewModel.addText()
//                editing.value = true
//            },
//            modifier = Modifier
//                .align(Alignment.TopEnd)
//                .offset(
//                    x = -(18).dp,
//                    y = 18.dp
//                )
//        ) {
//            Text(
//                text = "Add Text",
//                style = Typography.bodyMedium,
//                color = Color.White
//            )
//        }
//        TextButton(
//            onClick = {
//                addSticker.value = true
//            },
//            modifier = Modifier
//                .align(Alignment.TopCenter)
//                .offset(
//                    y = 18.dp
//                )
//        ) {
//            Text(
//                text = "Add Sticker",
//                style = Typography.bodyMedium,
//                color = Color.White
//            )
//        }
//        TextButton(
//            onClick = {
//                viewModel.saveMedia(uri, context)
//            },
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .offset(
//                    y = (-18).dp
//                )
//        ) {
//            Text(
//                text = "Save",
//                style = Typography.bodyMedium,
//                color = Color.White
//            )
//        }
//        if (addSticker.value) {
//            ModalBottomSheet(
//                sheetState = sheetState,
//                sheetMaxWidth = screenHeight,
//                containerColor = Black1,
//                dragHandle = { BottomSheetDefaults.DragHandle() },
//                onDismissRequest = { addSticker.value = false }
//            ) {
//                val view = LocalView.current
//                (view.parent as? DialogWindowProvider)?.window?.let { window ->
//                    SideEffect {
//                        WindowCompat.getInsetsController(
//                            window,
//                            view
//                        ).isAppearanceLightNavigationBars = false
//                    }
//                }
//
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(screenHeight)
//                ) {
//                    OverlayStickers(
//                        onStickerClicked = { sticker ->
//                            viewModel.addSticker(sticker)
//                            addSticker.value = false
//                        }
//                    )
//                }
//            }
//        }
//        if (editing.value) {
//            TextEditor(
//                overlayText = uiState.selectedTextOverlay,
//                onTextChange = { id, text ->
//                    viewModel.updateText(id, text)
//                },
//                onColorChange = { id, newColor ->
//                    viewModel.changeColor(id = id, color = newColor)
//                },
//                onDoneEditing = {
//                    if (uiState.selectedTextOverlay.text.isEmpty()) {
//                        viewModel.deleteOverlay(uiState.selectedTextOverlay.id)
//                    }
//                    viewModel.doneEditing()
//                    editing.value = false
//                }
//            )
//        }
//    }
//}
//
//@Composable
//fun VideoPlayer(
//    modifier: Modifier,
//    uri: Uri,
//    onVideoSizeChanged: (Int, Int) -> Unit
//) {
//    val context = LocalContext.current
//
//    AndroidView(
//        factory = { context ->
//            VideoView(context).apply {
//                setVideoURI(uri)
//                setOnPreparedListener { mediaPlayer ->
//                    mediaPlayer.isLooping = true
//                    onVideoSizeChanged(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
//                    start()
//                }
//            }
//        },
//        modifier = modifier
//    )
//}
//
//@Composable
//fun TextEditor(
//    modifier: Modifier = Modifier,
//    overlayText: TextOverlay,
//    onTextChange: (String, String) -> Unit,
//    onColorChange: (String, Color) -> Unit,
//    onDoneEditing: () -> Unit
//) {
//
//    // Debug: Check if overlayText is updating
//    LaunchedEffect(overlayText.text) {
//        Log.d("TextEditor", "Text changed to: ${overlayText.text}")
//    }
//
//    Box(
//        modifier = modifier
//            .fillMaxSize()
//            .background(Color.Black.copy(alpha = 0.2f)),
//        contentAlignment = Alignment.Center
//    ) {
//        OverlayTextField(
//            text = overlayText.text,
//            onTextChange = {
//                onTextChange(overlayText.id, it)
//            },
//            textFieldStyle = overlayText.config.fieldStyle,
//            fontSize = overlayText.config.textStyle.fontSize,
//            fontFamily = overlayText.config.textStyle.fontFamily ?: FontFamily.Default,
//            textAlign = overlayText.config.textStyle.textAlign,
//            textColor = overlayText.config.textStyle.color,
//            containerColor = overlayText.config.containerColor
//        )
//        TextButton(
//            onClick = onDoneEditing,
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .offset(
//                    x = -(18).dp,
//                    y = (-18).dp
//                )
//        ) {
//            Text(
//                text = "Done",
//                style = Typography.bodyMedium
//            )
//        }
//        TextColorPicker(
//            currentColor = overlayText.config.containerColor,
//            onColorClicked = { newColor ->
//                onColorChange(overlayText.id, newColor)
//            },
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .offset(
//                    x = 18.dp,
//                    y = 18.dp
//                )
//        )
//    }
//}
//
//sealed class StickerSource {
//    data class Resource(@DrawableRes val resId: Int) : StickerSource()
//    data class Uri(val uri: android.net.Uri) : StickerSource()
//    data class Url(val url: String) : StickerSource()
//    data class Asset(val path: String) : StickerSource()
//}
//
//data class OverlaySticker(
//    val id: String,
//    val source: StickerSource,
//    val offsetRatioX: Float = 0f,
//    val offsetRatioY: Float = 0f,
//    val rotation: Float = 0f,
//    val scale: Float = 1f,
//    val alpha: Float = 1f,
//    val normalizedWidth: Float = 0.2f
//)
//
//data class TextOverlay(
//    val id: String,
//    val text: String,
//    val fontSize: TextUnit = 36.sp,
//    val offsetRatioX: Float = 0f,
//    val offsetRatioY: Float = 0f,
//    val rotation: Float = 0f,
//    val scale: Float = 1f,
//    val config: OverlayTextConfig
//)
//
///**
// * Calculates the rectangle (Offset and Size) of the image displayed
// * inside the container using ContentScale.Fit logic.
// */
//fun calculateContentRect(containerSize: Size, mediaSize: Size): Rect {
//    if (containerSize == Size.Zero || mediaSize == Size.Zero) return Rect.Zero
//
//    val scale = min(
//        containerSize.width / mediaSize.width,
//        containerSize.height / mediaSize.height
//    )
//
//    val displayedWidth = mediaSize.width * scale
//    val displayedHeight = mediaSize.height * scale
//
//    val offsetX = (containerSize.width - displayedWidth) / 2
//    val offsetY = (containerSize.height - displayedHeight) / 2
//
//    return Rect(
//        offset = Offset(offsetX, offsetY),
//        size = Size(displayedWidth, displayedHeight)
//    )
//}

//class MediaRenderCalculator(
//    private val density: Float
//) {
//    companion object {
//        private const val TEXT_PADDING_DP = 8f
//        private const val ROUNDED_CORNER_DP = 16f
//    }
//
//    fun calculateScaleFactors(
//        bitmapWidth: Int,
//        bitmapHeight: Int,
//        templateSize: IntSize
//    ): ScaleFactors {
//        // Calculate how much we need to scale up from "UI Pixels" to "Bitmap Pixels"
//        val scaleX = if (templateSize.width > 0) bitmapWidth.toFloat() / templateSize.width else 1f
//        val scaleY = if (templateSize.height > 0) bitmapHeight.toFloat() / templateSize.height else 1f
//        val bitmapScale = (scaleX + scaleY) / 2
//
//        return ScaleFactors(scaleX, scaleY, bitmapScale)
//    }
//
//    /**
//     * Helper to map a point from Screen Coordinates (0..screenSize)
//     * to Bitmap Coordinates (0..bitmapSize), assuming the image is Centered on screen.
//     */
//    private fun mapScreenCoordToBitmapCoord(
//        ratio: Float,
//        screenDimension: Int,
//        templateDimension: Int,
//        scaleFactor: Float
//    ): Float {
//        // 1. Find the position in Screen Pixels
//        val screenPx = ratio * screenDimension
//
//        // 2. Calculate the empty space (black bar) on one side
//        val offsetPx = (screenDimension - templateDimension) / 2f
//
//        // 3. Subtract the offset to get coordinates relative to the Image's Top-Left
//        val relativePx = screenPx - offsetPx
//
//        // 4. Scale up to the actual Bitmap resolution
//        return relativePx * scaleFactor
//    }
//
//    fun calculateScaledTextOverlay(
//        overlayText: TextOverlay,
//        scaleFactors: ScaleFactors,
//        templateSize: IntSize,
//        screenSize: IntSize // Added parameter
//    ): ScaledOverlayText {
//        val (scaleX, scaleY, bitmapScale) = scaleFactors
//
//        // Translate Screen Ratio -> Bitmap Coordinate
//        val finalX = mapScreenCoordToBitmapCoord(
//            ratio = overlayText.offsetRatioX,
//            screenDimension = screenSize.width,
//            templateDimension = templateSize.width,
//            scaleFactor = scaleX
//        )
//
//        val finalY = mapScreenCoordToBitmapCoord(
//            ratio = overlayText.offsetRatioY,
//            screenDimension = screenSize.height,
//            templateDimension = templateSize.height,
//            scaleFactor = scaleY
//        )
//
//        val scaledOffset = Offset(x = finalX, y = finalY)
//
//        val textPaddingPx = TEXT_PADDING_DP * density
//        val scaledFontSize = overlayText.fontSize * bitmapScale
//
//        // Calculate constraint width relative to SCREEN, then scale to Bitmap
//        val paddingDp = TEXT_PADDING_DP * 2
//        val paddingPx = paddingDp * density
//
//        // Calculate how wide the text box can be on the bitmap
//        // We use screenSize.width for ratio calculation if the "container" is the screen
//        val screenSpaceWidth = (screenSize.width / overlayText.scale) - paddingPx
//        val constraintWidth = (screenSpaceWidth * scaleX).roundToInt().coerceAtLeast(1)
//
//        return ScaledOverlayText(
//            text = overlayText.text,
//            scaledOffset = scaledOffset,
//            scaledFontSizePx = scaledFontSize.toPx(),
//            roundedCorner = 13f,
//            containerColor = overlayText.config.containerColor.toArgb(),
//            fieldStyle = overlayText.config.fieldStyle,
//            constraintWidth = constraintWidth,
//            textPaddingX = textPaddingPx * scaleX,
//            textPaddingY = textPaddingPx * scaleY,
//            rotation = overlayText.rotation,
//            scale = overlayText.scale,
//            textColor = overlayText.config.textStyle.color.toArgb(),
//            cornerRadius = ROUNDED_CORNER_DP * density * bitmapScale,
//            bitmapScale = bitmapScale,
//            originalTextOverlay = overlayText
//        )
//    }
//
//    fun calculateScaledStickerOverlay(
//        overlaySticker: OverlaySticker,
//        scaleFactors: ScaleFactors,
//        templateSize: IntSize,
//        screenSize: IntSize // Added parameter
//    ): ScaledOverlaySticker {
//        val (scaleX, scaleY, _) = scaleFactors
//
//        // 1. Calculate Target Width
//        // The normalizedWidth is relative to the SCREEN width now.
//        // We convert that screen-relative width to UI Pixels, then to Bitmap Pixels.
//        val targetStickerWidthOnScreen = screenSize.width * overlaySticker.normalizedWidth
//        val targetStickerWidthBitmap = targetStickerWidthOnScreen * scaleX
//
//        // 2. Calculate Coordinates
//        val finalX = mapScreenCoordToBitmapCoord(
//            ratio = overlaySticker.offsetRatioX,
//            screenDimension = screenSize.width,
//            templateDimension = templateSize.width,
//            scaleFactor = scaleX
//        )
//
//        val finalY = mapScreenCoordToBitmapCoord(
//            ratio = overlaySticker.offsetRatioY,
//            screenDimension = screenSize.height,
//            templateDimension = templateSize.height,
//            scaleFactor = scaleY
//        )
//
//        val scaledOffset = Offset(x = finalX, y = finalY)
//
//        return when (val source = overlaySticker.source) {
//            is StickerSource.Resource -> ScaledOverlaySticker(
//                resId = source.resId,
//                scaledOffset = scaledOffset,
//                constraintWidth = targetStickerWidthBitmap.toInt(),
//                rotation = overlaySticker.rotation,
//                scale = overlaySticker.scale,
//                originalOverlaySticker = overlaySticker
//            )
//            is StickerSource.Uri -> ScaledOverlaySticker(
//                // Assuming you have a URI handling property in ScaledOverlaySticker
//                resId = 0,
//                scaledOffset = scaledOffset,
//                constraintWidth = targetStickerWidthBitmap.toInt(),
//                rotation = overlaySticker.rotation,
//                scale = overlaySticker.scale,
//                originalOverlaySticker = overlaySticker
//            )
//            else -> throw NotImplementedError()
//        }
//    }
//}