package com.orb.hubble.ui.components.crossPager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.scrollableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import com.orb.hubble.ui.components.crossPager.ScrollableState
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Pager(
    modifier: Modifier,
    state: CrossPagerState,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    //orientation: Orientation,
    flingBehavior: TargetedFlingBehavior,
    userScrollEnabled: Boolean,
    overscrollEffect: OverscrollEffect?,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    pageSize: PageSize,
    pageNestedScrollConnection: NestedScrollConnection,
    key: ((index: Int) -> Any)?,
    horizontalAlignment: Alignment.Horizontal,
    verticalAlignment: Alignment.Vertical,
    snapPosition: SnapPosition,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    requirePrecondition(beyondViewportPageCount >= 0) {
        "beyondViewportPageCount should be greater than or equal to 0, " +
                "you selected $beyondViewportPageCount"
    }

    val pagerItemProvider =
        rememberPagerItemProviderLambda(state = state, pageContent = pageContent, key = key) {
            state.pageCount
        }

    val currentOrientation = state.orientation

    val coroutineScope = rememberCoroutineScope()

    val measurePolicy =
        rememberPagerMeasurePolicy(
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            orientation = currentOrientation,
            beyondViewportPageCount = beyondViewportPageCount,
            pageSpacing = pageSpacing,
            pageSize = pageSize,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment,
            itemProviderLambda = pagerItemProvider,
            snapPosition = snapPosition,
            coroutineScope = coroutineScope,
            pageCount = { state.pageCount },
        )

    val semanticState = rememberPagerSemanticState(state, currentOrientation == Orientation.Vertical)

    val resolvedFlingBehavior =
        remember(state, flingBehavior) { PagerWrapperFlingBehavior(flingBehavior, state) }

    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
    val pagerBringIntoViewSpec =
        remember(state, defaultBringIntoViewSpec) {
            PagerBringIntoViewSpec(state, defaultBringIntoViewSpec)
        }

    val beyondBoundsModifier =
        if (userScrollEnabled) {
            Modifier.lazyLayoutBeyondBoundsModifier(
                state =
                    rememberPagerBeyondBoundsState(
                        state = state,
                        beyondViewportPageCount = beyondViewportPageCount,
                    ),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                orientation = currentOrientation,
            )
        } else {
            Modifier
        }

    LazyLayout(
        modifier =
            modifier
                .then(state.remeasurementModifier)
                .then(state.awaitLayoutModifier)
                .lazyLayoutSemantics(
                    itemProviderLambda = pagerItemProvider,
                    state = semanticState,
                    orientation = currentOrientation,
                    userScrollEnabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                )
                .pagerSemantics(
                    state,
                    //currentOrientation == Orientation.Vertical,
                    coroutineScope,
                    userScrollEnabled,
                )
                .then(beyondBoundsModifier)
                .scrollableArea(
                    state = state,
                    enabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                    flingBehavior = resolvedFlingBehavior,
                    interactionSource = state.internalInteractionSource,
                    overscrollEffect = overscrollEffect,
                    bringIntoViewSpec = pagerBringIntoViewSpec
                )
                .dragDirectionDetector(state)
                .nestedScroll(pageNestedScrollConnection),
        measurePolicy = measurePolicy,
        prefetchState = state.prefetchState,
        itemProvider = pagerItemProvider,
    )
}

internal class PagerLazyLayoutItemProvider(
    private val state: CrossPagerState,
    private val intervalContent: LazyLayoutIntervalContent<PagerIntervalContent>,
    private val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyLayoutItemProvider {

    private val pagerScopeImpl = PagerScopeImpl

    override val itemCount: Int
        get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedPages) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(pagerScopeImpl, localIndex)
            }
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PagerLazyLayoutItemProvider) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}

private class PagerLayoutIntervalContent(
    val pageContent: @Composable PagerScope.(page: Int) -> Unit,
    val key: ((index: Int) -> Any)?,
    val pageCount: Int,
) : LazyLayoutIntervalContent<PagerIntervalContent>() {
    override val intervals: IntervalList<PagerIntervalContent> =
        MutableIntervalList<PagerIntervalContent>().apply {
            addInterval(pageCount, PagerIntervalContent(key = key, item = pageContent))
        }
}

internal class PagerIntervalContent(
    override val key: ((page: Int) -> Any)?,
    val item: @Composable PagerScope.(page: Int) -> Unit,
) : LazyLayoutIntervalContent.Interval

@Composable
private fun rememberPagerItemProviderLambda(
    state: CrossPagerState,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
    key: ((index: Int) -> Any)?,
    pageCount: () -> Int,
): () -> PagerLazyLayoutItemProvider {
    val latestContent = rememberUpdatedState(pageContent)
    val latestKey = rememberUpdatedState(key)
    return remember(state, latestContent, latestKey, pageCount) {
        val intervalContentState =
            derivedStateOf(referentialEqualityPolicy()) {
                PagerLayoutIntervalContent(latestContent.value, latestKey.value, pageCount())
            }
        val itemProviderState =
            derivedStateOf(referentialEqualityPolicy()) {
                val intervalContent = intervalContentState.value
                val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
                PagerLazyLayoutItemProvider(
                    state = state,
                    intervalContent = intervalContent,
                    keyIndexMap = map,
                )
            }
        itemProviderState::value
    }
}

/** A modifier to detect up and down events in a Pager. */
private fun Modifier.dragDirectionDetector(state: CrossPagerState) =
    this then
            Modifier.pointerInput(state) {
                coroutineScope {
                    awaitEachGesture {
                        val downEvent =
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var upEventOrCancellation: PointerInputChange? = null
                        state.upDownDifference = Offset.Zero // Reset
                        while (upEventOrCancellation == null) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            if (event.changes.fastAll { it.changedToUp() }) {
                                // All pointers are up
                                upEventOrCancellation = event.changes[0]
                            }
                        }

                        state.upDownDifference = upEventOrCancellation.position - downEvent.position
                    }
                }
            }

private class PagerBringIntoViewSpec(
    val pagerState: CrossPagerState,
    val defaultBringIntoViewSpec: BringIntoViewSpec,
) : BringIntoViewSpec {

    /**
     * [calculateScrollDistance] for Pager behaves differently than in a normal list. We must always
     * respect the snapped pages over bringing a child into view. The logic here will behave like
     * so:
     * 1) If there's an ongoing request from the default bring into view spec, override the value to
     *    make it land on the closest page to the requested offset.
     * 2) If there's no ongoing request it means that either we moved enough to fulfill the
     *    previously on going request or we didn't need move at all. 2a) If we didn't move at all we
     *    do nothing (pagerState.firstVisiblePageOffset == 0) 2b) If we fulfilled the default
     *    request, settle to the next page in the direction where we were scrolling before. We use
     *    firstVisiblePage as anchor, but the goal is to keep the pager snapped.
     */
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val proposedOffsetMove =
            defaultBringIntoViewSpec.calculateScrollDistance(offset, size, containerSize)

        val isItemOutView =
            if (offset > 0) {
                offset + size > containerSize
            } else {
                offset + size <= 0
            }

        val finalOffset =
            if (proposedOffsetMove.absoluteValue != 0.0f && isItemOutView) {
                overrideProposedOffsetMove(proposedOffsetMove)
            } else {
                // if there's no info from the default behavior, or if we already satisfied their
                // request.
                if (pagerState.firstVisiblePageOffset.absoluteValue < 1e-6) {
                    // do nothing, we're settled
                    0f
                } else {
                    // move one page forward or backward, whilst making sure we don't move out of
                    // bounds
                    // again.
                    val reversedFirstPageScroll = pagerState.firstVisiblePageOffset * -1f
                    if (pagerState.lastScrolledForward) {
                        reversedFirstPageScroll + pagerState.pageSizeWithSpacing
                    } else {
                        reversedFirstPageScroll
                    }
                        .coerceIn(-containerSize, containerSize)
                    // moving the pager outside of container size bounds will make the focused item
                    // disappear so we're limiting how much we can scroll so the page won't move too
                    // much.
                }
            }
        return finalOffset
    }

    private fun overrideProposedOffsetMove(proposedOffsetMove: Float): Float {
        var correctedOffset = pagerState.firstVisiblePageOffset.toFloat() * -1

        // if moving forward, start from the first visible page, move as many pages as proposed.
        while (proposedOffsetMove > 0.0f && correctedOffset < proposedOffsetMove) {
            correctedOffset += pagerState.pageSizeWithSpacing
        }

        // if moving backwards, start from the first visible page, move as many pages as proposed.
        while (proposedOffsetMove < 0.0f && correctedOffset > proposedOffsetMove) {
            correctedOffset -= pagerState.pageSizeWithSpacing
        }
        return correctedOffset
    }
}

/** Wraps [snapFlingBehavior] to give out information about target page coming from flings. */
private class PagerWrapperFlingBehavior(
    val originalFlingBehavior: TargetedFlingBehavior,
    val pagerState: CrossPagerState,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val scope: ScrollScope = this
        val resultVelocity =
            with(originalFlingBehavior) {
                performFling(initialVelocity) { remainingScrollOffset ->
                    val flingPageDisplacement =
                        if (pagerState.pageSizeWithSpacing != 0) {
                            remainingScrollOffset / (pagerState.pageSizeWithSpacing)
                        } else {
                            0f
                        }
                    val targetPage = flingPageDisplacement.roundToInt() + pagerState.currentPage
                    with(pagerState) { scope.updateTargetPage(targetPage) }
                }
            }

        // fling finished, correct snapping for rounding
        if (
            pagerState.currentPageOffsetFraction != 0.0f &&
            pagerState.currentPageOffsetFraction.absoluteValue < 1e-3
        ) {
            pagerState.requestScrollToPage(pagerState.currentPage)
        } else {
            pagerState.currentPageOffsetFraction
        }

        return resultVelocity
    }
}
