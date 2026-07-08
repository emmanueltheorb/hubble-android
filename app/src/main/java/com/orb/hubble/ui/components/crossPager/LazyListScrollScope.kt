package com.orb.hubble.ui.components.crossPager

import androidx.compose.ui.util.fastFirstOrNull

/**
 * An implementation of [LazyLayoutScrollScope] that can be used with LazyLists. Please refer to the
 * sample to learn how to use this API.
 *
 * @param state The [LazyListState] associated with the layout where this custom scroll should be
 *   performed.
 * @param scrollScope The base [ScrollScope] where the scroll session was created.
 * @return An implementation of [LazyLayoutScrollScope] that works with [LazyRow] and [LazyColumn].
 * @sample androidx.compose.foundation.samples.LazyListCustomScrollUsingLazyLayoutScrollScopeSample
 */
fun LazyLayoutScrollScope(state: LazyListState, scrollScope: ScrollScope): LazyLayoutScrollScope {

    return object : LazyLayoutScrollScope, ScrollScope by scrollScope {
        override val firstVisibleItemIndex: Int
            get() = state.firstVisibleItemIndex

        override val firstVisibleItemScrollOffset: Int
            get() = state.firstVisibleItemScrollOffset

        override val lastVisibleItemIndex: Int
            get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

        override val itemCount: Int
            get() = state.layoutInfo.totalItemsCount

        override fun snapToItem(index: Int, offset: Int) {
            state.snapToItemIndexInternal(index, offset, forceRemeasure = true)
        }

        override fun calculateDistanceTo(targetIndex: Int, targetOffset: Int): Int {
            val layoutInfo = state.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return 0
            return if (targetIndex !in firstVisibleItemIndex..lastVisibleItemIndex) {
                val averageSize = layoutInfo.visibleItemsAverageSize()
                val indexesDiff = targetIndex - firstVisibleItemIndex
                (averageSize * indexesDiff) - firstVisibleItemScrollOffset
            } else {
                val visibleItem =
                    layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }
                visibleItem?.offset ?: 0
            } + targetOffset
        }
    }
}
