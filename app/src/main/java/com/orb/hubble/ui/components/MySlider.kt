package com.orb.hubble.ui.components

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.TickSize
import androidx.compose.material3.SliderDefaults.TrackStopIndicatorSize
import androidx.compose.material3.SliderDefaults.colors
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.orb.hubble.ui.theme.Grey1
import com.orb.hubble.ui.theme.Grey2
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
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
    inactiveColor: Color = Grey2
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

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

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