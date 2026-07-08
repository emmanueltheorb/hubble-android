package com.orb.hubble.ui.components.crossPager

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints

/*
 * Defines the measure and layout behavior of a [LazyLayout].
 */
fun interface LazyLayoutMeasurePolicy {
    /**
     * The function that defines the measurement and layout. For each item in this [LazyLayout] we
     * should call [LazyLayoutMeasureScope.compose] and then call [Measurable.measure] with the
     * child [Constraints] to be used.
     *
     * @param constraints The constraints used to measure this Lazy Layout.
     */
    fun LazyLayoutMeasureScope.measure(constraints: Constraints): MeasureResult
}