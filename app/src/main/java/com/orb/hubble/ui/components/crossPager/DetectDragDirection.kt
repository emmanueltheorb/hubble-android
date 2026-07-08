package com.orb.hubble.ui.components.crossPager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

suspend fun PointerInputScope.detectCrossPagerDrag(
    onDragStart: () -> Unit,
    onOrientationLocked: (Orientation) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    awaitEachGesture {
        // PHASE 1: The user touches the screen
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart()

        // PHASE 2: Wait for them to move their finger past the touch slop
        var overSlopOffset = Offset.Zero
        val slopChange = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
            change.consume()
            overSlopOffset = overSlop // The amount they dragged PAST the slop threshold
        }

        // If slopChange is not null, they successfully broke the slop before lifting their finger
        if (slopChange != null) {

            // AT THIS EXACT MOMENT: Evaluate and LOCK the orientation
            val lockedOrientation = if (abs(overSlopOffset.x) > abs(overSlopOffset.y)) {
                Orientation.Horizontal
            } else {
                Orientation.Vertical
            }

            // Tell your state to lock this orientation until the gesture ends
            onOrientationLocked(lockedOrientation)

            // Immediately report the initial overshoot amount to your scrollBy
            onDrag(overSlopOffset)

            // PHASE 3: Stop evaluating! just stream the raw drag data until they lift their finger.
            val dragSuccessful = drag(slopChange.id) { change ->
                val delta = change.positionChange()
                onDrag(delta) // This goes to your scrollBy
                change.consume()
            }

            // PHASE 4: The user lifted their finger
            if (dragSuccessful) {
                onDragEnd() // Trigger fling logic here
            } else {
                onDragEnd() // Gesture was cancelled
            }
        } else {
            // User touched and let go without moving past the touch slop (e.g., a tap)
            onDragEnd()
        }
    }
}