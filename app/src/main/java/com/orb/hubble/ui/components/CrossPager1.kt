package com.orb.hubble.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun <T> CrossPager1(
    modifier: Modifier = Modifier,
    items: List<T>,
    animationSpec: AnimationSpec<Float> = spring(stiffness = 400f),
    swipeThreshold: Float = 0.3f,
    nextPageContentContainer: @Composable (T) -> Unit,
    contentContainer: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    //Represents drag progress: -1f (moving to next) to 1f (moving to prev)
    val dragProgress = remember { Animatable(0f) }
    var swipeAxis by remember { mutableStateOf(SwipeAxis.None) }
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items.size, currentIndex) {
                    val velocityTracker = VelocityTracker()

                    detectDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            swipeAxis = SwipeAxis.None
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity()
                            //Determine if we should snap based on velocity or drag distance and drag direction
                            val target = when {
                                dragProgress.value < -swipeThreshold || (swipeAxis == SwipeAxis.Horizontal && velocity.x < -300) || (swipeAxis == SwipeAxis.Vertical && velocity.y < -300) -> -1f
                                dragProgress.value > swipeThreshold || (swipeAxis == SwipeAxis.Horizontal && velocity.x > 300) || (swipeAxis == SwipeAxis.Vertical && velocity.y > 300) -> 1f
                                else -> 0f
                            }

                            coroutineScope.launch {
                                //Prevent scrolling out of bounds
                                val isFirstAndSwipingBack = currentIndex == 0 && target > 0
                                val isLastAndSwipingForward =
                                    currentIndex == items.size - 1 && target < 0

                                val finalTarget =
                                    if (isFirstAndSwipingBack || isLastAndSwipingForward) 0f else target

                                dragProgress.animateTo(finalTarget, animationSpec)

                                //Once animation finishes, update index and reset progress instantly
                                if (finalTarget != 0f) {
                                    currentIndex =
                                        if (finalTarget == -1f) currentIndex + 1 else currentIndex - 1
                                    dragProgress.snapTo(0f)
                                }
                                //Reset the direction after the snap animation is completely finished
                                swipeAxis = SwipeAxis.None
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            //lock in the swipe direction on the first drag event
                            if (swipeAxis == SwipeAxis.None) {
                                swipeAxis = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    SwipeAxis.Horizontal
                                } else SwipeAxis.Vertical
                            }

                            //Only calculate progress based on the locked direction
                            val deltaProgress = if (swipeAxis == SwipeAxis.Horizontal) {
                                dragAmount.x / widthPx
                            } else dragAmount.y / heightPx

                            coroutineScope.launch {
                                //Add to current progress, clamping between -1f and 1f
                                val newProgress =
                                    (dragProgress.value + deltaProgress).coerceIn(-1f, 1f)
                                dragProgress.snapTo(newProgress)
                            }
                        }
                    )
                }
        ) {
            // RENDER PREVIOUS ITEM
            if (currentIndex > 0 && dragProgress.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            //Offset it above/left out of view, and bring it in based on progress
                            IntOffset(
                                x = if (swipeAxis == SwipeAxis.Horizontal) ((dragProgress.value - 1f) * widthPx).roundToInt() else 0,
                                y = if (swipeAxis == SwipeAxis.Vertical) ((dragProgress.value - 1f) * heightPx).roundToInt() else 0
                            )
                        }
                ) {
                    nextPageContentContainer(items[currentIndex - 1])
                }
            }

            // RENDER NEXT ITEM
            if (currentIndex < items.size - 1 && dragProgress.value < 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            // Offset it below/right out of view, and bring it in based on progress
                            IntOffset(
                                x = if (swipeAxis == SwipeAxis.Horizontal) ((1f + dragProgress.value) * widthPx).roundToInt() else 0,
                                y = if (swipeAxis == SwipeAxis.Vertical) ((1f + dragProgress.value) * heightPx).roundToInt() else 0
                            )
                        }
                ) {
                    nextPageContentContainer(items[currentIndex + 1])
                }
            }

            // RENDER CURRENT ITEM
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            x = if (swipeAxis == SwipeAxis.Horizontal) (dragProgress.value * widthPx).roundToInt() else 0,
                            y = if (swipeAxis == SwipeAxis.Vertical) (dragProgress.value * heightPx).roundToInt() else 0
                        )
                    }
            ) {
                contentContainer(items[currentIndex])
            }
        }
    }
}

enum class SwipeAxis { None, Horizontal, Vertical }