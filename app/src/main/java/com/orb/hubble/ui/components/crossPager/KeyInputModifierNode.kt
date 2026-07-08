package com.orb.hubble.ui.components.crossPager

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.node.DelegatableNode

/**
 * Implement this interface to create a [Modifier.Node] that can intercept hardware Key events.
 *
 * The event is routed to the focused item. Before reaching the focused item, [onPreKeyEvent]() is
 * called for parents of the focused item. If the parents don't consume the event, [onPreKeyEvent]()
 * is called for the focused item. If the event is still not consumed, [onKeyEvent]() is called on
 * the focused item's parents.
 */
interface KeyInputModifierNode : DelegatableNode {

    /**
     * This function is called when a [KeyEvent] is received by this node during the upward pass.
     * While implementing this callback, return true to stop propagation of this event. If you
     * return false, the key event will be sent to this [KeyInputModifierNode]'s parent.
     */
    fun onKeyEvent(event: KeyEvent): Boolean

    /**
     * This function is called when a [KeyEvent] is received by this node during the downward pass.
     * It gives ancestors of a focused component the chance to intercept an event. Return true to
     * stop propagation of this event. If you return false, the event will be sent to this
     * [KeyInputModifierNode]'s child. If none of the children consume the event, it will be sent
     * back up to the root using the [onKeyEvent] function.
     */
    fun onPreKeyEvent(event: KeyEvent): Boolean
}
