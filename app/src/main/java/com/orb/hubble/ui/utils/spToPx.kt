package com.orb.hubble.ui.utils

import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.ui.unit.TextUnit

fun TextUnit.toPx(): Float {
    val metrics = Resources.getSystem().displayMetrics
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.value,
        metrics
    )
}