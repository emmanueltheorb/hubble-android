package com.orb.hubble.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TextPostViewer(
    contentText: String,
    backgroundColor: Color,
    topPadding: Dp = 1.dp,
    bottomPadding: Dp = 0.dp,
    onGoLeft: () -> Unit,
    onGoRight: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(top = topPadding)
            .padding(bottom = bottomPadding)
            .background(color = backgroundColor, shape = RoundedCornerShape(15.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    val centerWidthPercent = 0.3f
                    val screenWidth = size.width
                    val screenMid = screenWidth / 2
                    val centerStart = screenMid - (screenWidth * centerWidthPercent / 2)
                    val centerEnd = screenMid + (screenWidth * centerWidthPercent / 2)
                    if (offset.x in centerStart..centerEnd) {
                        //when tap is in center area
                        onGoRight()
                    }
                    if (offset.x in 0f..centerStart) {
                        onGoLeft()
                    }
                    if (offset.x in centerEnd..screenWidth.toFloat()) {
                        onGoRight()
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = contentText,
            textAlign = TextAlign.Center,
            color = Color.White
        )
    }
}