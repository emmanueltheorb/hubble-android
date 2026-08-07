package com.orb.hubble.ui.utils

import androidx.annotation.DrawableRes
import com.orb.hubble.R

/**
 * Deterministically picks a drawable ID from the list based on the provided [value].
 * Always returns the same drawable ID for the same [value].
 */
fun List<Int>.withHash(
    value: Any?,
    @DrawableRes fallback: Int = R.drawable.thumb1
): Int {
    //Edge case 1: Empty list safety guard
    if (this.isEmpty()) return fallback

    //Edge case 2 & 3: Safe nullable hash code lookup
    val hash = value?.hashCode() ?: 0

    //Math.floorMod safely handles negative hashes and Int.MIN_VALUE overflow!
    val index = Math.floorMod(hash, this.size)

    return this[index]
}