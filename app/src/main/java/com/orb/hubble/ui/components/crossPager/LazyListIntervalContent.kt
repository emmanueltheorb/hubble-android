package com.orb.hubble.ui.components.crossPager

import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.emptyIntList
import androidx.collection.mutableIntListOf
import androidx.compose.runtime.Composable

internal class LazyListIntervalContent(content: LazyListScope.() -> Unit) :
    LazyLayoutIntervalContent<LazyListInterval>(), LazyListScope {
    override val intervals: MutableIntervalList<LazyListInterval> = MutableIntervalList()

    private var _headerIndexes: MutableIntList? = null
    val headerIndexes: IntList
        get() = _headerIndexes ?: emptyIntList()

    init {
        apply(content)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        intervals.addInterval(
            count,
            LazyListInterval(key = key, type = contentType, item = itemContent),
        )
    }

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        intervals.addInterval(
            1,
            LazyListInterval(
                key = if (key != null) { _: Int -> key } else null,
                type = { contentType },
                item = { content() },
            ),
        )
    }

    override fun stickyHeader(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyItemScope.(Int) -> Unit,
    ) {
        val headersIndexes = _headerIndexes ?: mutableIntListOf().also { _headerIndexes = it }
        headersIndexes.add(intervals.size)
        val headerIndex = intervals.size

        item(key, contentType) { content(headerIndex) }
    }
}

internal class LazyListInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val item: @Composable LazyItemScope.(index: Int) -> Unit,
) : LazyLayoutIntervalContent.Interval
