package com.orb.hubble.ui.components.crossPager

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.node.requireView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.getFocusedRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.dispatchOnScrollChanged
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

@Stable
fun Modifier.scrollable(
    state: ScrollableState,
    //orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier =
    scrollable(
        state = state,
        //orientation = orientation,
        enabled = enabled,
        reverseDirection = reverseDirection,
        flingBehavior = flingBehavior,
        interactionSource = interactionSource,
        overscrollEffect = null,
    )

@Stable
fun Modifier.scrollable(
    state: ScrollableState,
    //orientation: Orientation,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    bringIntoViewSpec: BringIntoViewSpec? = null,
) =
    this then
            ScrollableElement(
                state,
                //orientation,
                overscrollEffect,
                enabled,
                reverseDirection,
                flingBehavior,
                interactionSource,
                bringIntoViewSpec,
            )

private class ScrollableElement(
    val state: ScrollableState,
    //val orientation: Orientation,
    val overscrollEffect: OverscrollEffect?,
    val enabled: Boolean,
    val reverseDirection: Boolean,
    val flingBehavior: FlingBehavior?,
    val interactionSource: MutableInteractionSource?,
    val bringIntoViewSpec: BringIntoViewSpec?,
) : ModifierNodeElement<ScrollableNode>() {
    override fun create(): ScrollableNode {
        return ScrollableNode(
            state,
            overscrollEffect,
            flingBehavior,
            //orientation,
            enabled,
            reverseDirection,
            interactionSource,
            bringIntoViewSpec,
        )
    }

    override fun update(node: ScrollableNode) {
        node.update(
            state,
            //orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec,
        )
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        //result = 31 * result + orientation.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + bringIntoViewSpec.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is ScrollableElement) return false

        if (state != other.state) return false
        //if (orientation != other.orientation) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (enabled != other.enabled) return false
        if (reverseDirection != other.reverseDirection) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false
        if (bringIntoViewSpec != other.bringIntoViewSpec) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollable"
        //properties["orientation"] = orientation
        properties["state"] = state
        properties["overscrollEffect"] = overscrollEffect
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
        properties["bringIntoViewSpec"] = bringIntoViewSpec
    }
}

internal class ScrollableNode(
    state: ScrollableState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior?,
    //orientation: Orientation,
    enabled: Boolean,
    reverseDirection: Boolean,
    interactionSource: MutableInteractionSource?,
    bringIntoViewSpec: BringIntoViewSpec?,
) :
    DragGestureNode(
        canDrag = CanDragCalculation,
        enabled = enabled,
        interactionSource = interactionSource,
        orientationLock = null,//state.orientation,
    ),
    KeyInputModifierNode,
    SemanticsModifierNode,
    OnScrollChangedDispatcher {

    override val shouldAutoInvalidate: Boolean = false

    private val nestedScrollDispatcher = NestedScrollDispatcher()

    private val scrollableContainerNode = delegate(ScrollableContainerNode(enabled))

    // Place holder fling behavior, we'll initialize it when the density is available.
    private val defaultFlingBehavior = platformScrollableDefaultFlingBehavior()

    private val scrollingLogic =
        ScrollingLogic(
            scrollableState = state,
            orientation = state.orientation,
            overscrollEffect = overscrollEffect,
            reverseDirection = reverseDirection,
            flingBehavior = flingBehavior ?: defaultFlingBehavior,
            nestedScrollDispatcher = nestedScrollDispatcher,
            onScrollChangedDispatcher = this,
            isScrollableNodeAttached = { isAttached },
        )

    private val nestedScrollConnection =
        ScrollableNestedScrollConnection(enabled = enabled, scrollingLogic = scrollingLogic)

    private val focusTargetModifierNode =
        delegate(FocusTargetModifierNode(focusability = Focusability.Never))

    private val contentInViewNode =
        delegate(
            ContentInViewNode(
                orientation = state.orientation,
                scrollingLogic = scrollingLogic,
                reverseDirection = reverseDirection,
                bringIntoViewSpec = bringIntoViewSpec,
                getFocusedRect = { focusTargetModifierNode.getFocusedRect() },
            )
        )

    private var scrollByAction: ((x: Float, y: Float) -> Boolean)? = null
    private var scrollByOffsetAction: (suspend (Offset) -> Offset)? = null

    private var mouseWheelScrollingLogic: MouseWheelScrollingLogic? = null

    init {
        /** Nested scrolling */
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))

        /** Focus scrolling */
        delegate(BringIntoViewResponderNode(contentInViewNode))
        if (
            @OptIn(ExperimentalFoundationApi::class)
            !ComposeFoundationFlags.isKeepInViewFocusObservationChangeEnabled
        ) {
            delegate(FocusedBoundsObserverNode { contentInViewNode.onFocusBoundsChanged(it) })
        }
    }

    override fun dispatchScrollDeltaInfo(delta: Offset) {
        if (!isAttached) return
        dispatchOnScrollChanged(delta)
    }

    override suspend fun drag(
        forEachDelta: suspend ((dragDelta: DragEvent.DragDelta) -> Unit) -> Unit
    ) {
        with(scrollingLogic) {
            scroll(scrollPriority = MutatePriority.UserInput) {
                //clear any stale orientation rules from the previous swipe loop
                resetGestureOrientation()

                forEachDelta {
                    // Indirect pointer Events should be reverted to account for the reverse we
                    // do in Scrollable. Regular touchscreen events are inverted in scrollable, but
                    // that shouldn't happen for indirect pointer events, so we cancel the reverse
                    // here.
                    val invertIndirectPointer = if (it.isIndirectPointerEvent) -1f else 1f

                    //removed .singleAxisOffset() from it.delta to pass down the full 2D delta offset
                    //to the scrolling logic
                    scrollByWithOverscroll(
                        it.delta * invertIndirectPointer,
                        source = UserInput,
                    )
                }
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {}

    override fun onDragStopped(event: DragEvent.DragStopped) {
        nestedScrollDispatcher.coroutineScope.launch {
            // Indirect pointer Events should be reverted to account for the reverse we
            // do in Scrollable. Regular touchscreen events are inverted in scrollable, but
            // that shouldn't happen for indirect pointer events, so we cancel the reverse
            // here.
            val invertIndirectPointer = if (event.isIndirectPointerEvent) -1f else 1f
            scrollingLogic.onScrollStopped(
                event.velocity * invertIndirectPointer,
                isMouseWheel = false,
            )
        }
    }

    private fun onWheelScrollStopped(velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onScrollStopped(velocity, isMouseWheel = true)
        }
    }

    override fun startDragImmediately(): Boolean {
        return scrollingLogic.shouldScrollImmediately()
    }

    private fun ensureMouseWheelScrollNodeInitialized() {
        if (mouseWheelScrollingLogic == null) {
            mouseWheelScrollingLogic =
                MouseWheelScrollingLogic(
                    scrollingLogic = scrollingLogic,
                    mouseWheelScrollConfig = platformScrollConfig(),
                    onScrollStopped = ::onWheelScrollStopped,
                    density = requireDensity(),
                )
        }

        mouseWheelScrollingLogic?.startReceivingMouseWheelEvents(coroutineScope)
    }

    fun update(
        state: ScrollableState,
        //orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
        bringIntoViewSpec: BringIntoViewSpec?,
    ) {
        var shouldInvalidateSemantics = false
        if (this.enabled != enabled) { // enabled changed
            nestedScrollConnection.enabled = enabled
            scrollableContainerNode.update(enabled)
            shouldInvalidateSemantics = true
        }
        // a new fling behavior was set, change the resolved one.
        val resolvedFlingBehavior = flingBehavior ?: defaultFlingBehavior

        val resetPointerInputHandling =
            scrollingLogic.update(
                scrollableState = state,
                orientation = state.orientation,
                overscrollEffect = overscrollEffect,
                reverseDirection = reverseDirection,
                flingBehavior = resolvedFlingBehavior,
                nestedScrollDispatcher = nestedScrollDispatcher,
            )
        contentInViewNode.update(state.orientation, reverseDirection, bringIntoViewSpec)

        this.overscrollEffect = overscrollEffect
        this.flingBehavior = flingBehavior

        // update DragGestureNode
        update(
            canDrag = CanDragCalculation,
            enabled = enabled,
            interactionSource = interactionSource,
            orientationLock = null,//if (scrollingLogic.isVertical()) Vertical else Horizontal,
            shouldResetPointerInputHandling = resetPointerInputHandling,
        )

        if (shouldInvalidateSemantics) {
            clearScrollSemanticsActions()
            invalidateSemantics()
        }
    }

    override fun onAttach() {
        updateDefaultFlingBehavior()
        mouseWheelScrollingLogic?.updateDensity(requireDensity())
    }

    private fun updateDefaultFlingBehavior() {
        if (!isAttached) return
        val density = requireDensity()
        defaultFlingBehavior.updateDensity(density)
    }

    override fun onDensityChange() {
        onCancelPointerInput()
        updateDefaultFlingBehavior()
        mouseWheelScrollingLogic?.updateDensity(requireDensity())
    }

    // Key handler for Page up/down scrolling behavior.
    override fun onKeyEvent(event: KeyEvent): Boolean {
        return if (
            enabled &&
            (event.key == Key.PageDown || event.key == Key.PageUp) &&
            (event.type == KeyEventType.KeyDown) &&
            (!event.isCtrlPressed)
        ) {

            val scrollAmount: Offset =
                if (scrollingLogic.isVertical()) {
                    val viewportHeight = contentInViewNode.viewportSize.height

                    val yAmount =
                        if (event.key == Key.PageUp) {
                            viewportHeight.toFloat()
                        } else {
                            -viewportHeight.toFloat()
                        }

                    Offset(0f, yAmount)
                } else {
                    val viewportWidth = contentInViewNode.viewportSize.width

                    val xAmount =
                        if (event.key == Key.PageUp) {
                            viewportWidth.toFloat()
                        } else {
                            -viewportWidth.toFloat()
                        }

                    Offset(xAmount, 0f)
                }

            // A coroutine is launched for every individual scroll event in the
            // larger scroll gesture. If we see degradation in the future (that is,
            // a fast scroll gesture on a slow device causes UI jank [not seen up to
            // this point), we can switch to a more efficient solution where we
            // lazily launch one coroutine (with the first event) and use a Channel
            // to communicate the scroll amount to the UI thread.
            coroutineScope.launch {
                scrollingLogic.scroll(scrollPriority = MutatePriority.UserInput) {
                    scrollBy(offset = scrollAmount, source = UserInput)
                }
            }
            true
        } else {
            false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent) = false

    // Forward all PointerInputModifierNode method calls to `mmouseWheelScrollNode.pointerInputNode`
    // See explanation in `MouseWheelScrollNode.pointerInputNode`

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pointerEvent.changes.fastAny { canDrag.invoke(it.type) }) {
            super.onPointerEvent(pointerEvent, pass, bounds)
        }
        if (enabled) {
            if (pass == PointerEventPass.Initial && pointerEvent.type == PointerEventType.Scroll) {
                ensureMouseWheelScrollNodeInitialized()
            }
            mouseWheelScrollingLogic?.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (enabled && (scrollByAction == null || scrollByOffsetAction == null)) {
            setScrollSemanticsActions()
        }

        scrollByAction?.let { scrollBy(action = it) }

        scrollByOffsetAction?.let { scrollByOffset(action = it) }
    }

    private fun setScrollSemanticsActions() {
        scrollByAction = { x, y ->
            coroutineScope.launch { scrollingLogic.semanticsScrollBy(Offset(x, y)) }
            true
        }

        scrollByOffsetAction = { offset -> scrollingLogic.semanticsScrollBy(offset) }
    }

    private fun clearScrollSemanticsActions() {
        scrollByAction = null
        scrollByOffsetAction = null
    }
}

/** Contains the default values used by [scrollable] */
object ScrollableDefaults {

    /** Create and remember default [FlingBehavior] that will represent natural fling curve. */
    @Composable fun flingBehavior(): FlingBehavior = rememberPlatformDefaultFlingBehavior()

    /**
     * Returns a remembered [OverscrollEffect] created from the current value of
     * [LocalOverscrollFactory].
     *
     * This API has been deprpecated, and replaced with [rememberOverscrollEffect]
     */
    @Deprecated(
        "This API has been replaced with rememberOverscrollEffect, which queries theme provided OverscrollFactory values instead of the 'platform default' without customization.",
        replaceWith =
            ReplaceWith(
                "rememberOverscrollEffect()",
                "androidx.compose.foundation.rememberOverscrollEffect",
            ),
    )
    @Composable
    fun overscrollEffect(): OverscrollEffect {
        return rememberPlatformOverscrollEffect() ?: NoOpOverscrollEffect
    }

    private object NoOpOverscrollEffect : OverscrollEffect {
        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset,
        ): Offset = performScroll(delta)

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity,
        ) {
            performFling(velocity)
        }

        override val isInProgress: Boolean
            get() = false

        override val node: DelegatableNode
            get() = object : Modifier.Node() {}
    }

    /**
     * Calculates the final `reverseDirection` value for a scrollable component.
     *
     * This is a helper function used by [androidx.compose.foundation.scrollableArea] to determine
     * whether to reverse the direction of scroll input. The goal is to provide a "natural"
     * scrolling experience where content moves with the user's gesture, while also accounting for
     * the [layoutDirection].
     *
     * The logic is as follows:
     * 1. To achieve "natural" scrolling (content moves with the gesture), scroll deltas are
     *    inverted. This function returns `true` by default when `reverseScrolling` is `false`.
     * 2. In a Right-to-Left (`Rtl`) context with a `Horizontal` orientation, the direction is
     *    flipped an additional time to maintain the natural feel, as the content is laid out from
     *    right to left.
     *
     * @param layoutDirection current layout direction (e.g. from [LocalLayoutDirection])
     * @param orientation orientation of scroll
     * @param reverseScrolling whether scrolling direction should be reversed
     * @return `true` if scroll direction should be reversed, `false` otherwise.
     */
    fun reverseDirection(
        layoutDirection: LayoutDirection,
        orientation: Orientation,
        reverseScrolling: Boolean,
    ): Boolean {
        // A finger moves with the content, not with the viewport. Therefore,
        // always reverse once to have "natural" gesture that goes reversed to layout
        var reverseDirection = !reverseScrolling
        // But if rtl and horizontal, things move the other way around
        val isRtl = layoutDirection == LayoutDirection.Rtl
        if (isRtl && orientation != Orientation.Vertical) {
            reverseDirection = !reverseDirection
        }
        return reverseDirection
    }
}

internal interface ScrollConfig {

    /** Enables animated transition of scroll on mouse wheel events. */
    val isSmoothScrollingEnabled: Boolean
        get() = true

    fun isPreciseWheelScroll(event: PointerEvent): Boolean = false

    fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset
}

internal fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig =
    AndroidConfig(android.view.ViewConfiguration.get(requireView().context))

internal class AndroidConfig(val viewConfiguration: android.view.ViewConfiguration) : ScrollConfig {
    // 64 dp value is taken from ViewConfiguration.java, replace with better solution

    @VisibleForTesting
    internal fun Density.getVerticalScrollFactor() =
        if (Build.VERSION.SDK_INT > 26) {
            ViewConfigurationApi26Impl.getVerticalScrollFactor(viewConfiguration)
        } else {
            64.dp.toPx()
        }

    @VisibleForTesting
    internal fun Density.getHorizontalScrollFactor() =
        if (Build.VERSION.SDK_INT > 26) {
            ViewConfigurationApi26Impl.getHorizontalScrollFactor(viewConfiguration)
        } else {
            64.dp.toPx()
        }

    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        val verticalScrollFactor = -getVerticalScrollFactor()
        val horizontalScrollFactor = -getHorizontalScrollFactor()
        return event.changes
            .fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta }
            .let { Offset(it.x * horizontalScrollFactor, it.y * verticalScrollFactor) }
    }
}

@RequiresApi(26)
private object ViewConfigurationApi26Impl {
    fun getVerticalScrollFactor(viewConfiguration: android.view.ViewConfiguration): Float {
        return viewConfiguration.scaledVerticalScrollFactor
    }

    fun getHorizontalScrollFactor(viewConfiguration: android.view.ViewConfiguration): Float {
        return viewConfiguration.scaledHorizontalScrollFactor
    }
}

// TODO: provide public way to drag by mouse (especially requested for Pager)
internal val CanDragCalculation: (PointerType) -> Boolean = { type -> type != PointerType.Mouse }

internal class ScrollingLogic(
    var scrollableState: ScrollableState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior,
    private var orientation: Orientation,
    private var reverseDirection: Boolean,
    private var nestedScrollDispatcher: NestedScrollDispatcher,
    private var onScrollChangedDispatcher: OnScrollChangedDispatcher,
    private val isScrollableNodeAttached: () -> Boolean,
) : ScrollLogic {
    // specifies if this scrollable node is currently flinging
    override var isFlinging = false
        private set

    private var currentGestureOrientation: Orientation? = null

    fun resetGestureOrientation() {
        currentGestureOrientation = null
    }

    private fun getActiveOrientation(): Orientation = currentGestureOrientation ?: orientation

    fun Float.toOffset(): Offset =
        //if (getActiveOrientation() == Horizontal) Offset(this, 0f) else Offset(0f, this)
        when {
            this == 0f -> Offset.Zero
            else -> if (getActiveOrientation() == Horizontal) Offset(this, 0f) else Offset(0f, this)
        }

    fun Offset.singleAxisOffset(): Offset =
        if (getActiveOrientation() == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Offset.toFloat(): Float = if (getActiveOrientation() == Horizontal) this.x else this.y

    /**
     * Converts this offset to a single axis delta based on the derived angle from the x and y
     * deltas.
     *
     * @return Returns a single axis delta based on the angle. If the angle is mostly horizontal,
     *   and we are in a horizontal scrollable, this will return the x component. If the angle is
     *   mostly vertical, and we are in a vertical scrollable, this will return the y component.
     *   Otherwise, this will return 0. Mostly horizontal means angles smaller than
     *   [VerticalAxisThresholdAngle].
     */
    fun Offset.toSingleAxisDeltaFromAngle(): Float {
        val angle = atan2(this.y.absoluteValue, this.x.absoluteValue)
        val activeOrientation = getActiveOrientation()
        return if (angle >= VerticalAxisThresholdAngle) {
            if (activeOrientation == Vertical) this.y else 0f
        } else {
            if (activeOrientation == Horizontal) this.x else 0f
        }
    }

    fun Float.toVelocity(): Velocity =
        when {
            this == 0f -> Velocity.Zero
            else -> if (getActiveOrientation() == Horizontal) Velocity(this, 0f) else Velocity(x = 0f, this)
        }

    private fun Velocity.toFloat(): Float = if (getActiveOrientation() == Horizontal) this.x else this.y

    private fun Velocity.singleAxisVelocity(): Velocity =
        if (getActiveOrientation() == Horizontal) copy(y = 0f) else copy(x = 0f)

    private fun Velocity.update(newValue: Float): Velocity =
        if (getActiveOrientation() == Horizontal) copy(x = newValue) else copy(y = newValue)

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

    private var latestScrollSource = UserInput
    private var outerStateScope = NoOpScrollScope

    private val nestedScrollScope =
        object : NestedScrollScope {
            override fun scrollBy(offset: Offset, source: NestedScrollSource): Offset {
                return with(outerStateScope) { performScroll(offset, source) }
            }

            override fun scrollByWithOverscroll(
                offset: Offset,
                source: NestedScrollSource,
            ): Offset {
                latestScrollSource = source
                val overscroll = overscrollEffect
                return if (overscroll != null && shouldDispatchOverscroll) {
                    overscroll.applyToScroll(offset, latestScrollSource, performScrollForOverscroll)
                } else {
                    with(outerStateScope) { performScroll(offset, source) }
                }
            }
        }

    private val performScrollForOverscroll: (Offset) -> Offset = { delta ->
        with(outerStateScope) { performScroll(delta, latestScrollSource) }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun ScrollScope.performScroll(delta: Offset, source: NestedScrollSource): Offset {
        val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(delta, source)

        val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

        if (currentGestureOrientation == null) {
            val absX = scrollAvailableAfterPreScroll.x.absoluteValue
            val absY = scrollAvailableAfterPreScroll.y.absoluteValue
            if (absX > 0.001f || absY > 0.001f) {
                currentGestureOrientation = if (absX > absY) Horizontal else Vertical
                scrollableState.setOrientation(if (absX > absY) Horizontal else Vertical, changeOrientation = true)
            }
        }

        val activeOrientation = getActiveOrientation()

        val rawFloatDelta = if (activeOrientation == Horizontal) scrollAvailableAfterPreScroll.x else scrollAvailableAfterPreScroll.y
        val singleAxisDeltaForSelfScroll = if (reverseDirection) rawFloatDelta * -1f else rawFloatDelta

        val consumedFloat = scrollBy(singleAxisDeltaForSelfScroll)

        val rawConsumedFloat = if (reverseDirection) consumedFloat * -1f else consumedFloat
        val consumedBySelfScroll = if (activeOrientation == Horizontal) {
            Offset(rawConsumedFloat, 0f)
        } else {
            Offset(0f, rawConsumedFloat)
        }

        // Trigger on scroll changed callback
        onScrollChangedDispatcher.dispatchScrollDeltaInfo(consumedBySelfScroll)

        val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
        val consumedByPostScroll =
            nestedScrollDispatcher.dispatchPostScroll(
                consumedBySelfScroll,
                deltaAvailableAfterScroll,
                source,
            )
        return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
    }

    private val shouldDispatchOverscroll
        get() = scrollableState.canScrollForward || scrollableState.canScrollBackward

    override fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            dispatchRawDelta(scroll)
        }
    }

    private fun dispatchRawDelta(scroll: Offset): Offset {
        return scrollableState
            .dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
            .reverseIfNeeded()
            .toOffset()
    }

    suspend fun onScrollStopped(initialVelocity: Velocity, isMouseWheel: Boolean) {
        if (isMouseWheel && !flingBehavior.shouldBeTriggeredByMouseWheel) {
            return
        }
        val availableVelocity = initialVelocity.singleAxisVelocity()

        val performFling: suspend (Velocity) -> Velocity = { velocity ->
            val preConsumedByParent = nestedScrollDispatcher.dispatchPreFling(velocity)
            val available = velocity - preConsumedByParent

            val velocityLeft = doFlingAnimation(available)

            val consumedPost =
                nestedScrollDispatcher.dispatchPostFling((available - velocityLeft), velocityLeft)
            val totalLeft = velocityLeft - consumedPost
            velocity - totalLeft
        }

        val overscroll = overscrollEffect
        if (overscroll != null && shouldDispatchOverscroll) {
            overscroll.applyToFling(availableVelocity, performFling)
        } else {
            performFling(availableVelocity)
        }

        //clean up dynamic fields upon completion of drag and physics actions
        resetGestureOrientation()
    }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        isFlinging = true

        try {
            scroll(scrollPriority = MutatePriority.Default) {
                val nestedScrollScope = this
                val reverseScope =
                    object : ScrollScope {
                        override fun scrollBy(pixels: Float): Float {

                            // Fling has hit the bounds or node left composition,
                            // cancel it to allow continuation. This will conclude this node's
                            // fling,
                            // allowing the onPostFling signal to be called
                            // with the leftover velocity from the fling animation. Any nested
                            // scroll
                            // node above will be able to pick up the left over velocity and
                            // continue
                            // the fling.
                            if (
                                pixels.absoluteValue != 0.0f && !isScrollableNodeAttached.invoke()
                            ) {
                                throw FlingCancellationException()
                            }

                            return nestedScrollScope
                                .scrollByWithOverscroll(
                                    offset = pixels.toOffset().reverseIfNeeded(),
                                    source = SideEffect,
                                )
                                .toFloat()
                                .reverseIfNeeded()
                        }
                    }
                with(reverseScope) {
                    with(flingBehavior) {
                        result =
                            result.update(
                                performFling(available.toFloat().reverseIfNeeded())
                                    .reverseIfNeeded()
                            )
                    }
                }
            }
        } finally {
            isFlinging = false
        }

        return result
    }

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.isScrollInProgress || overscrollEffect?.isInProgress ?: false
    }

    /** Opens a scrolling session with nested scrolling and overscroll support. */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend NestedScrollScope.() -> Unit,
    ) {
        scrollableState.scroll(scrollPriority) {
            outerStateScope = this
            block.invoke(nestedScrollScope)
        }
    }

    /** @return true if the pointer input should be reset */
    fun update(
        scrollableState: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior,
        nestedScrollDispatcher: NestedScrollDispatcher,
    ): Boolean {
        var resetPointerInputHandling = false
        if (this.scrollableState != scrollableState) {
            this.scrollableState = scrollableState
            resetPointerInputHandling = true
        }
        this.overscrollEffect = overscrollEffect
        if (this.orientation != scrollableState.orientation) {
            this.orientation = scrollableState.orientation
            resetPointerInputHandling = true
        }
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }
        this.flingBehavior = flingBehavior
        this.nestedScrollDispatcher = nestedScrollDispatcher
        return resetPointerInputHandling
    }

    fun isVertical(): Boolean = getActiveOrientation() == Vertical
}

private val NoOpScrollScope: ScrollScope =
    object : ScrollScope {
        override fun scrollBy(pixels: Float): Float = pixels
    }

internal class ScrollableNestedScrollConnection(
    val scrollingLogic: ScrollLogic,
    var enabled: Boolean,
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (enabled) {
            scrollingLogic.performRawScroll(available)
        } else {
            Offset.Zero
        }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return if (enabled) {
            val velocityLeft =
                if (scrollingLogic.isFlinging) {
                    Velocity.Zero
                } else {
                    scrollingLogic.doFlingAnimation(available)
                }
            available - velocityLeft
        } else {
            Velocity.Zero
        }
    }
}

/** Interface to allow re-use across Scrollable and Scrollable2D. */
internal interface ScrollLogic {
    val isFlinging: Boolean

    fun performRawScroll(scroll: Offset): Offset

    suspend fun doFlingAnimation(available: Velocity): Velocity
}

internal interface ScrollableDefaultFlingBehavior : FlingBehavior {
    fun updateDensity(density: Density) = Unit
}

private val FlingBehavior.shouldBeTriggeredByMouseWheel
    get() = this !is ScrollableDefaultFlingBehavior

/**
 * This method returns [ScrollableDefaultFlingBehavior] whose density will be managed by the
 * [ScrollableElement] because it's not created inside [Composable] context. This is different from
 * [rememberPlatformDefaultFlingBehavior] which creates [FlingBehavior] whose density depends on
 * [LocalDensity] and is automatically resolved.
 */
internal fun platformScrollableDefaultFlingBehavior(): ScrollableDefaultFlingBehavior {
    return DefaultFlingBehavior(splineBasedDecay(UnityDensity))
}

@Composable
internal fun rememberPlatformDefaultFlingBehavior(): FlingBehavior {
    val flingSpec = rememberSplineBasedDecay<Float>()
    return remember(flingSpec) { DefaultFlingBehavior(flingSpec) }
}

internal class DefaultFlingBehavior(
    private var flingDecay: DecayAnimationSpec<Float>,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,
) : ScrollableDefaultFlingBehavior {

    // For Testing
    var lastAnimationCycleCount = 0

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        lastAnimationCycleCount = 0
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                val animationState =
                    AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
                try {
                    animationState.animateDecay(flingDecay) {
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity
                        // avoid rounding errors and stop if anything is unconsumed
                        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                        lastAnimationCycleCount++
                    }
                } catch (exception: CancellationException) {
                    velocityLeft = animationState.velocity
                }
                velocityLeft
            } else {
                initialVelocity
            }
        }
    }

    override fun updateDensity(density: Density) {
        flingDecay = splineBasedDecay(density)
    }
}

private const val DefaultScrollMotionDurationScaleFactor = 1f
internal val DefaultScrollMotionDurationScale =
    object : MotionDurationScale {
        override val scaleFactor: Float
            get() = DefaultScrollMotionDurationScaleFactor
    }

/**
 * (b/311181532): This could not be flattened so we moved it to TraversableNode, but ideally
 * ScrollabeNode should be the one to be travesable.
 */
internal class ScrollableContainerNode(enabled: Boolean) : Modifier.Node(), TraversableNode {
    override val traverseKey: Any = TraverseKey

    var enabled: Boolean = enabled
        private set

    companion object TraverseKey

    fun update(enabled: Boolean) {
        this.enabled = enabled
    }
}

internal val UnityDensity =
    object : Density {
        override val density: Float
            get() = 1f

        override val fontScale: Float
            get() = 1f
    }

internal interface NestedScrollScope {
    fun scrollBy(offset: Offset, source: NestedScrollSource): Offset

    fun scrollByWithOverscroll(offset: Offset, source: NestedScrollSource): Offset
}

/**
 * Scroll deltas originating from the semantics system. Should be dispatched as an animation driven
 * event.
 */
private suspend fun ScrollingLogic.semanticsScrollBy(offset: Offset): Offset {
    var previousValue = 0f
    scroll(scrollPriority = MutatePriority.Default) {
        animate(0f, offset.toFloat()) { currentValue, _ ->
            val delta = currentValue - previousValue
            val consumed =
                scrollBy(offset = delta.reverseIfNeeded().toOffset(), source = UserInput)
                    .toFloat()
                    .reverseIfNeeded()
            previousValue += consumed
        }
    }
    return previousValue.toOffset()
}

internal class FlingCancellationException :
    PlatformOptimizedCancellationException("The fling animation was cancelled")

internal interface OnScrollChangedDispatcher {
    fun dispatchScrollDeltaInfo(delta: Offset)
}

private const val VerticalAxisThresholdAngle = PI / 4

private val Velocity.magnitude
    get() = sqrt(x.pow(2) + y.pow(2))
private val Velocity.angle
    get() = atan2(x = x, y = y)
