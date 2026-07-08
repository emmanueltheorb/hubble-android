package com.orb.hubble.ui.components.crossPager

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Velocity

@Stable
interface TargetedFlingBehavior : FlingBehavior {

    /**
     * Perform settling via fling animation with given velocity and suspend until fling has
     * finished. Use [onRemainingDistanceUpdated] to report the status of the ongoing fling
     * animation and the remaining amount of scroll offset.
     *
     * This functions is called with [ScrollScope] to drive the state change of the
     * [androidx.compose.foundation.gestures.ScrollableState] via [ScrollScope.scrollBy].
     *
     * This function must return the correct velocity left after it is finished flinging in order to
     * guarantee proper nested scroll support.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     *   [androidx.compose.foundation.gestures.scrollable] that invoked this method.
     * @param onRemainingDistanceUpdated a lambda that will be called anytime the distance to the
     *   settling offset is updated. The settling offset in pixels is passed to this lambda an it
     *   represents the final offset where this fling will stop and may change depending on the
     *   snapping animation progression.
     * @return remaining velocity after fling operation has ended
     */
    suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onRemainingDistanceUpdated: (Float) -> Unit,
    ): Float

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float =
        performFling(initialVelocity, NoOnReport)
}

private val NoOnReport: (Float) -> Unit = {}