package com.orb.hubble.ui.components.crossPager

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.trace
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberPagerMeasurePolicy(
    itemProviderLambda: () -> PagerLazyLayoutItemProvider,
    state: CrossPagerState,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    orientation: Orientation,
    beyondViewportPageCount: Int,
    pageSpacing: Dp,
    pageSize: PageSize,
    horizontalAlignment: Alignment.Horizontal?,
    verticalAlignment: Alignment.Vertical?,
    snapPosition: SnapPosition,
    coroutineScope: CoroutineScope,
    pageCount: () -> Int,
) =
    remember(
        state,
        contentPadding,
        reverseLayout,
        orientation,
        horizontalAlignment,
        verticalAlignment,
        pageSpacing,
        pageSize,
        snapPosition,
        pageCount,
        beyondViewportPageCount,
        coroutineScope,
    ) {
        LazyLayoutMeasurePolicy { containerConstraints ->
            state.measurementScopeInvalidator.attachToScope()
            val isVertical = state.orientation == Orientation.Vertical
            checkScrollableContainerConstraints(
                containerConstraints,
                if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            )

            // resolve content paddings
            val startPadding =
                if (isVertical) {
                    contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
                } else {
                    // in horizontal configuration, padding is reversed by placeRelative
                    contentPadding.calculateStartPadding(layoutDirection).roundToPx()
                }

            val endPadding =
                if (isVertical) {
                    contentPadding.calculateRightPadding(layoutDirection).roundToPx()
                } else {
                    // in horizontal configuration, padding is reversed by placeRelative
                    contentPadding.calculateEndPadding(layoutDirection).roundToPx()
                }
            val topPadding = contentPadding.calculateTopPadding().roundToPx()
            val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
            val totalVerticalPadding = topPadding + bottomPadding
            val totalHorizontalPadding = startPadding + endPadding
            val totalMainAxisPadding =
                if (isVertical) totalVerticalPadding else totalHorizontalPadding
            val beforeContentPadding =
                when {
                    isVertical && !reverseLayout -> topPadding
                    isVertical && reverseLayout -> bottomPadding
                    !isVertical && !reverseLayout -> startPadding
                    else -> endPadding // !isVertical && reverseLayout
                }
            val afterContentPadding = totalMainAxisPadding - beforeContentPadding
            val contentConstraints =
                containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

            state.density = this

            val spaceBetweenPages = pageSpacing.roundToPx()

            // can be negative if the content padding is larger than the max size from constraints
            val mainAxisAvailableSize =
                if (isVertical) {
                    containerConstraints.maxHeight - totalVerticalPadding
                } else {
                    containerConstraints.maxWidth - totalHorizontalPadding
                }
            val visualItemOffset =
                if (!reverseLayout || mainAxisAvailableSize > 0) {
                    IntOffset(startPadding, topPadding)
                } else {
                    // When layout is reversed and paddings together take >100% of the available
                    // space,
                    // layout size is coerced to 0 when positioning. To take that space into
                    // account,
                    // we offset start padding by negative space between paddings.
                    IntOffset(
                        if (isVertical) startPadding else startPadding + mainAxisAvailableSize,
                        if (isVertical) topPadding + mainAxisAvailableSize else topPadding,
                    )
                }

            val pageAvailableSize =
                with(pageSize) {
                    calculateMainAxisPageSize(mainAxisAvailableSize, spaceBetweenPages)
                        .coerceAtLeast(0)
                }

            state.premeasureConstraints =
                Constraints(
                    maxWidth =
                        if (state.orientation == Orientation.Vertical) {
                            contentConstraints.maxWidth
                        } else {
                            pageAvailableSize
                        },
                    maxHeight =
                        if (state.orientation != Orientation.Vertical) {
                            contentConstraints.maxHeight
                        } else {
                            pageAvailableSize
                        },
                )
            val itemProvider = itemProviderLambda()

            val currentPage: Int
            val currentPageOffset: Int
            val layoutSize = mainAxisAvailableSize + beforeContentPadding + afterContentPadding

            Snapshot.withoutReadObservation {
                currentPage = state.matchScrollPositionWithKey(itemProvider, state.currentPage)
                currentPageOffset =
                    snapPosition.currentPageOffset(
                        layoutSize,
                        pageAvailableSize,
                        spaceBetweenPages,
                        beforeContentPadding,
                        afterContentPadding,
                        state.currentPage,
                        state.currentPageOffsetFraction,
                        state.pageCount,
                    )
            }

            val pinnedPages =
                itemProvider.calculateLazyLayoutPinnedIndices(
                    pinnedItemList = state.pinnedPages,
                    beyondBoundsInfo = state.beyondBoundsInfo,
                )

            val placeablesCache = mutableIntObjectMapOf<List<Placeable>>()

            // todo: wrap with snapshot when b/341782245 is resolved
            val measureResult =
                measurePager(
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    constraints = contentConstraints,
                    pageCount = pageCount(),
                    spaceBetweenPages = spaceBetweenPages,
                    mainAxisAvailableSize = mainAxisAvailableSize,
                    visualPageOffset = visualItemOffset,
                    pageAvailableSize = pageAvailableSize,
                    beyondViewportPageCount = beyondViewportPageCount,
                    orientation = state.orientation,
                    currentPage = currentPage,
                    currentPageOffset = currentPageOffset,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                    pagerItemProvider = itemProvider,
                    reverseLayout = reverseLayout,
                    pinnedPages = pinnedPages,
                    snapPosition = snapPosition,
                    placementScopeInvalidator = state.placementScopeInvalidator,
                    coroutineScope = coroutineScope,
                    placeablesCache = placeablesCache,
                    density = this,
                    layout = { width, height, placement ->
                        layout(
                            containerConstraints.constrainWidth(width + totalHorizontalPadding),
                            containerConstraints.constrainHeight(height + totalVerticalPadding),
                            emptyMap(),
                            placement,
                        )
                    },
                )
            state.applyMeasureResult(measureResult, isLookingAhead = isLookingAhead)
            // apply keep around after updating the strategy with measure result.
            keepAroundItems(
                cacheWindowLogic = state.cacheWindowLogic,
                visiblePagesList = measureResult.visiblePagesInfo,
            )
            measureResult
        }
    }

@OptIn(ExperimentalFoundationApi::class)
private fun LazyLayoutMeasureScope.keepAroundItems(
    cacheWindowLogic: CacheWindowLogic,
    visiblePagesList: List<MeasuredPage>, //was List<PageInfo>
) {
    trace("compose:pager:cache_window:keepAroundItems") {
        // only run if window and new layout info is available
        if (cacheWindowLogic.hasValidBounds() && visiblePagesList.isNotEmpty()) {
            val firstVisiblePageIndex = visiblePagesList.first().index
            val lastVisiblePageIndex = visiblePagesList.last().index

            debugLog { "Keep Around First Visible Page Index: $firstVisiblePageIndex" }
            debugLog { "Keep Around Last Visible Page Index: $firstVisiblePageIndex" }
            debugLog { "Prefetch Window Start Line: ${cacheWindowLogic.prefetchWindowStartLine}" }
            debugLog { "Prefetch Window End Line: ${cacheWindowLogic.prefetchWindowEndLine}" }
            // we must send a message in case of changing directions for items
            // that were keep around and become prefetch forward
            for (item in cacheWindowLogic.prefetchWindowStartLine..<firstVisiblePageIndex) {
                compose(item)
            }

            for (item in (lastVisiblePageIndex + 1)..cacheWindowLogic.prefetchWindowEndLine) {
                compose(item)
            }
        }
    }
}

private const val DebugEnabled = false

private inline fun debugLog(generateMsg: () -> String) {
    if (DebugEnabled) {
        println("Pager Measure Policy: ${generateMsg()}")
    }
}

internal fun SnapPosition.currentPageOffset(
    layoutSize: Int,
    pageSize: Int,
    spaceBetweenPages: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int,
): Int {
    val snapOffset =
        position(
            layoutSize,
            pageSize,
            beforeContentPadding,
            afterContentPadding,
            currentPage,
            pageCount,
        )

    return (snapOffset - currentPageOffsetFraction * (pageSize + spaceBetweenPages)).roundToInt()
}