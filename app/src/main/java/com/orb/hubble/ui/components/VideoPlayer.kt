package com.orb.hubble.ui.components

import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.orb.hubble.R
import com.orb.hubble.ui.theme.Grey2
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    paddingTop: Dp = 1.dp,
    paddingBottom: Dp = 0.dp,
    videoUrl: String,
    description: String? = "Wahala be like i phone",
    onGoLeft: () -> Unit,
    onGoRight: () -> Unit,
    onCenterTapped: () -> Unit,
    pausePlay: Boolean = false,
    dataSourceFactory: DataSource.Factory
) {
    val context = LocalContext.current

    val currentPosition = remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                playWhenReady = true
            }
    }

    LaunchedEffect(videoUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
        isBuffering = true

        if (pausePlay) {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition.longValue = exoPlayer.currentPosition
            delay(400)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING) {
                    isBuffering = true
                } else if (playbackState == Player.STATE_READY) {
                    isBuffering = false
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
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
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { offset ->
                        val centerWidthPercent = 0.3f
                        val screenWidth = size.width
                        val screenMid = screenWidth / 2
                        val centerStart = screenMid - (screenWidth * centerWidthPercent / 2)
                        val centerEnd = screenMid + (screenWidth * centerWidthPercent / 2)
                        if (offset.x in centerStart..centerEnd) {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                            } else {
                                exoPlayer.play()
                            }
                            onCenterTapped()
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
            ContentFrame(
                player = exoPlayer,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                contentScale = ContentScale.Fit
            )

            if (isBuffering) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeCap = StrokeCap.Butt,
                    strokeWidth = 2.5.dp
                )
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (description != null && description.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            //.fillMaxHeight(0.2f)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(10.dp)
                            .padding(bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
//                        BoxWithConstraints {
//                            val width = constraints.maxWidth
//                            DescriptionText(
//                                text = "So as i stared inot the lonely abyss that marked the \uD83E\uDD70",
//                                containerWidth = width
//                            )
//                        }
                        Text(
                            text = "So as i stared inot the lonely abyss that marked the \uD83E\uDD70", //formerly description
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                VideoSeeker(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    duration = exoPlayer.duration.coerceAtLeast(0L),
                    currentPosition = currentPosition.longValue,
                    onSeek = { position ->
                        exoPlayer.seekTo(position)
                    }
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSeeker(
    modifier: Modifier = Modifier,
    duration: Long,
    currentPosition: Long,
    onSeek: (Long) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }

    val animatedPosition by animateFloatAsState(
        targetValue = if (isDragging) sliderPosition else currentPosition.toFloat(),
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 200,
            easing = LinearEasing
        ),
        label = "SliderSmoothing"
    )

    val videoDuration = if (duration > 0) duration.toFloat() else 1f

    LaunchedEffect(currentPosition) {
        if (!isDragging) {
            sliderPosition = currentPosition.toFloat()
        }
    }

    MySlider(
        value = animatedPosition,
        onValueChange = {
            isDragging = true
            sliderPosition = it
            onSeek(sliderPosition.toLong())
        },
        onValueChangeFinished = {
            isDragging = false
        },
        valueRange = 0f..videoDuration,
        interactionSource = interactionSource,
        thumb = { },
        track = { sliderState ->
            MyHorizontalTrack(
                sliderState = sliderState,
                interactionSource = interactionSource
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}

data class PlayerAction(
    val actionType: PlayerActionType,
    val data: Any? = null
)

enum class PlayerActionType {
    PLAY, PAUSE, SEEK
}