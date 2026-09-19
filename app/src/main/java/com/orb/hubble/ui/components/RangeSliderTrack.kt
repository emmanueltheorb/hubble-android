package com.orb.hubble.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults.colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.orb.hubble.ui.theme.Blue1
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.Purple40
import com.orb.hubble.ui.theme.White1
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyRangeSliderTrack(
    modifier: Modifier = Modifier,
    rangeSliderState: MyRangeSliderState,
    trackCornerSize: Dp = Dp(Float.NaN),
    enabled: Boolean = true,
    colors: SliderColors = colors(),
    drawStopIndicator: (DrawScope.(Offset) -> Unit)? = {
        drawStopIndicator(
            offset = it,
            color = trackColor(enabled, active = true),
            size = TrackStopIndicatorSize,
        )
    },
    drawTick: DrawScope.(Offset, Color) -> Unit = { offset, color ->
        drawStopIndicator(offset = offset, color = color, size = TickSize)
    },
    thumbTrackGapSize: Dp = 0.0.dp,//formerly 6.0.dp
    trackInsideCornerSize: Dp = 2.dp,
) {
    TrackImpl(
        rangeSliderState = rangeSliderState,
        trackCornerSize = trackCornerSize,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        drawStopIndicator = drawStopIndicator,
        drawTick = drawTick,
        thumbTrackGapSize = thumbTrackGapSize,
        trackInsideCornerSize = trackInsideCornerSize,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackImpl(
    rangeSliderState: MyRangeSliderState,
    trackCornerSize: Dp,
    modifier: Modifier,
    enabled: Boolean,
    colors: SliderColors,
    drawStopIndicator: (DrawScope.(Offset) -> Unit)?,
    drawTick: DrawScope.(Offset, Color) -> Unit,
    thumbTrackGapSize: Dp,
    trackInsideCornerSize: Dp,
) {
    val inactiveTrackColor = trackColor(enabled, active = false)
    val activeTrackColor = trackColor(enabled, active = true)
    val inactiveTickColor = tickColor(enabled, active = false)
    val activeTickColor = tickColor(enabled, active = true)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(40.0.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val trackCornerSize = placeable.height / 2
                layout(
                    width = placeable.width,
                    height = placeable.height,
                    alignmentLines = mapOf(CornerSizeAlignmentLine to trackCornerSize),
                ) {
                    placeable.place(0, 0)
                }
            }
    ) {
        val cornerSize: Float =
            if (trackCornerSize == Dp.Unspecified) {
                size.height / 2
            } else {
                trackCornerSize.toPx()
            }
        drawTrack(
            tickFractions = rangeSliderState.tickFractions,
            activeRangeStart = rangeSliderState.coercedActiveRangeStartAsFraction,
            activeRangeEnd = rangeSliderState.coercedActiveRangeEndAsFraction,
            inactiveTrackColor = inactiveTrackColor,
            activeTrackColor = activeTrackColor,
            inactiveTickColor = inactiveTickColor,
            activeTickColor = activeTickColor,
            startThumbWidth = rangeSliderState.startThumbWidth.toDp(),
            startThumbHeight = rangeSliderState.startThumbHeight.toDp(),
            endThumbWidth = rangeSliderState.endThumbWidth.toDp(),
            endThumbHeight = rangeSliderState.endThumbHeight.toDp(),
            thumbTrackGapSize = thumbTrackGapSize,
            trackInsideCornerSize = trackInsideCornerSize,
            trackCornerSize = cornerSize.toDp(),
            drawStopIndicator = drawStopIndicator,
            drawTick = drawTick,
            isRangeSlider = true,
        )
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    startThumbWidth: Dp,
    startThumbHeight: Dp,
    endThumbWidth: Dp,
    endThumbHeight: Dp,
    thumbTrackGapSize: Dp,
    trackInsideCornerSize: Dp,
    trackCornerSize: Dp,
    drawStopIndicator: (DrawScope.(Offset) -> Unit)?,
    drawTick: DrawScope.(Offset, Color) -> Unit,
    isRangeSlider: Boolean,
    enableCornerShrinking: Boolean = false,
    orientation: Orientation = Horizontal,
    isCentered: Boolean = false,
) {
    val isVertical = orientation == Vertical
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val isRtlHorizontal = isRtl && !isVertical
    val cornerSize = trackCornerSize.toPx()
    val sliderStart = 0f
    val sliderEnd = if (isVertical) size.height else size.width

    val isStartOnFirstOrLastStep =
        activeRangeStart == tickFractions.firstOrNull() ||
                activeRangeStart == tickFractions.lastOrNull()
    val isEndOnFirstOrLastStep =
        activeRangeEnd == tickFractions.firstOrNull() ||
                activeRangeEnd == tickFractions.lastOrNull()
    val sliderValueEnd =
        if (tickFractions.isNotEmpty() && !isEndOnFirstOrLastStep) {
            sliderStart +
                    (sliderEnd - sliderStart - cornerSize * 2) * activeRangeEnd +
                    cornerSize
        } else {
            sliderStart + (sliderEnd - sliderStart) * activeRangeEnd
        }
    val sliderValueStart =
        if (tickFractions.isNotEmpty() && !isStartOnFirstOrLastStep) {
            sliderStart +
                    (sliderEnd - sliderStart - cornerSize * 2) * activeRangeStart +
                    cornerSize
        } else {
            sliderStart + (sliderEnd - sliderStart) * activeRangeStart
        }

    val insideCornerSize = trackInsideCornerSize.toPx()
    var startGap = 0f
    var endGap = 0f
    if (thumbTrackGapSize > 0.dp) {
        if (isVertical) {
            startGap = startThumbHeight.toPx() / 2 + thumbTrackGapSize.toPx()
            endGap = endThumbHeight.toPx() / 2 + thumbTrackGapSize.toPx()
        } else {
            startGap = startThumbWidth.toPx() / 2 + thumbTrackGapSize.toPx()
            endGap = endThumbWidth.toPx() / 2 + thumbTrackGapSize.toPx()
        }
    }
    val centerAxis = if (isVertical) center.y else center.x

    // inactive track (centered or range slider)
    var rangeInactiveTrackThreshold = sliderStart + startGap
    if (!enableCornerShrinking || tickFractions.isNotEmpty()) {
        rangeInactiveTrackThreshold += cornerSize
    }
    val adjustedSliderValueEnd =
        if (isCentered) {
            min(sliderValueEnd, centerAxis)
        } else {
            sliderValueStart
        }
    if ((isCentered || isRangeSlider) && adjustedSliderValueEnd > rangeInactiveTrackThreshold) {
        val startCornerRadius = if (isRtlHorizontal) insideCornerSize else cornerSize
        val endCornerRadius = if (isRtlHorizontal) cornerSize else insideCornerSize
        val start = sliderStart
        val end = adjustedSliderValueEnd - startGap
        val trackOffset =
            if (isRtlHorizontal) {
                Offset(size.width - end, 0f)
            } else {
                Offset(0f, 0f)
            }
        val trackSize =
            if (isVertical) {
                Size(size.width, end - start)
            } else {
                Size(end - start, size.height)
            }
        drawTrackPath(
            orientation,
            trackOffset,
            trackSize,
            inactiveTrackColor.copy(alpha = 0.3f),
            0f,
            0f,//formerly endCornerRadius
            shouldFill = true,
            trackPosition = TrackPosition.START
        )
        drawTrackPath(
            orientation,
            trackOffset,
            trackSize,
            inactiveTrackColor,
            0f,
            0f,//formerly endCornerRadius
            shouldFill = false,
            trackPosition = TrackPosition.START
        )
        val stopIndicatorOffset =
            if (isVertical) {
                Offset(center.x, start + cornerSize)
            } else if (isRtl) {
                Offset(size.width - start - cornerSize, center.y)
            } else {
                Offset(start + cornerSize, center.y)
            }
        //drawStopIndicator?.invoke(this, stopIndicatorOffset)
    }
    // inactive track
    var inactiveTrackThreshold = sliderEnd - endGap
    if (!enableCornerShrinking || tickFractions.isNotEmpty()) {
        inactiveTrackThreshold -= cornerSize
    }
    val adjustedSliderValueStart =
        if (isCentered) {
            max(sliderValueEnd, centerAxis)
        } else {
            sliderValueEnd
        }
    if (adjustedSliderValueStart < inactiveTrackThreshold) {
        val startCornerRadius = if (isRtlHorizontal) cornerSize else insideCornerSize
        val endCornerRadius = if (isRtlHorizontal) insideCornerSize else cornerSize
        val start = adjustedSliderValueStart + endGap
        val end = sliderEnd
        val inactiveTrackWidth = end - start
        val trackOffset =
            if (isVertical) {
                Offset(0f, start)
            } else if (isRtl) {
                Offset(0f, 0f)
            } else {
                Offset(start, 0f)
            }
        val size =
            if (isVertical) {
                Size(size.width, inactiveTrackWidth)
            } else if (isRtl && !isRangeSlider) {
                Size(size.width - start, size.height)
            } else {
                Size(inactiveTrackWidth, size.height)
            }
        drawTrackPath(
            orientation,
            trackOffset,
            size,
            inactiveTrackColor.copy(alpha = 0.3f),
            0f, //formerly startCornerRadius
            0f,
            shouldFill = true,
            trackPosition = TrackPosition.END
        )
        drawTrackPath(
            orientation,
            trackOffset,
            size,
            inactiveTrackColor,
            0f, //formerly startCornerRadius
            0f,
            shouldFill = false,
            trackPosition = TrackPosition.END
        )
        val stopIndicatorOffset =
            if (isVertical) {
                Offset(center.x, end - cornerSize)
            } else if (isRtl) {
                Offset(cornerSize, center.y)
            } else {
                Offset(end - cornerSize, center.y)
            }
        //drawStopIndicator?.invoke(this, stopIndicatorOffset)
    }
    // active track
    val activeTrackStart =
        if (isCentered) {
            adjustedSliderValueEnd + if (adjustedSliderValueEnd < centerAxis) startGap else 0f
        } else if (isRangeSlider) {
            sliderValueStart + startGap
        } else {
            0f
        }
    val activeTrackEnd =
        if (isCentered) {
            adjustedSliderValueStart - if (adjustedSliderValueStart > centerAxis) endGap else 0f
        } else {
            sliderValueEnd - endGap
        }
    val startCornerRadius =
        if (isRtlHorizontal || isCentered || isRangeSlider) cornerSize /* insideCornerSize */ else cornerSize
    val endCornerRadius =
        if (isRtlHorizontal && !isCentered && !isRangeSlider) cornerSize else cornerSize /* insideCornerSize */
    val activeTrackWidth =
        if (isRtlHorizontal && !isCentered && !isRangeSlider) activeTrackEnd
        else activeTrackEnd - activeTrackStart

    val activeTrackThreshold =
        if (!enableCornerShrinking || tickFractions.isNotEmpty()) startCornerRadius else 0f
    if (activeTrackWidth > activeTrackThreshold) {
        val trackOffset =
            if (isVertical) {
                Offset(0f, activeTrackStart)
            } else if (isRtl) {
                Offset(size.width - activeTrackEnd, 0f)
            } else {
                Offset(activeTrackStart, 0f)
            }

        val size =
            if (isVertical) {
                Size(size.width, activeTrackWidth)
            } else if (isRtl && !isCentered && !isRangeSlider) {
                Size(activeTrackEnd, size.height)
            } else {
                Size(activeTrackWidth, size.height)
            }
        drawTrackPath(
            orientation,
            trackOffset,
            size,
            activeTrackColor,
            0f,
            0f,
            trackPosition = TrackPosition.CENTER
        )
    }

    val start = sliderStart + cornerSize
    val end = sliderEnd - cornerSize
    val activeTrack = activeTrackStart..activeTrackEnd
    val tickCenterGap = centerAxis - endGap..centerAxis + endGap
    val tickStartGap = sliderValueStart - startGap..sliderValueStart + startGap
    val tickEndGap = sliderValueEnd - endGap..sliderValueEnd + endGap
    tickFractions.forEachIndexed { index, tick ->
        // skip ticks that fall on the stop indicator
        if (drawStopIndicator != null) {
            val isStopIndicatorAtStart = (isCentered || isRangeSlider) && index == 0
            if (isStopIndicatorAtStart || index == tickFractions.size - 1) {
                return@forEachIndexed
            }
        }

        val centerTick = lerp(start, end, tick)
        // skip ticks that fall on a gap
        if (
            (isCentered && centerTick in tickCenterGap) ||
            (isRangeSlider && centerTick in tickStartGap) ||
            centerTick in tickEndGap
        ) {
            return@forEachIndexed
        }
        val offset =
            if (isVertical) {
                Offset(center.x, centerTick)
            } else if (isRtl) {
                Offset(size.width - centerTick, center.y)
            } else {
                Offset(centerTick, center.y)
            }
        drawTick(
            this,
            offset,
            if (centerTick in activeTrack) activeTickColor else inactiveTickColor,
        )
    }
}

private fun DrawScope.drawTrackPath(
    orientation: Orientation,
    offset: Offset,
    size: Size,
    color: Color,
    startCornerRadius: Float,
    endCornerRadius: Float,
    shouldFill: Boolean = false,
    trackPosition: TrackPosition
) {
    val startCorner = CornerRadius(startCornerRadius, startCornerRadius)
    val endCorner = CornerRadius(endCornerRadius, endCornerRadius)
    val track =
        if (orientation == Vertical) {
            RoundRect(
                rect = Rect(offset, size = Size(size.width, size.height)),
                topLeft = startCorner,
                topRight = startCorner,
                bottomRight = endCorner,
                bottomLeft = endCorner,
            )
        } else {
            RoundRect(
                rect = Rect(offset, size = Size(size.width, 100f)),
                topLeft = startCorner,
                topRight = endCorner,
                bottomRight = endCorner,
                bottomLeft = startCorner,
            )
        }
    //trackPath.addRoundRect(track)
    val radiusPx: Float = 20f

    trackPath.apply {
        val height = size.height
        val radius = radiusPx //height / 2f

        val startX = offset.x
        val endX = offset.x + size.width
        val topY = offset.y
        val bottomY = offset.y + height
        val boxBottom = offset.y + (radius * 2)
        val boxTop = bottomY - (radius * 2)

        when (trackPosition) {
            TrackPosition.START -> {
                moveTo(startX + radius, topY)
                lineTo(endX + radius, topY)

                arcTo(
                    rect = Rect(
                        left = endX,
                        right = endX + (radius * 2),
                        top = topY,
                        bottom = boxBottom
                    ),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )

                lineTo(endX, boxTop)

                arcTo(
                    rect = Rect(
                        left = endX,
                        right = endX + (radius * 2),
                        top = boxTop,
                        bottom = bottomY
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )

                lineTo(startX + radius, bottomY)

                arcTo(
                    rect = Rect(
                        left = startX,
                        right = startX + (radius * 2),
                        top = boxTop,
                        bottom = bottomY
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(startX, boxBottom)

                arcTo(
                    rect = Rect(
                        left = startX,
                        right = startX + (radius * 2),
                        top = topY,
                        bottom = boxBottom
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                close()
            }
            TrackPosition.CENTER -> {
                moveTo(startX + radius, topY)
                lineTo(endX - radius, topY)

                arcTo(
                    rect = Rect(
                        left = endX - (radius * 2),
                        right = endX,
                        top = topY,
                        bottom = boxBottom
                    ),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(endX, boxTop)

                arcTo(
                    rect = Rect(
                        left = endX - (radius * 2),
                        right = endX,
                        top = boxTop,
                        bottom = bottomY
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(startX + radius, bottomY)

                arcTo(
                    rect = Rect(
                        left = startX,
                        right = startX + (radius * 2),
                        top = boxTop,
                        bottom = bottomY
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(startX, boxBottom)

                arcTo(
                    rect = Rect(
                        left = startX,
                        right = startX + (radius * 2),
                        top = topY,
                        bottom = boxBottom
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                close()
            }
            TrackPosition.END -> {
                moveTo(startX, topY)
                lineTo(endX - radius, topY)

                arcTo(
                    rect = Rect(
                        left = endX - (radius * 2),
                        right = endX,
                        top = topY,
                        bottom = boxBottom
                    ),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(endX, boxTop)

                arcTo(
                    rect = Rect(
                        left = endX - (radius * 2),
                        right = endX,
                        top = boxTop,
                        bottom = bottomY
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(startX, bottomY)

                arcTo(
                    rect = Rect(
                        left = startX - (radius * 2),
                        right = startX,
                        top = boxTop,
                        bottom = bottomY
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )

                lineTo(startX, boxBottom)

                arcTo(
                    rect = Rect(
                        left = startX - (radius * 2),
                        right = startX,
                        top = topY,
                        bottom = boxBottom
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )

                close()
            }
        }
    }
    drawPath(trackPath, color, style = if (shouldFill) Fill else Stroke(width = 6f))
    trackPath.rewind()
}

/**
 * The Default stop indicator.
 *
 * @param offset the coordinate where the indicator is to be drawn.
 * @param size the size of the indicator.
 * @param color the color of the indicator.
 */
fun DrawScope.drawStopIndicator(offset: Offset, size: Dp, color: Color) {
    drawCircle(color = color, center = offset, radius = size.toPx() / 2f)
}

/** The default size for the stop indicator at the end of the track. */
val TrackStopIndicatorSize: Dp = 4.0.dp

/** The default size for the ticks if steps are greater than 0. */
val TickSize: Dp = 4.0.dp

private val trackPath = Path()

@Stable
internal fun thumbColor(enabled: Boolean): Color =
    if (enabled) Blue1 else Grey2

@Stable
internal fun trackColor(enabled: Boolean, active: Boolean): Color =
    if (enabled) {
        if (active) White1 else Color.DarkGray
    } else {
        if (active) Grey2 else Purple40
    }

@Stable
internal fun tickColor(enabled: Boolean, active: Boolean): Color =
    if (enabled) {
        if (active) Color.Yellow else Color.Green
    } else {
        if (active) Grey2 else Purple40
    }

class ConcaveTopLeftShape(private val radiusPx: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            // Start at the end of the top-left curve on the top edge
            moveTo(radiusPx, 0f)

            // Draw top edge to top-right
            lineTo(size.width, 0f)
            // Draw right edge to bottom-right
            lineTo(size.width, size.height)
            // Draw bottom edge to bottom-left
            lineTo(0f, size.height)
            //Draw left edge up to where the concave curve begins
            lineTo(0f, radiusPx)

            // Inverted/Concave corner bending into itself:
            // This sweeps an arc from (0, radiusPx) to (radiusPx, 0)
            // using a bounding box centered on (0, 0)
            arcTo(
                rect = Rect(
                    left = -radiusPx,
                    top = -radiusPx,
                    right = radiusPx,
                    bottom = radiusPx
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f, // Negative sweep bends it inward
                forceMoveTo = false
            )
            close()
        }

        val path2 = Path().apply {
            lineTo(size.width, 0f)

            arcTo(
                rect = Rect(
                    left = size.height,
                    right = -(size.height),
                    top = size.height,
                    bottom = size.height
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )

            lineTo(0f, size.height)
            lineTo(0f, 0f)
        }

        return Outline.Generic(path)
    }

}

private fun Path.addCustomShape(radiusPx: Float) {

}

enum class TrackPosition {
    START, CENTER, END
}