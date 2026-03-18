package com.orb.hubble.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.orb.hubble.ui.theme.Grey2

@Composable
fun ImageViewer(
    imageUrl: String,
    paddingTop: Dp = 1.dp,
    paddingBottom: Dp = 0.dp,
    onGoRight: () -> Unit,
    onGoLeft: () -> Unit
) {
    Column(
        modifier = Modifier
        .fillMaxSize()
        .padding(top = paddingTop)
        .padding(bottom = paddingBottom)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
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
            AsyncImage(
                model = imageUrl,
                contentDescription = "an image post",
                contentScale = ContentScale.Fit
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Grey2
        )
    }
}