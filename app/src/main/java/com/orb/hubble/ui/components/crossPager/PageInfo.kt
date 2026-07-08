package com.orb.hubble.ui.components.crossPager

/** This represents a single measured page in a [Pager] layout. */
sealed interface PageInfo {

    /** The index of this page. */
    val index: Int

    /**
     * The key of the page which was passed to the [HorizontalPager] or [VerticalPager] composables.
     */
    val key: Any

    /**
     * The main axis offset of the item in pixels. It is relative to the start of the [Pager]
     * container.
     */
    val offset: Int
}