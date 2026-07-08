package com.orb.hubble.ui.components.crossPager

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.platform.InspectorInfo

/**
 * Calls [onPositioned] whenever the bounds of the currently-focused area changes. If a child of
 * this node has focus, [onPositioned] will be called immediately with a non-null
 * [LayoutCoordinates] that can be queried for the focused bounds, and again every time the focused
 * child changes or is repositioned. When a child loses focus, [onPositioned] will be passed `null`.
 *
 * When an event occurs, it is bubbled up from the focusable node, so the nearest parent gets the
 * event first, and then its parent, etc.
 *
 * Note that there may be some cases where the focused bounds change but the callback is _not_
 * invoked, but the last [LayoutCoordinates] will always return the most up-to-date bounds.
 */
fun Modifier.onFocusedBoundsChanged(onPositioned: (LayoutCoordinates?) -> Unit): Modifier =
    this then FocusedBoundsObserverElement(onPositioned)

private class FocusedBoundsObserverElement(val onPositioned: (LayoutCoordinates?) -> Unit) :
    ModifierNodeElement<FocusedBoundsObserverNode>() {
    override fun create(): FocusedBoundsObserverNode = FocusedBoundsObserverNode(onPositioned)

    override fun update(node: FocusedBoundsObserverNode) {
        node.onPositioned = onPositioned
    }

    override fun hashCode(): Int = onPositioned.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? FocusedBoundsObserverElement ?: return false
        return onPositioned === otherModifier.onPositioned
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onFocusedBoundsChanged"
        properties["onPositioned"] = onPositioned
    }
}

internal class FocusedBoundsObserverNode(var onPositioned: (LayoutCoordinates?) -> Unit) :
    Modifier.Node(), TraversableNode {

    override val traverseKey: Any = TraverseKey

    /** Called when a child gains/loses focus or is focused and changes position. */
    fun onFocusBoundsChanged(focusedBounds: LayoutCoordinates?) {
        onPositioned(focusedBounds)
        findNearestAncestor()?.onFocusBoundsChanged(focusedBounds)
    }

    companion object TraverseKey
}
