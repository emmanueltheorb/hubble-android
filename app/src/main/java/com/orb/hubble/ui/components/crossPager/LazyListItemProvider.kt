package com.orb.hubble.ui.components.crossPager

import androidx.collection.IntList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

internal interface LazyListItemProvider : LazyLayoutItemProvider {
    val keyIndexMap: LazyLayoutKeyIndexMap
    /** The list of indexes of the sticky header items */
    val headerIndexes: IntList
    /** The scope used by the item content lambdas */
    val itemScope: LazyItemScopeImpl
}

@Composable
internal fun rememberLazyListItemProviderLambda(
    state: LazyListState,
    content: LazyListScope.() -> Unit,
): () -> LazyListItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        val scope = LazyItemScopeImpl()
        val intervalContentState =
            derivedStateOf(referentialEqualityPolicy()) {
                LazyListIntervalContent(latestContent.value)
            }
        val itemProviderState =
            derivedStateOf(referentialEqualityPolicy()) {
                val intervalContent = intervalContentState.value
                val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
                LazyListItemProviderImpl(
                    state = state,
                    intervalContent = intervalContent,
                    itemScope = scope,
                    keyIndexMap = map,
                )
            }
        itemProviderState::value
    }
}

private class LazyListItemProviderImpl
constructor(
    private val state: LazyListState,
    private val intervalContent: LazyListIntervalContent,
    override val itemScope: LazyItemScopeImpl,
    override val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyListItemProvider {

    override val itemCount: Int
        get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(itemScope, localIndex)
            }
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getContentType(index: Int): Any? = intervalContent.getContentType(index)

    override val headerIndexes: IntList
        get() = intervalContent.headerIndexes

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyListItemProviderImpl) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}
