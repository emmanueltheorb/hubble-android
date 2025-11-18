package com.orb.hubble.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.diagonalGradient(
    colors: List<Color>
) = this.background(
    brush = Brush.linearGradient(colors)
)