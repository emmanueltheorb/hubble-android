package com.orb.hubble.ui.components.crossPager

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

interface ScrollableState {

    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend ScrollScope.() -> Unit,
    )

    fun dispatchRawDelta(delta: Float): Float

    val isScrollInProgress: Boolean

    val orientation: Orientation

    fun setOrientation(newOrientation: Orientation, changeOrientation: Boolean)

    val canScrollForward: Boolean
        get() = true

    val canScrollBackward: Boolean
        get() = true

    @get:Suppress("GetterSetterNames")
    val lastScrolledForward: Boolean
        get() = false

    @get:Suppress("GetterSetterNames")
    val lastScrolledBackward: Boolean
        get() = false

    val scrollIndicatorState: ScrollIndicatorState?
        get() = null
}

fun ScrollableState(consumeScrollDelta: (Float) -> Float): ScrollableState {
    return DefaultScrollableState(consumeScrollDelta)
}

@Composable
fun rememberScrollableState(consumeScrollDelta: (Float) -> Float): ScrollableState {
    val lambdaState = rememberUpdatedState(consumeScrollDelta)
    return remember { ScrollableState { lambdaState.value.invoke(it) } }
}

interface ScrollScope {
    fun scrollBy(pixels: Float): Float
}

private class DefaultScrollableState(val onDelta: (Float) -> Float) : ScrollableState {

    private val scrollScope: ScrollScope =
        object : ScrollScope {
            override fun scrollBy(pixels: Float): Float {
                if (pixels.isNaN()) return 0f
                val delta = onDelta(pixels)
                isLastScrollForwardState.value = delta > 0
                isLastScrollBackwardState.value = delta < 0
                return delta
            }
        }

    private val scrollMutex = MutatorMutex()

    private val orientationState = mutableStateOf(Orientation.Vertical)

    private val isScrollingState = mutableStateOf(false)
    private val isLastScrollForwardState = mutableStateOf(false)
    private val isLastScrollBackwardState = mutableStateOf(false)


    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ): Unit = coroutineScope {
        scrollMutex.mutateWith(scrollScope, scrollPriority) {
            isScrollingState.value = true
            try {
                block()
            } finally {
                isScrollingState.value = false
            }
        }
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return onDelta(delta)
    }

    override val orientation: Orientation
        get() = orientationState.value

    override fun setOrientation(
        newOrientation: Orientation,
        changeOrientation: Boolean
    ) {
        if (changeOrientation) {
            if (orientationState.value != newOrientation) {
                orientationState.value = newOrientation
            }
        }
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value

    override val lastScrolledForward: Boolean
        get() = isLastScrollForwardState.value

    override val lastScrolledBackward: Boolean
        get() = isLastScrollBackwardState.value
}