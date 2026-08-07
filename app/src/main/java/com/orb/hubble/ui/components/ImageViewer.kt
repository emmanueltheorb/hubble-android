package com.orb.hubble.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.orb.hubble.ui.theme.Black4
import com.orb.hubble.ui.theme.Grey2

@Composable
fun ImageViewer(
    imageUrl: String,
    thumbnailString: String?,
    paddingTop: Dp = 1.dp,
    paddingBottom: Dp = 0.dp,
    onGoRight: () -> Unit,
    onGoLeft: () -> Unit
) {
    val blurThumbnail = remember(thumbnailString) {
        if (!thumbnailString.isNullOrEmpty()) {
            decodeBase64Thumbnail(thumbnailString)?.let { bitmap ->
                BitmapPainter(bitmap.asImageBitmap())
            }
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
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
//            AsyncImage(
//                model = imageUrl,
//                contentDescription = "an image post",
//                contentScale = ContentScale.Fit,
//                placeholder = blurThumbnail,
//                error = blurThumbnail,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .blur(15.dp)
//            )

            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "an image post",
                contentScale = ContentScale.Fit,
                loading = {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = null,
                            contentDescription = "placeholder blurred image",
                            contentScale = ContentScale.Fit,
                            placeholder = blurThumbnail,
                            error = blurThumbnail,
                            modifier = Modifier
                                .fillMaxWidth()
                                .blur(15.dp)
                        )
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeCap = StrokeCap.Butt,
                            strokeWidth = 2.5.dp
                        )
                    }
                },
                error = {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = null,
                            contentDescription = "placeholder blurred image",
                            contentScale = ContentScale.Fit,
                            placeholder = blurThumbnail,
                            error = blurThumbnail,
                            modifier = Modifier
                                .fillMaxWidth()
                                .blur(15.dp)
                        )
                        CircularProgressIndicator(
                            color = Color.Red,
                            strokeCap = StrokeCap.Butt,
                            strokeWidth = 2.5.dp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Black4
        )
    }
}

fun decodeBase64Thumbnail(base64String: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}