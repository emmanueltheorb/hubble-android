package com.orb.hubble.ui.components.crossPager

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach

internal interface LazyLayoutMeasuredItem {
    val index: Int
    val key: Any
    val isVertical: Boolean
    val mainAxisSizeWithSpacings: Int
    val placeablesCount: Int
    var nonScrollableItem: Boolean
    val constraints: Constraints
    val lane: Int
    val span: Int

    fun getOffset(index: Int): IntOffset

    fun position(mainAxisOffset: Int, crossAxisOffset: Int, layoutWidth: Int, layoutHeight: Int)

    fun getParentData(index: Int): Any?
}

internal abstract class LazyLayoutMeasuredItemProvider<T : LazyLayoutMeasuredItem> {
    /**
     * A cache of the previously composed items. It allows us to support [get] re-executions with
     * the same index during the same measure pass.
     */
    private val placeablesCache = mutableIntObjectMapOf<List<Placeable>>()

    abstract fun getAndMeasure(index: Int, lane: Int, span: Int, constraints: Constraints): T

    fun LazyLayoutMeasureScope.getPlaceables(
        index: Int,
        constraints: Constraints,
    ): List<Placeable> {
        val cachedPlaceable = placeablesCache[index]
        return if (cachedPlaceable != null) {
            cachedPlaceable
        } else {
            val mensurables = compose(index)
            List(mensurables.size) { i -> mensurables[i].measure(constraints) }
                .also { placeablesCache[index] = it }
        }
    }
}

internal fun <T : LazyLayoutMeasuredItem> updatedVisibleItems(
    firstVisibleIndex: Int,
    lastVisibleIndex: Int,
    positionedItems: List<T>,
    stickingItems: List<T>,
): List<T> {
    if (positionedItems.isEmpty()) return emptyList()

    val finalVisibleItems = stickingItems.toMutableList()

    // positioned items between firstVisibleIndex and lastVisibleIndex
    positionedItems.fastForEach {
        if (it.index in firstVisibleIndex..lastVisibleIndex) finalVisibleItems.add(it)
    }

    finalVisibleItems.sortWith(LazyLayoutMeasuredItemIndexComparator)

    return finalVisibleItems
}

private val LazyLayoutMeasuredItem.mainAxisOffset
    get() = getOffset(0).let { if (isVertical) it.y else it.x }

private val LazyLayoutMeasuredItemIndexComparator =
    Comparator<LazyLayoutMeasuredItem> { a, b -> a.index.compareTo(b.index) }
