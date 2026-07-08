package com.orb.hubble.ui.components.crossPager

/**
 * Contains useful information about an individual item in lazy lists like [LazyColumn] or
 * [LazyRow].
 *
 * @see LazyListLayoutInfo
 */
interface LazyListItemInfo {
    /** The index of the item in the list. */
    val index: Int

    /** The key of the item which was passed to the item() or items() function. */
    val key: Any

    /**
     * The main axis offset of the item in pixels. It is relative to the start of the lazy list
     * container.
     */
    val offset: Int

    /**
     * The main axis size of the item in pixels. Note that if you emit multiple layouts in the
     * composable slot for the item then this size will be calculated as the sum of their sizes.
     */
    val size: Int

    /** The content type of the item which was passed to the item() or items() function. */
    val contentType: Any?
        get() = null
}