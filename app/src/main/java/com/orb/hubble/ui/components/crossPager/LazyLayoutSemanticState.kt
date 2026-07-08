package com.orb.hubble.ui.components.crossPager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.semantics.CollectionInfo

internal fun LazyLayoutSemanticState(
    state: CrossPagerState,
    isVertical: Boolean,
): LazyLayoutSemanticState =
    object : LazyLayoutSemanticState {
        override val scrollOffset: Float
            get() = state.currentAbsoluteScrollOffset().toFloat()

        override val maxScrollOffset: Float
            get() = state.layoutInfo.calculateNewMaxScrollOffset(state.pageCount).toFloat()

        override suspend fun scrollToItem(index: Int) {
            state.scrollToPage(index)
        }

        override fun collectionInfo(): CollectionInfo =
            if (isVertical) {
                CollectionInfo(rowCount = state.pageCount, columnCount = 1)
            } else {
                CollectionInfo(rowCount = 1, columnCount = state.pageCount)
            }

        override val viewport: Int
            get() =
                if (state.layoutInfo.orientation == Orientation.Vertical) {
                    state.layoutInfo.viewportSize.height
                } else {
                    state.layoutInfo.viewportSize.width
                }

        override val contentPadding: Int
            get() = state.layoutInfo.beforeContentPadding + state.layoutInfo.afterContentPadding
    }
