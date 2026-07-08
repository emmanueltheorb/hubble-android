package com.orb.hubble.ui.components.crossPager

import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

/**
 * Provides [SaveableStateHolder] to be used with the lazy layout items. This allows to save the
 * state of the items like inner LazyRow scroll position which such items are scrolled away in order
 * to restore it when this item is visible again. However on top of the default logic provided via
 * [rememberSaveableStateHolder] this instance will only save the items which are currently visible
 * when the parent [SaveableStateRegistry] triggers [SaveableStateRegistry.performSave] in order to
 * save on the space we use in the Android`s Bundle to not cause crash with
 * TransactionTooLargeException.
 */
@Composable
internal fun LazySaveableStateHolderProvider(content: @Composable (SaveableStateHolder) -> Unit) {
    val currentRegistry = LocalSaveableStateRegistry.current
    val wrappedHolder = rememberSaveableStateHolder()
    val holder =
        rememberSaveable(
            currentRegistry,
            saver = LazySaveableStateHolder.saver(currentRegistry, wrappedHolder),
        ) {
            LazySaveableStateHolder(currentRegistry, emptyMap(), wrappedHolder)
        }
    CompositionLocalProvider(LocalSaveableStateRegistry provides holder) { content(holder) }
}

private class LazySaveableStateHolder(
    private val wrappedRegistry: SaveableStateRegistry,
    private val wrappedHolder: SaveableStateHolder,
) : SaveableStateRegistry by wrappedRegistry, SaveableStateHolder {

    constructor(
        parentRegistry: SaveableStateRegistry?,
        restoredValues: Map<String, List<Any?>>?,
        wrappedHolder: SaveableStateHolder,
    ) : this(
        SaveableStateRegistry(restoredValues) { parentRegistry?.canBeSaved(it) ?: true },
        wrappedHolder,
    )

    private val previouslyComposedKeys = mutableScatterSetOf<Any>()

    override fun performSave(): Map<String, List<Any?>> {
        previouslyComposedKeys.forEach { wrappedHolder.removeState(it) }
        return wrappedRegistry.performSave()
    }

    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        wrappedHolder.SaveableStateProvider(key, content)
        DisposableEffect(key) {
            previouslyComposedKeys -= key
            onDispose { previouslyComposedKeys += key }
        }
    }

    override fun removeState(key: Any) {
        wrappedHolder.removeState(key)
    }

    companion object {
        fun saver(parentRegistry: SaveableStateRegistry?, wrappedHolder: SaveableStateHolder) =
            Saver<LazySaveableStateHolder, Map<String, List<Any?>>>(
                save = { it.performSave().ifEmpty { null } },
                restore = { restored ->
                    LazySaveableStateHolder(parentRegistry, restored, wrappedHolder)
                },
            )
    }
}
