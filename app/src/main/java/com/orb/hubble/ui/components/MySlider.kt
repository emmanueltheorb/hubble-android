package com.orb.hubble.ui.components

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.TickSize
import androidx.compose.material3.SliderDefaults.TrackStopIndicatorSize
import androidx.compose.material3.SliderDefaults.colors
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.material3.SliderState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import com.orb.hubble.ui.theme.Black4
import com.orb.hubble.ui.theme.Grey1
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.White1
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
@ExperimentalMaterial3Api
fun MySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (MySliderState) -> Unit = {

    },
    track: @Composable (MySliderState) -> Unit = { sliderState ->

    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val state = rememberMySliderState(value, 0 ,onValueChangeFinished, valueRange)
    //state.onValueChangeFinished = onValueChangeFinished
    state.onValueChange = onValueChange
    state.value = value

    MySliderImpl(
        state = state,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MySliderImpl(
    state: MySliderState,
    modifier: Modifier,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    thumb: @Composable (MySliderState) -> Unit,
    track: @Composable (MySliderState) -> Unit
) {
    // Determine dragging and tap logic
    // We use a simplified version of the internal modifiers
    val press = Modifier.mySliderTapModifier(state, interactionSource, enabled)

    val drag =
        Modifier.draggable(
            orientation = state.orientation,
            reverseDirection = false,
            enabled = enabled,
            interactionSource = interactionSource,
            onDragStopped = { state.gestureEndAction() },
            startDragImmediately = state.isDragging,
            state = state,
        )

    Layout(
        content = {
            Box(modifier = Modifier.layoutId("thumb")) { thumb(state) }
            Box(modifier = Modifier.layoutId("track")) { track(state) }
        },
        modifier = modifier
            .heightIn(min = 1.dp) // Constrain the touchable area height
            .then(press)
            .then(drag)
    ) { measurables, constraints ->
        val thumbPlaceable = measurables.first { it.layoutId == "thumb" }.measure(constraints)
        val trackPlaceable = measurables.first { it.layoutId == "track" }.measure(
            constraints.copy(minHeight = 0)
        )

        val sliderWidth = constraints.maxWidth
        val sliderHeight = max(thumbPlaceable.height, trackPlaceable.height)

        // Important: Update SliderState dimensions so it knows how to calculate
        // offsets based on the ACTUAL width of the track
        state.updateDimensions(newTotalWidth = sliderWidth, newTotalHeight = sliderHeight)

        // We tell the state the thumb is 0-width for the math,
        // so the active track reaches the absolute 0 and absolute Max.
        //state.thumbWidth = 0

        //state.totalWidth = sliderWidth

        layout(constraints.maxWidth, sliderHeight) {
            // Place track at the very start (0,0) - No more padding!
            trackPlaceable.placeRelative(0, (sliderHeight - trackPlaceable.height) / 2)

            // Calculate thumb position based on fraction
            val thumbOffsetX = (sliderWidth * state.coercedValueAsFraction).roundToInt() - (thumbPlaceable.width / 2)
            val thumbOffsetY = (sliderHeight - thumbPlaceable.height) / 2
            thumbPlaceable.placeRelative(thumbOffsetX, thumbOffsetY)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyHorizontalTrack(
    modifier: Modifier = Modifier,
    sliderState: MySliderState,
    interactionSource: MutableInteractionSource,
    activeColor: Color = Grey1,
    inactiveColor: Color = Black4 // formerly Grey2
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    val trackHeight by animateDpAsState(
        targetValue = if (isInteracting) 6.dp else 1.dp,
        animationSpec = tween(durationMillis = 150),
        label = "TrackHeight"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        val width = size.width
        val height = size.height
        val cornerRadius = CornerRadius(height / 2, height / 2)

        val sliderRange = sliderState.valueRange.endInclusive - sliderState.valueRange.start
        val progress = (sliderState.value - sliderState.valueRange.start) / sliderRange

        drawRect(
            color = inactiveColor,
            size = size
        )

        drawRect(
            color = activeColor,
            size = Size(width * progress, height)
        )
    }
}

@Composable
fun MyInvisibleThumb(
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.size(20.dp))
}

//@Stable
//class MySliderState2(
//    initialValue: Float = 0f,
//    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
//    var onValueChangeFinished: (() -> Unit)? = null
//) : DraggableState {
//
//    var value by mutableFloatStateOf(initialValue.coerceIn(valueRange))
//
//    var onValueChange: ((Float) -> Unit)? = null
//
//    var totalWidth by mutableFloatStateOf(0f)
//    var isDragging by mutableStateOf(false)
//        private set
//
//    /**
//     * Converts the current value to a fraction (0.0 to 1.0)
//     * based on the provided [valueRange].
//     */
//    val coercedValueAsFraction: Float
//        get() = MathUtils.lerpInverse(valueRange.start, valueRange.endInclusive, value)
//
//    private val dragScope: DragScope = object : DragScope {
//        override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
//    }
//
//    private val scrollMutex = MutatorMutex()
//
//    override suspend fun drag(
//        dragPriority: MutatePriority,
//        block: suspend DragScope.() -> Unit
//    ): Unit = coroutineScope {
//        isDragging = true
//        scrollMutex.mutateWith(dragScope, dragPriority, block)
//        isDragging = false
//    }
//
//    override fun dispatchRawDelta(delta: Float) {
//        val rangeLength = valueRange.endInclusive - valueRange.start
//        if (totalWidth > 0) {
//            // Calculate how much the value should change based on pixel delta
//            val valueDelta = (delta / totalWidth) * rangeLength
//            updateValue(value + valueDelta)
//        }
//    }
//
//    fun updateValue(newValue: Float) {
//        val coerced = newValue.coerceIn(valueRange)
//        if (onValueChange != null) {
//            onValueChange?.invoke(coerced)
//        } else {
//            value = coerced
//        }
//    }
//
//    fun valueFromOffset(x: Float): Float {
//        val rangeLength = valueRange.endInclusive - valueRange.start
//        val fraction = if (totalWidth > 0) (x / totalWidth).coerceIn(0f, 1f) else 0f
//        return valueRange.start + (fraction * rangeLength)
//    }
//
//    companion object {
//        @OptIn(ExperimentalMaterial3Api::class)
//        fun Saver(
//            onValueChangeFinished: (() -> Unit)?,
//            valueRange: ClosedFloatingPointRange<Float>,
//        ): Saver<MySliderState, *> = listSaver(
//            save = { listOf(it.value) },
//            restore = {
//                MySliderState(
//                    initialValue = it[0],
//                    valueRange = valueRange,
//                    onValueChangeFinished = onValueChangeFinished
//                )
//            }
//        )
//    }
//}

object MathUtils {
    fun lerpInverse(start: Float, end: Float, value: Float): Float {
        return if (start != end) ((value - start) / (end - start)).coerceIn(0f, 1f) else 0f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberMySliderState(
    value: Float = 0f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
): MySliderState {
    return rememberSaveable(
        valueRange,
        saver = MySliderState.Saver(onValueChangeFinished, valueRange)
    ) {
        MySliderState(value, steps,onValueChangeFinished, valueRange)
    }
}

@ExperimentalMaterial3Api
class MySliderState(
    value: Float = 0f,
    @param: IntRange(from = 0) val steps: Int = 0,
    var onValueChangeFinished: (() -> Unit)? = null,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) : DraggableState {

    private var valueState by mutableFloatStateOf(value)

    /** [Float] that indicates the value that the thumb currently is in respect to the track. */
    var value: Float
        set(newVal) {
            valueState =
                if (shouldAutoSnap) {
                    calculateSnappedValue(newVal)
                } else {
                    newVal
                }
        }
        get() = valueState

    private fun calculateSnappedValue(newVal: Float): Float {
        val coercedValue = newVal.coerceIn(valueRange.start, valueRange.endInclusive)
        return snapValueToTick(
            coercedValue,
            tickFractions,
            valueRange.start,
            valueRange.endInclusive,
        )
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit,
    ): Unit = coroutineScope {
        isDragging = true
        scrollMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        val maxPx: Float
        val minPx: Float
        if (orientation == Orientation.Vertical) {
            maxPx = max(totalHeight - thumbHeight / 2f, 0f)
            minPx = min(thumbHeight / 2f, maxPx)
        } else {
            maxPx = max(totalWidth - thumbWidth / 2f, 0f)
            minPx = min(thumbWidth / 2f, maxPx)
        }
        rawOffset = (rawOffset + delta + pressOffset)
        pressOffset = 0f
        val offsetInTrack = snapValueToTick(rawOffset, tickFractions, minPx, maxPx)
        val scaledUserValue = scaleToUserValue(minPx, maxPx, offsetInTrack)
        if (scaledUserValue != this.value) {
            if (onValueChange != null) {
                onValueChange?.let { it(scaledUserValue) }
            } else {
                this.value = scaledUserValue
            }
        }
    }

    /** Callback in which value should be updated. */
    var onValueChange: ((Float) -> Unit)? = null

    /** Controls the auto-snapping mechanism, disabling it may be useful for custom animations. */
    @get:JvmName("shouldAutoSnap") var shouldAutoSnap: Boolean = true

    internal val tickFractions = stepsToTickFractions(steps)
    var totalWidth by mutableIntStateOf(0)
    var totalHeight by mutableIntStateOf(0)
    internal var isRtl = false
    internal var thumbWidth by mutableIntStateOf(0)
    internal var thumbHeight by mutableIntStateOf(0)
    internal var orientation = Orientation.Horizontal
    internal var reverseVerticalDirection = false

    /** The fraction of the track that the thumb currently is in. */
    val coercedValueAsFraction: Float
        get() =
            calcFraction(
                valueRange.start,
                valueRange.endInclusive,
                value.coerceIn(valueRange.start, valueRange.endInclusive),
            )

    var isDragging by mutableStateOf(false)
        private set

    internal fun updateDimensions(newTotalWidth: Int, newTotalHeight: Int) {
        totalWidth = newTotalWidth
        totalHeight = newTotalHeight
    }

    internal val gestureEndAction = {
        if (!isDragging) {
            // check isDragging in case the change is still in progress (touch -> drag case)
            onValueChangeFinished?.invoke()
        }
    }

    internal fun onPress(pos: Offset) {
        val to =
            if (orientation == Orientation.Vertical) {
                if (reverseVerticalDirection) totalHeight - pos.y else pos.y
            } else {
                if (isRtl) totalWidth - pos.x else pos.x
            }
        pressOffset = to - rawOffset
    }

    private var rawOffset by mutableFloatStateOf(scaleToOffset(0f, 0f, value))
    private var pressOffset by mutableFloatStateOf(0f)
    private val dragScope: DragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
        }

    private val scrollMutex = MutatorMutex()

    private fun scaleToUserValue(minPx: Float, maxPx: Float, offset: Float) =
        scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

    private fun scaleToOffset(minPx: Float, maxPx: Float, userValue: Float) =
        scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

    companion object {
        /**
         * The default [Saver] implementation for [SliderState].
         *
         * @param onValueChangeFinished lambda to be invoked when value change has ended. This
         *   callback shouldn't be used to update the range slider values (use [onValueChange] for
         *   that), but rather to know when the user has completed selecting a new value by ending a
         *   drag or a click.
         * @param valueRange range of values that Slider values can take. [value] will be coerced to
         *   this range.
         */
        fun Saver(
            onValueChangeFinished: (() -> Unit)?,
            valueRange: ClosedFloatingPointRange<Float>,
        ): Saver<MySliderState, *> =
            listSaver(
                save = { listOf(it.value, it.steps) },
                restore = {
                    MySliderState(
                        value = it[0] as Float,
                        steps = it[1] as Int,
                        onValueChangeFinished = onValueChangeFinished,
                        valueRange = valueRange,
                    )
                },
            )
    }
}

@Composable
fun MyThumb(
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbSize: DpSize = DpSize(4.0.dp, 44.0.dp),
) {
    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }

    val size =
        if (interactions.isNotEmpty()) {
            thumbSize.copy(width = thumbSize.width / 2)
        } else {
            thumbSize
        }
    Spacer(
        modifier
            .size(size)
            .hoverable(interactionSource = interactionSource)
            .background(White1, /* ShapeKeyTokens.CornerFull */)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRangeSlider(
    currentValue: Float,
    onCurrentValueChange: (Float) -> Unit,
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
) {
    val startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val centerInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    RangeSlider1(
        currentValue = currentValue,
        onCurrentValueChange = onCurrentValueChange,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        centerInteractionSource = centerInteractionSource,
        startThumb = {
            MyThumb(
                interactionSource = startInteractionSource,
                enabled = enabled,
            )
        },
        endThumb = {
            MyThumb(
                interactionSource = endInteractionSource,
                enabled = enabled,
            )
//            SliderDefaults.Thumb(
//                interactionSource = endInteractionSource,
//                colors = colors,
//                enabled = enabled,
//            )
        },
        centerThumb = {
            MyThumb(
                interactionSource = centerInteractionSource,
                enabled = enabled,
            )
        },
        track = { rangeSliderState ->
            MyRangeSliderTrack(
                colors = colors,
                enabled = enabled,
                rangeSliderState = rangeSliderState
            )
        },
    )
}

@Composable
@ExperimentalMaterial3Api
fun RangeSlider1(
    currentValue: Float,
    onCurrentValueChange: (Float) -> Unit,
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    centerInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    startThumb: @Composable (MyRangeSliderState) -> Unit = {
        MyThumb(
            interactionSource = startInteractionSource,
            enabled = enabled,
        )
    },
    endThumb: @Composable (MyRangeSliderState) -> Unit = {
        MyThumb(
            interactionSource = endInteractionSource,
            enabled = enabled,
        )
    },
    centerThumb: @Composable (MyRangeSliderState) -> Unit = {
        MyThumb(
            interactionSource = centerInteractionSource,
            enabled = enabled,
        )
    },
    track: @Composable (MyRangeSliderState) -> Unit = { rangeSliderState ->
        MyRangeSliderTrack(
            colors = colors,
            enabled = enabled,
            rangeSliderState = rangeSliderState
        )
    },
    @IntRange(from = 0) steps: Int = 0,
) {
    val state =
        remember(steps, valueRange) {
            MyRangeSliderState(
                currentValue,
                value.start,
                value.endInclusive,
                steps,
                onValueChangeFinished,
                valueRange,
            )
        }

    state.onValueChangeFinished = onValueChangeFinished
    state.onValueChange = { onValueChange(it.start..it.endInclusive) }
    state.activeRangeStart = value.start
    state.activeRangeEnd = value.endInclusive
    state.onCurrentValueChange = onCurrentValueChange
    state.value = currentValue

    RangeSlider2(
        modifier = modifier,
        state = state,
        enabled = enabled,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        centerInteractionSource = centerInteractionSource,
        startThumb = startThumb,
        endThumb = endThumb,
        centerThumb = centerThumb,
        track = track,
    )
}

@Composable
@ExperimentalMaterial3Api
fun RangeSlider2(
    state: MyRangeSliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
    startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    centerInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    startThumb: @Composable (MyRangeSliderState) -> Unit = {
        MyThumb(
            interactionSource = startInteractionSource,
            enabled = enabled,
        )
    },
    endThumb: @Composable (MyRangeSliderState) -> Unit = {
        MyThumb(
            interactionSource = endInteractionSource,
            enabled = enabled,
        )
    },
    centerThumb: @Composable (MyRangeSliderState) -> Unit = {
        MyThumb(
            interactionSource = centerInteractionSource,
            enabled = enabled,
        )
    },
    track: @Composable (MyRangeSliderState) -> Unit = { rangeSliderState ->
        MyRangeSliderTrack(
            colors = colors,
            enabled = enabled,
            rangeSliderState = rangeSliderState
        )
    },
) {
    require(state.steps >= 0) { "steps should be >= 0" }

    MyRangeSliderImpl(
        modifier = modifier,
        state = state,
        enabled = enabled,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        centerInteractionSource = centerInteractionSource,
        startThumb = startThumb,
        endThumb = endThumb,
        centerThumb = centerThumb,
        track = track,
    )
}

@ExperimentalMaterial3Api
class MyRangeSliderState(
    value: Float = 0f,
    activeRangeStart: Float = 0f,
    activeRangeEnd: Float = 1f,
    @param: IntRange(from = 0) val steps: Int = 0,
    var onValueChangeFinished: (() -> Unit)? = null,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
): DraggableState {
    private var valueState by mutableFloatStateOf(value)
    private var activeRangeStartState by mutableFloatStateOf(activeRangeStart)
    private var activeRangeEndState by mutableFloatStateOf(activeRangeEnd)

    /** [Float] that indicates the value that the thumb currently is in respect to the track. */
    var value: Float
        set(newVal) {
            valueState =
                if (shouldAutoSnap) {
                    calculateSnappedValue(newVal)
                } else {
                    newVal
                }
        }
        get() = valueState

    /** [Float] that indicates the start of the current active range for the [RangeSlider]. */
    var activeRangeStart: Float
        set(newVal) {
            val coercedValue = newVal.coerceIn(valueRange.start, activeRangeEnd)
            val snappedValue =
                snapValueToTick(
                    coercedValue,
                    tickFractions,
                    valueRange.start,
                    valueRange.endInclusive,
                )
            activeRangeStartState = snappedValue
        }
        get() = activeRangeStartState

    /** [Float] that indicates the end of the current active range for the [RangeSlider]. */
    var activeRangeEnd: Float
        set(newVal) {
            val coercedValue = newVal.coerceIn(activeRangeStart, valueRange.endInclusive)
            val snappedValue =
                snapValueToTick(
                    coercedValue,
                    tickFractions,
                    valueRange.start,
                    valueRange.endInclusive,
                )
            activeRangeEndState = snappedValue
        }
        get() = activeRangeEndState

    private fun calculateSnappedValue(newVal: Float): Float {
        val coercedValue = newVal.coerceIn(valueRange.start, valueRange.endInclusive)
        return snapValueToTick(
            coercedValue,
            tickFractions,
            valueRange.start,
            valueRange.endInclusive,
        )
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit,
    ): Unit = coroutineScope {
        isDragging = true
        scrollMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        val maxPx: Float
        val minPx: Float
        if (orientation == Orientation.Vertical) {
            maxPx = max(totalHeight - thumbHeight / 2f, 0f)
            minPx = min(thumbHeight / 2f, maxPx)
        } else {
            maxPx = max(totalWidth - thumbWidth / 2f, 0f)
            minPx = min(thumbWidth / 2f, maxPx)
        }
        rawOffset = (rawOffset + delta + pressOffset)
        pressOffset = 0f
        val offsetInTrack = snapValueToTick(rawOffset, tickFractions, minPx, maxPx)
        val scaledUserValue = scaleToUserValueFloat(minPx, maxPx, offsetInTrack)
        if (scaledUserValue != this.value) {
            if (onCurrentValueChange != null) {
                onCurrentValueChange?.let { it(scaledUserValue) }
            } else {
                this.value = scaledUserValue
            }
        }
    }

    var onCurrentValueChange: ((Float) -> Unit)? = null
    internal var onValueChange: ((SliderRange) -> Unit)? = null
    /** Controls the auto-snapping mechanism, disabling it may be useful for custom animations. */
    @get:JvmName("shouldAutoSnap") var shouldAutoSnap: Boolean = true

    var totalHeight by mutableIntStateOf(0)

    internal var thumbWidth by mutableIntStateOf(0)
    internal var thumbHeight by mutableIntStateOf(0)
    internal var orientation = Orientation.Horizontal
    internal var reverseVerticalDirection = false

    internal fun onPress(pos: Offset) {
        val to =
            if (orientation == Orientation.Vertical) {
                if (reverseVerticalDirection) totalHeight - pos.y else pos.y
            } else {
                if (isRtl) totalWidth - pos.x else pos.x
            }
        pressOffset = to - rawOffset
    }

    private var rawOffset by mutableFloatStateOf(scaleToOffset(0f, 0f, value))
    private var pressOffset by mutableFloatStateOf(0f)
    private val dragScope: DragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
        }

    private val scrollMutex = MutatorMutex()

    internal val tickFractions = stepsToTickFractions(steps)

    internal var startThumbWidth by mutableFloatStateOf(0f)
    internal var startThumbHeight by mutableFloatStateOf(0f)
    internal var endThumbWidth by mutableFloatStateOf(0f)
    internal var endThumbHeight by mutableFloatStateOf(0f)
    internal var totalWidth by mutableIntStateOf(0)
    internal var rawOffsetStart by mutableFloatStateOf(0f)
    internal var rawOffsetEnd by mutableFloatStateOf(0f)
    internal var isDragging by mutableStateOf(false)

    internal var isRtl by mutableStateOf(false)

    internal val gestureEndAction1 = {
        if (!isDragging) {
            // check isDragging in case the change is still in progress (touch -> drag case)
            onValueChangeFinished?.invoke()
        }
    }
    internal val gestureEndAction: (Boolean) -> Unit = { onValueChangeFinished?.invoke() }

    private var maxPx by mutableFloatStateOf(0f)
    private var minPx by mutableFloatStateOf(0f)

    internal fun onDrag(isStart: Boolean, offset: Float) {
        val offsetRange =
            if (isStart) {
                rawOffsetStart = (rawOffsetStart + offset)
                rawOffsetEnd = scaleToOffset(minPx, maxPx, activeRangeEnd)
                val offsetEnd = rawOffsetEnd
                var offsetStart = rawOffsetStart.coerceIn(minPx, offsetEnd)
                offsetStart = snapValueToTick(offsetStart, tickFractions, minPx, maxPx)
                SliderRange(offsetStart.coerceAtMost(offsetEnd), offsetEnd)
            } else {
                rawOffsetEnd = (rawOffsetEnd + offset)
                rawOffsetStart = scaleToOffset(minPx, maxPx, activeRangeStart)
                val offsetStart = rawOffsetStart
                var offsetEnd = rawOffsetEnd.coerceIn(offsetStart, maxPx)
                offsetEnd = snapValueToTick(offsetEnd, tickFractions, minPx, maxPx)
                SliderRange(offsetStart, offsetEnd.coerceAtLeast(offsetStart))
            }
        val scaledUserValue = scaleToUserValue(isStart, minPx, maxPx, offsetRange)
        if (scaledUserValue != SliderRange(activeRangeStart, activeRangeEnd)) {
            if (onValueChange != null) {
                onValueChange?.let { it(scaledUserValue) }
            } else {
                this.activeRangeStart = scaledUserValue.start
                this.activeRangeEnd = scaledUserValue.endInclusive
            }
        }
    }

    internal val coercedActiveRangeStartAsFraction
        get() = calcFraction(valueRange.start, valueRange.endInclusive, activeRangeStart)

    internal val coercedActiveRangeEndAsFraction
        get() = calcFraction(valueRange.start, valueRange.endInclusive, activeRangeEnd)

    val coercedValueAsFraction: Float
        get() =
            calcFraction(
                valueRange.start,
                valueRange.endInclusive,
                value.coerceIn(activeRangeStart, activeRangeEnd),
            )

    internal val startSteps
        get() = floor(steps * coercedActiveRangeEndAsFraction).toInt()

    internal val endSteps
        get() = floor(steps * (1f - coercedActiveRangeStartAsFraction)).toInt()

    private fun scaleToUserValueFloat(minPx: Float, maxPx: Float, offset: Float) =
        scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

    private fun scaleToOffsetFloat(minPx: Float, maxPx: Float, userValue: Float) =
        scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

    // scales range offset from within minPx..maxPx to within valueRange.start..valueRange.end
    private fun scaleToUserValue(
        isStart: Boolean,
        minPx: Float,
        maxPx: Float,
        offset: SliderRange,
    ) = scale(isStart, minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

    // scales float userValue within valueRange.start..valueRange.end to within minPx..maxPx
    private fun scaleToOffset(minPx: Float, maxPx: Float, userValue: Float) =
        scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

    internal fun updateMinMaxPx() {
        val newMaxPx = max(totalWidth - endThumbWidth / 2, 0f)
        val newMinPx = min(startThumbWidth / 2, newMaxPx)
        if (
            !isDragging &&
            (minPx != newMinPx || maxPx != newMaxPx || activeRangeStart != activeRangeEnd)
        ) {
            minPx = newMinPx
            maxPx = newMaxPx
            rawOffsetStart = scaleToOffset(minPx, maxPx, activeRangeStart)
            rawOffsetEnd = scaleToOffset(minPx, maxPx, activeRangeEnd)
        }
    }

    companion object {
        /**
         * The default [Saver] implementation for [RangeSliderState].
         *
         * @param onValueChangeFinished lambda to be invoked when value change has ended. This
         *   callback shouldn't be used to update the range slider values (use [onValueChange] for
         *   that), but rather to know when the user has completed selecting a new value by ending a
         *   drag or a click.
         * @param valueRange range of values that Range Slider values can take. [activeRangeStart]
         *   and [activeRangeEnd] will be coerced to this range.
         */
        fun Saver(
            onValueChangeFinished: (() -> Unit)?,
            valueRange: ClosedFloatingPointRange<Float>,
        ): Saver<MyRangeSliderState, *> =
            listSaver(
                save = { listOf(it.value, it.activeRangeStart, it.activeRangeEnd, it.steps) },
                restore = {
                    MyRangeSliderState(
                        value = it[0] as Float,
                        activeRangeStart = it[1] as Float,
                        activeRangeEnd = it[2] as Float,
                        steps = it[3] as Int,
                        onValueChangeFinished = onValueChangeFinished,
                        valueRange = valueRange,
                    )
                },
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyRangeSliderImpl(
    modifier: Modifier,
    state: MyRangeSliderState,
    enabled: Boolean,
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    centerInteractionSource: MutableInteractionSource,
    startThumb: @Composable ((MyRangeSliderState) -> Unit),
    endThumb: @Composable ((MyRangeSliderState) -> Unit),
    centerThumb: @Composable ((MyRangeSliderState) -> Unit),
    track: @Composable ((MyRangeSliderState) -> Unit),
) {
    state.isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val pressDrag =
        Modifier.rangeSliderPressDragModifier(
            state,
            startInteractionSource,
            endInteractionSource,
            enabled,
        )

    val press = Modifier.mySliderTapModifier2(state, centerInteractionSource, enabled)

    val drag =
        Modifier.draggable(
            orientation = state.orientation,
            reverseDirection = false,
            enabled = enabled,
            interactionSource = centerInteractionSource,
            onDragStopped = { state.gestureEndAction1() },
            startDragImmediately = state.isDragging,
            state = state,
        )

    val startContentDescription = "rangeStart"
    val endContentDescription = "rangeEnd"

    Layout(
        {
            Box(
                modifier =
                    Modifier.layoutId("startThumb")
                        .wrapContentWidth()
                        .onSizeChanged {
                            state.startThumbWidth = it.width.toFloat()
                            state.startThumbHeight = it.height.toFloat()
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = startContentDescription
                        }
                        .focusable(enabled, startInteractionSource)
            ) {
                startThumb(state)
            }
            Box(
                modifier =
                    Modifier.layoutId("endThumb")
                        .wrapContentWidth()
                        .onSizeChanged {
                            state.endThumbWidth = it.width.toFloat()
                            state.endThumbHeight = it.height.toFloat()
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = endContentDescription
                        }
                        .focusable(enabled, endInteractionSource)
            ) {
                endThumb(state)
            }
            Box(modifier = Modifier.layoutId("centerThumb")) { centerThumb(state) }
            Box(modifier = Modifier.layoutId("track")) { track(state) }
        },
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .requiredSizeIn(minWidth = 4.0.dp, minHeight = 44.0.dp)
                .then(pressDrag),
    ) { measurables, constraints ->
        val startThumbPlaceable =
            measurables
                .fastFirst { it.layoutId == "startThumb" }
                .measure(constraints)

        val endThumbPlaceable =
            measurables
                .fastFirst { it.layoutId == "endThumb" }
                .measure(constraints)

        val centerThumbPlaceable =
            measurables
                .fastFirst { it.layoutId == "centerThumb" }
                .measure(constraints)

        val trackPlaceable =
            measurables
                .fastFirst { it.layoutId == "track" }
                .measure(
                    constraints
                        .offset(
                            horizontal = -(startThumbPlaceable.width + endThumbPlaceable.width) / 2
                        )
                        .copy(minHeight = 0)
                )

        val sliderWidth =
            trackPlaceable.width + (startThumbPlaceable.width + endThumbPlaceable.width) / 2
        val sliderHeight =
            maxOf(trackPlaceable.height, startThumbPlaceable.height, endThumbPlaceable.height, centerThumbPlaceable.height)

        state.totalWidth = sliderWidth

        state.updateMinMaxPx()

        val startValueAsFraction = state.coercedActiveRangeStartAsFraction
        val isStartOnFirstOrLastStep =
            startValueAsFraction == state.tickFractions.firstOrNull() ||
                    startValueAsFraction == state.tickFractions.lastOrNull()
        val endValueAsFraction = state.coercedActiveRangeEndAsFraction
        val isEndOnFirstOrLastStep =
            endValueAsFraction == state.tickFractions.firstOrNull() ||
                    endValueAsFraction == state.tickFractions.lastOrNull()
        val trackOffsetX = startThumbPlaceable.width / 2
        val trackCornerSize =
            trackPlaceable[CornerSizeAlignmentLine].let {
                if (it != AlignmentLine.Unspecified) it else 0
            }

        val startThumbOffsetX =
            if (state.steps > 0 && !isStartOnFirstOrLastStep) {
                ((trackPlaceable.width - trackCornerSize * 2) * startValueAsFraction).roundToInt() +
                        trackCornerSize
            } else {
                (trackPlaceable.width * startValueAsFraction).roundToInt()
            }
        // When start thumb and end thumb have different widths,
        // we need to add a correction for the centering of the slider.
        val endCorrection = (startThumbPlaceable.width - endThumbPlaceable.width) / 2
        val endThumbOffsetX =
            if (state.steps > 0 && !isEndOnFirstOrLastStep) {
                ((trackPlaceable.width - trackCornerSize * 2) * endValueAsFraction + endCorrection)
                    .roundToInt() + trackCornerSize
            } else {
                (trackPlaceable.width * endValueAsFraction + endCorrection).roundToInt()
            }
        val trackOffsetY = (sliderHeight - trackPlaceable.height) / 2
        val startThumbOffsetY = (sliderHeight - startThumbPlaceable.height) / 2
        val endThumbOffsetY = (sliderHeight - endThumbPlaceable.height) / 2

        layout(sliderWidth, sliderHeight) {
            trackPlaceable.placeRelative(trackOffsetX, trackOffsetY)
            startThumbPlaceable.placeRelative(startThumbOffsetX, startThumbOffsetY)
            endThumbPlaceable.placeRelative(endThumbOffsetX, endThumbOffsetY)

            // Calculate thumb position based on fraction
            val centerThumbOffsetX = (sliderWidth * state.coercedValueAsFraction).roundToInt() - (centerThumbPlaceable.width / 2)
            val centerThumbOffsetY = (sliderHeight - centerThumbPlaceable.height) / 2
            centerThumbPlaceable.placeRelative(centerThumbOffsetX, centerThumbOffsetY)
        }
    }
}

private fun snapValueToTick(
    current: Float,
    tickFractions: FloatArray,
    minPx: Float,
    maxPx: Float,
): Float {
    // target is a closest anchor to the `current`, if exists
    return tickFractions
        .minByOrNull { abs(lerp(minPx, maxPx, it) - current) }
        ?.run { lerp(minPx, maxPx, this) } ?: current
}

private suspend fun AwaitPointerEventScope.awaitSlop(
    id: PointerId,
    type: PointerType,
): Pair<PointerInputChange, Float>? {
    var initialDelta = 0f
    val postPointerSlop = { pointerInput: PointerInputChange, offset: Float ->
        pointerInput.consume()
        initialDelta = offset
    }
    val afterSlopResult = awaitHorizontalPointerSlopOrCancellation(id, type, postPointerSlop)
    return if (afterSlopResult != null) afterSlopResult to initialDelta else null
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Scale x.start, x.endInclusive from a1..b1 range to a2..b2 range
private fun scale(
    isStart: Boolean,
    a1: Float,
    b1: Float,
    x: SliderRange,
    a2: Float,
    b2: Float,
): SliderRange {
    val start = scale(a1, b1, x.start, a2, b2)
    val end = scale(a1, b1, x.endInclusive, a2, b2)

    return if (isStart) {
        SliderRange(start.coerceAtMost(end), end)
    } else {
        SliderRange(start, end.coerceAtLeast(start))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.mySliderTapModifier(
    state: MySliderState,
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
) =
    if (enabled) {
        pointerInput(state, interactionSource) {
            detectTapGestures(
                onPress = { state.onPress(it) },
                onTap = {
                    state.dispatchRawDelta(0f)
                    state.gestureEndAction()
                },
            )
        }
    } else {
        this
    }

@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.mySliderTapModifier2(
    state: MyRangeSliderState,
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
) =
    if (enabled) {
        pointerInput(state, interactionSource) {
            detectTapGestures(
                onPress = { state.onPress(it) },
                onTap = {
                    state.dispatchRawDelta(0f)
                    state.gestureEndAction1()
                },
            )
        }
    } else {
        this
    }

@OptIn(ExperimentalMaterial3Api::class)
@Stable
private fun Modifier.rangeSliderPressDragModifier(
    state: MyRangeSliderState,
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    enabled: Boolean,
): Modifier =
    if (enabled) {
        pointerInput(startInteractionSource, endInteractionSource, state) {
            val rangeSliderLogic =
                MyRangeSliderLogic(state, startInteractionSource, endInteractionSource)
            coroutineScope {
                awaitEachGesture {
                    val event = awaitFirstDown(requireUnconsumed = false)
                    val interaction = DragInteraction.Start()
                    var posX =
                        if (state.isRtl) state.totalWidth - event.position.x else event.position.x
                    val compare = rangeSliderLogic.compareOffsets(posX)
                    var draggingStart =
                        if (compare != 0) {
                            compare < 0
                        } else {
                            state.rawOffsetStart > posX
                        }

                    awaitSlop(event.id, event.type)?.let {
                        val slop = viewConfiguration.pointerSlop(event.type)
                        val shouldUpdateCapturedThumb =
                            abs(state.rawOffsetEnd - posX) < slop &&
                                    abs(state.rawOffsetStart - posX) < slop
                        if (shouldUpdateCapturedThumb) {
                            val dir = it.second
                            draggingStart = if (state.isRtl) dir >= 0f else dir < 0f
                            posX += it.first.positionChange().x
                        }
                    }

                    rangeSliderLogic.captureThumb(
                        draggingStart,
                        posX,
                        interaction,
                        this@coroutineScope,
                    )

                    val finishInteraction =
                        try {
                            state.isDragging = true
                            val success =
                                horizontalDrag(pointerId = event.id) {
                                    val deltaX = it.positionChange().x
                                    state.onDrag(
                                        draggingStart,
                                        if (state.isRtl) -deltaX else deltaX,
                                    )
                                }
                            if (success) {
                                DragInteraction.Stop(interaction)
                            } else {
                                DragInteraction.Cancel(interaction)
                            }
                        } catch (e: CancellationException) {
                            DragInteraction.Cancel(interaction)
                        } finally {
                            state.isDragging = false
                        }

                    state.gestureEndAction(draggingStart)
                    launch {
                        rangeSliderLogic.activeInteraction(draggingStart).emit(finishInteraction)
                    }
                }
            }
        }
    } else {
        this
    }

/**
 * Immutable float range for [RangeSlider]
 *
 * Used in [RangeSlider] to determine the active track range for the component. The range is as
 * follows: SliderRange.start..SliderRange.endInclusive.
 */
@Immutable
@JvmInline
internal value class SliderRange(val packedValue: Long) {
    /** start of the [SliderRange] */
    @Stable
    val start: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) { "SliderRange is unspecified" }
            return unpackFloat1(packedValue)
        }

    /** End (inclusive) of the [SliderRange] */
    @Stable
    val endInclusive: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) { "SliderRange is unspecified" }
            return unpackFloat2(packedValue)
        }

    companion object {
        /**
         * Represents an unspecified [SliderRange] value, usually a replacement for `null` when a
         * primitive value is desired.
         */
        @Stable val Unspecified = SliderRange(Float.NaN, Float.NaN)
    }

    /** String representation of the [SliderRange] */
    override fun toString() =
        if (isSpecified) {
            "$start..$endInclusive"
        } else {
            "FloatRange.Unspecified"
        }
}

/**
 * Creates a [SliderRange] from a given start and endInclusive float. It requires endInclusive to
 * be >= start.
 *
 * @param start float that indicates the start of the range
 * @param endInclusive float that indicates the end of the range
 */
@Stable
internal fun SliderRange(start: Float, endInclusive: Float): SliderRange {
    val isUnspecified = start.isNaN() && endInclusive.isNaN()

    require(isUnspecified || start <= endInclusive) {
        "start($start) must be <= endInclusive($endInclusive)"
    }
    return SliderRange(packFloats(start, endInclusive))
}

/**
 * Creates a [SliderRange] from a given [ClosedFloatingPointRange]. It requires
 * range.endInclusive >= range.start.
 *
 * @param range the ClosedFloatingPointRange<Float> for the range.
 */
@Stable
internal fun SliderRange(range: ClosedFloatingPointRange<Float>): SliderRange {
    val start = range.start
    val endInclusive = range.endInclusive
    val isUnspecified = start.isNaN() && endInclusive.isNaN()
    require(isUnspecified || start <= endInclusive) {
        "ClosedFloatingPointRange<Float>.start($start) must be <= " +
                "ClosedFloatingPoint.endInclusive($endInclusive)"
    }
    return SliderRange(packFloats(start, endInclusive))
}

/** Check for if a given [SliderRange] is not [SliderRange.Unspecified]. */
@Stable
internal val SliderRange.isSpecified: Boolean
    get() = packedValue != SliderRange.Unspecified.packedValue

internal val CornerSizeAlignmentLine = VerticalAlignmentLine(::min)
