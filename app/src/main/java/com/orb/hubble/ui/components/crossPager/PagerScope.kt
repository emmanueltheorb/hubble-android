package com.orb.hubble.ui.components.crossPager

/**
 * Receiver scope for [Pager]. Note: This is empty now, but we may add new members in the future if
 * needed.
 */
sealed interface PagerScope

internal object PagerScopeImpl : PagerScope
