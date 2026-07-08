package com.orb.hubble.ui.components.crossPager

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/**
 * This is used to determine how Pages are laid out in [Pager]. By changing the size of the pages
 * one can change how many pages are shown.
 *
 * Please refer to the sample to learn how to use this API.
 *
 * @sample androidx.compose.foundation.samples.CustomPageSizeSample
 */
@Stable
interface PageSize {

    /**
     * Based on [availableSpace] pick a size for the pages
     *
     * @param availableSpace The amount of space in pixels the pages in this Pager can use.
     * @param pageSpacing The amount of space in pixels used to separate pages.
     */
    fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int

    /** Pages take up the whole Pager size. */
    object Fill : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return availableSpace
        }
    }

    /**
     * Multiple pages in a viewport
     *
     * @param pageSize A fixed size for pages
     */
    class Fixed(val pageSize: Dp) : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return pageSize.roundToPx()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Fixed) return false
            return pageSize == other.pageSize
        }

        override fun hashCode(): Int {
            return pageSize.hashCode()
        }
    }
}