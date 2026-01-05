package com.orb.hubble.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.orb.hubble.data.OverlayInteraction
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.TextOverlay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DraggableContainer(
    children: List<TextOverlay>,
    stickers: List<OverlaySticker>,
    onChildTransformChanged: (id: String, offset: Offset, rotation: Float, scale: Float) -> Unit,
    onStickerTransformChanged: (id: String, offset: Offset, rotation: Float, scale: Float, parentSize: Size, currentStickerSize: Size) -> Unit,
    overlayInteraction: OverlayInteraction,
    onChildClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val childSizes = remember { mutableStateMapOf<String, Size>() }
    val stickerSizes = remember { mutableStateMapOf<String, Size>() }

    val currentChildren by rememberUpdatedState(children)
    val currentStickers by rememberUpdatedState(stickers)
    val currentOnChildTransformChanged by rememberUpdatedState(onChildTransformChanged)
    val currentOnStickerTransformChanged by rememberUpdatedState(onStickerTransformChanged)

    BoxWithConstraints(modifier) {
        val parentWidth = constraints.maxWidth.toFloat()
        val parentHeight = constraints.maxHeight.toFloat()
        val parentSize = Size(parentWidth, parentHeight)

        when (overlayInteraction) {
            is OverlayInteraction.Selected -> {}
            else -> {
                children.forEach { child ->
                    if (child.text.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = child.offsetRatioX * parentWidth
                                    translationY = child.offsetRatioY * parentHeight
                                    rotationZ = child.rotation
                                    scaleX = child.scale
                                    scaleY = child.scale
                                }
                                .onSizeChanged {
                                    childSizes[child.id] = Size(it.width.toFloat(), it.height.toFloat())
                                }
                        ) {
                            OverlayText(
                                text = child.text,
                                onClick = { /* Handled by Overlay */ },
                                maxWidth = with(density) { (parentWidth / child.scale).toDp() },
                                maxHeight = with(density) { (parentHeight / child.scale).toDp() },
                                textStyle = child.config.textStyle,
                                containerColor = child.config.containerColor
                            )
                        }
                    }
                }
                stickers.forEach { sticker ->
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = sticker.offsetRatioX * parentWidth
                                translationY = sticker.offsetRatioY * parentHeight
                                rotationZ = sticker.rotation
                                scaleX = sticker.scale
                                scaleY = sticker.scale
                            }
                            .onSizeChanged {
                                stickerSizes[sticker.id] = Size(it.width.toFloat(), it.height.toFloat())
                            }
                    ) {
                        OverlaySticker(
                            sticker = sticker
                        )
                    }
                }
            }
        }
        // 1. RENDER CHILDREN

        // 2. GESTURE OVERLAY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .pointerInput(Unit) {
                    // Separate detector for Taps vs Transforms
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // Hit test (Reverse order to hit top-most first)
                            val clickedChild = currentChildren.asReversed().find { child ->
                                val size = childSizes[child.id] ?: Size.Zero
                                val centerX =
                                    child.offsetRatioX * parentWidth + (size.width * child.scale / 2)
                                val centerY =
                                    child.offsetRatioY * parentHeight + (size.height * child.scale / 2)
                                isPointInRotatedBox(
                                    tapOffset,
                                    Offset(centerX, centerY),
                                    Size(size.width * child.scale, size.height * child.scale),
                                    child.rotation
                                )
                            }
                            if (clickedChild != null) onChildClick(clickedChild.id)
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // A. Wait for the first finger to touch the screen
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // B. DETERMINE TARGET (Hit Test Once) - Check both children and stickers
                        // We check "Strict" (visual bounds) first, then "Loose" (near bounds).
                        var activeElement: Element? = null
                        var isStrictHit = false

                        // Get geometry for text elements
                        fun getTextGeometry(child: TextOverlay): Pair<Offset, Size> {
                            val size = childSizes[child.id] ?: Size.Zero
                            val cx = child.offsetRatioX * parentWidth + (size.width / 2)
                            val cy = child.offsetRatioY * parentHeight + (size.height / 2)
                            return Offset(cx, cy) to Size(
                                size.width * child.scale,
                                size.height * child.scale
                            )
                        }

                        // Get geometry for sticker elements
                        fun getStickerGeometry(sticker: OverlaySticker): Pair<Offset, Size> {
                            val size = stickerSizes[sticker.id] ?: Size.Zero
                            val cx = sticker.offsetRatioX * parentWidth + (size.width / 2)
                            val cy = sticker.offsetRatioY * parentHeight + (size.height / 2)
                            return Offset(cx, cy) to Size(
                                size.width * sticker.scale,
                                size.height * sticker.scale
                            )
                        }

                        // Check stickers first (since they're usually on top)
                        val reversedStickers = currentStickers.asReversed()
                        val reversedChildren = currentChildren.asReversed()

                        // 1. Check Strict Hits (Direct touches) - Stickers first
                        activeElement = reversedStickers.find { sticker ->
                            val (center, size) = getStickerGeometry(sticker)
                            isPointInRotatedBox(
                                down.position,
                                center,
                                size,
                                sticker.rotation,
                                padding = 0f
                            )
                        }?.let { Element.Sticker(it) }

                        // If no sticker found, check children
                        if (activeElement == null) {
                            activeElement = reversedChildren.find { child ->
                                val (center, size) = getTextGeometry(child)
                                isPointInRotatedBox(
                                    down.position,
                                    center,
                                    size,
                                    child.rotation,
                                    padding = 0f
                                )
                            }?.let { Element.Text(it) }
                        }

                        if (activeElement != null) {
                            isStrictHit = true
                        } else {
                            // 2. If no strict hit, check Loose Hits (Near touches for Zooming)
                            // Check stickers first
                            activeElement = reversedStickers.find { sticker ->
                                val (center, size) = getStickerGeometry(sticker)
                                isPointInRotatedBox(
                                    down.position,
                                    center,
                                    size,
                                    sticker.rotation,
                                    padding = 200f
                                )
                            }?.let { Element.Sticker(it) }

                            if (activeElement == null) {
                                activeElement = reversedChildren.find { child ->
                                    val (center, size) = getTextGeometry(child)
                                    isPointInRotatedBox(
                                        down.position,
                                        center,
                                        size,
                                        child.rotation,
                                        padding = 200f
                                    )
                                }?.let { Element.Text(it) }
                            }
                        }

                        // C. IF WE FOUND A TARGET, LOCK ONTO IT
                        if (activeElement != null) {
                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.any { it.isConsumed }
                                if (canceled) break

                                val zoomChange = event.calculateZoom()
                                val rotationChange = event.calculateRotation()
                                val panChange = event.calculatePan()

                                // Apply changes based on element type
                                when (activeElement) {
                                    is Element.Text -> {
                                        val latestChild = currentChildren.find { it.id == activeElement.data.id }
                                        if (latestChild != null) {
                                            val newRotation = latestChild.rotation + rotationChange
                                            val newScale = (latestChild.scale * zoomChange).coerceIn(0.5f, 4f)

                                            // Pan Logic: Only allow Pan if we had a Strict Hit
                                            val finalPanX = if (isStrictHit) panChange.x else 0f
                                            val finalPanY = if (isStrictHit) panChange.y else 0f

                                            // Calculate bounds
                                            val minX = -parentWidth * 0.5f
                                            val maxX = parentWidth * 1.5f
                                            val minY = -parentHeight * 0.5f
                                            val maxY = parentHeight * 1.5f

                                            val newOffset = Offset(
                                                x = (latestChild.offsetRatioX * parentWidth + finalPanX).coerceIn(minX, maxX),
                                                y = (latestChild.offsetRatioY * parentHeight + finalPanY).coerceIn(minY, maxY)
                                            )

                                            if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                                                currentOnChildTransformChanged(
                                                    latestChild.id,
                                                    newOffset,
                                                    newRotation,
                                                    newScale
                                                )
                                            }
                                        }
                                    }

                                    is Element.Sticker -> {
                                        val latestSticker = currentStickers.find { it.id == activeElement.data.id }
                                        val currentStickerBaseSize = stickerSizes[activeElement.data.id] ?: Size.Zero
                                        if (latestSticker != null) {
                                            val newRotation = latestSticker.rotation + rotationChange
                                            val newScale = (latestSticker.scale * zoomChange).coerceIn(0.5f, 4f)

                                            // Pan Logic: Only allow Pan if we had a Strict Hit
                                            val finalPanX = if (isStrictHit) panChange.x else 0f
                                            val finalPanY = if (isStrictHit) panChange.y else 0f

                                            // Calculate bounds
                                            val minX = -parentWidth * 0.5f
                                            val maxX = parentWidth * 1.5f
                                            val minY = -parentHeight * 0.5f
                                            val maxY = parentHeight * 1.5f

                                            val newOffset = Offset(
                                                x = (latestSticker.offsetRatioX * parentWidth + finalPanX).coerceIn(minX, maxX),
                                                y = (latestSticker.offsetRatioY * parentHeight + finalPanY).coerceIn(minY, maxY)
                                            )

                                            if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                                                currentOnStickerTransformChanged(
                                                    latestSticker.id,
                                                    newOffset,
                                                    newRotation,
                                                    newScale,
                                                    parentSize,
                                                    currentStickerBaseSize
                                                )
                                            }
                                        }
                                    }
                                }

                                // Consume the gesture events so they don't propagate
                                event.changes.forEach {
                                    if (it.positionChanged()) it.consume()
                                }

                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
        )
    }
}

// Re-including the helper function for context
fun isPointInRotatedBox(
    point: Offset,
    boxCenter: Offset,
    boxSize: Size,
    rotationDegrees: Float,
    padding: Float = 0f
): Boolean {
    val relativePoint = point - boxCenter
    val angleRad = -rotationDegrees * (Math.PI / 180f).toFloat()
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)

    val rotatedX = relativePoint.x * cosA - relativePoint.y * sinA
    val rotatedY = relativePoint.x * sinA + relativePoint.y * cosA

    val halfWidth = (boxSize.width / 2) + padding
    val halfHeight = (boxSize.height / 2) + padding

    return rotatedX >= -halfWidth && rotatedX <= halfWidth &&
            rotatedY >= -halfHeight && rotatedY <= halfHeight
}

sealed class Element {
    data class Text(val data: TextOverlay) : Element()
    data class Sticker(val data: OverlaySticker) : Element()
}