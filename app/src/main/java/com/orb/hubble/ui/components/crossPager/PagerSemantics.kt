package com.orb.hubble.ui.components.crossPager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberPagerSemanticState(
    state: CrossPagerState,
    isVertical: Boolean,
): LazyLayoutSemanticState {
    return remember(state, isVertical) { LazyLayoutSemanticState(state, isVertical) }
}