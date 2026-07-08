package com.orb.hubble.ui.components.crossPager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberPagerBeyondBoundsState(
    state: CrossPagerState,
    beyondViewportPageCount: Int,
): LazyLayoutBeyondBoundsState {
    return remember(state, beyondViewportPageCount) {
        PagerBeyondBoundsState(state, beyondViewportPageCount)
    }
}

internal class PagerBeyondBoundsState(
    private val state: CrossPagerState,
    private val beyondViewportPageCount: Int,
) : LazyLayoutBeyondBoundsState {

    override val itemCount: Int
        get() = state.pageCount

    override val hasVisibleItems: Boolean
        get() = state.layoutInfo.visiblePagesInfo.isNotEmpty()

    override val firstPlacedIndex: Int
        get() = maxOf(0, state.firstVisiblePage - beyondViewportPageCount)

    override val lastPlacedIndex: Int
        get() =
            minOf(
                itemCount - 1,
                state.layoutInfo.visiblePagesInfo.last().index + beyondViewportPageCount,
            )

    override fun itemsPerViewport(): Int {
        val visibleItemCount = state.layoutInfo.visiblePagesInfo.size
        if (visibleItemCount == 0) return 0
        val viewportSize = state.layoutInfo.mainAxisViewportSize
        val averageItemSize = state.layoutInfo.pageSize + state.layoutInfo.pageSpacing
        if (averageItemSize == 0) return 1
        return (viewportSize / averageItemSize).coerceAtLeast(1)
    }
}