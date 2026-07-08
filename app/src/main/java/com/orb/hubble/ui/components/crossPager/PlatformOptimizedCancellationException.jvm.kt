package com.orb.hubble.ui.components.crossPager

import kotlinx.coroutines.CancellationException

private val EmptyStackTraceElements = emptyArray<StackTraceElement>()

internal abstract class PlatformOptimizedCancellationException
constructor(message: String?) : CancellationException(message) {

    override fun fillInStackTrace(): Throwable {
        // Avoid null.clone() on Android <= 6.0 when accessing stackTrace
        stackTrace = EmptyStackTraceElements
        return this
    }
}
