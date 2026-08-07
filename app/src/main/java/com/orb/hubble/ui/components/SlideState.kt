package com.orb.hubble.ui.components

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.orb.hubble.R
import com.orb.hubble.ui.screens.MoreFolder
import com.orb.hubble.ui.theme.Black1
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RangeSliderTestScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoUri = remember<MutableState<Uri?>> { mutableStateOf(null) }

    val isVideo = remember(videoUri.value) {
        if (videoUri.value != null) {
            val mimeType = context.contentResolver.getType(videoUri.value!!)
            mimeType?.startsWith("video/", ignoreCase = true) == true
        } else false
    }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val currentPosition = remember { mutableLongStateOf(0L) }
    val centerInteractionSource = remember { MutableInteractionSource() }

    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var startTime by remember { mutableFloatStateOf(0f) }
    var endTime by remember { mutableFloatStateOf(90f) }

    val animatedPosition by animateFloatAsState(
        targetValue = if (isDragging) sliderPosition else startTime + (currentPosition.longValue / 1000f),
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 200,
            easing = LinearEasing
        ),
        label = "SliderSmoothing"
    )

    val maxVideoDurationSec = (videoDurationMs / 1000f).coerceAtLeast(1f)

    // System Image Picker Launcher
    val pickVisualMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            videoUri.value = uri
        }
    }
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            videoUri.value = uri
        }
    }

    var isBuffering by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    LaunchedEffect(currentPosition) {
        if (!isDragging) {
            sliderPosition = currentPosition.longValue.toFloat()
        }
    }

    LaunchedEffect(videoUri.value) {
        if (videoUri.value != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUri.value!!))
            exoPlayer.prepare()
            isBuffering = true
        }

//        if (pausePlay) {
//            exoPlayer.pause()
//        }
    }

    LaunchedEffect(videoUri.value) {
        if (isVideo && videoUri.value != null) {
            val durationMs = getVideoDuration(context, videoUri.value!!)
            videoDurationMs = durationMs

            startTime = 0f
            endTime = 90f.coerceAtMost(durationMs / 1000f)
        }
    }

    LaunchedEffect(videoUri.value, startTime, endTime) {
        if (videoUri.value != null) {
            val startTimeMs = (startTime * 1000).toLong()
            val endTimeMs = (endTime * 1000).toLong()

            val mediaItem = MediaItem.Builder()
                .setUri(videoUri.value)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startTimeMs)
                        .setEndPositionMs(endTimeMs)
                        .build()
                )
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition.longValue = exoPlayer.currentPosition
            delay(400.milliseconds)
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

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                //exoPlayer.prepare()
                isBuffering = true
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Black1)
    ) {
        ContentFrame(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            contentScale = ContentScale.Fit,
            shutter = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                ) {
                    Column {
                        Image(
                            painter = painterResource(R.drawable.thumbnail_load_error),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                            //.fillMaxWidth()
                        )
                    }
                }
            },
            modifier = Modifier
                .size(400.dp)
        )

        MoreFolder(
            modifier = Modifier,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickVisualMedia.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                } else {
                    pickMediaLauncher.launch("*/*")
                }
            }
        )

        if (videoUri.value != null && isVideo) {
//            RangeSlider(
//                value = startTime..endTime,
//                modifier = Modifier.fillMaxSize(),
//                onValueChange = { range ->
//                    val newStart = range.start.coerceIn(0f, maxVideoDurationSec)
//                    val newEnd = range.endInclusive.coerceIn(0f, maxVideoDurationSec)
//
//                    if (newEnd >= newStart) {
//                        val difference = newEnd - newStart
//                        if (difference > 90) {
//                            if (range.start != startTime) {
//                                startTime = newStart
//                                endTime = (newStart + 90).coerceAtMost(maxVideoDurationSec)
//                            } else {
//                                endTime = newEnd
//                                startTime = (newEnd - 90).coerceAtLeast(0f)
//                            }
//                        } else {
//                            startTime = newStart
//                            endTime = newEnd
//                        }
//                    }
//                },
//                valueRange = 0f..1f.coerceAtLeast(maxVideoDurationSec),
//                steps = 0,
//                colors = SliderDefaults.colors(
//                    activeTrackColor = Color.Transparent,
//                    inactiveTrackColor = Color.Black.copy(alpha = 0.5f)
//                )
//            )

            MyRangeSlider(
                currentValue = animatedPosition,
                onCurrentValueChange = {
                    isDragging = true
                    sliderPosition = it
                },
                value = startTime..endTime,
                modifier = Modifier.fillMaxSize(),
                onValueChange = { range ->
                    val newStart = range.start.coerceIn(0f, maxVideoDurationSec)
                    val newEnd = range.endInclusive.coerceIn(0f, maxVideoDurationSec)

                    if (newEnd >= newStart) {
                        val difference = newEnd - newStart
                        if (difference > 90) {
                            if (range.start != startTime) {
                                startTime = newStart
                                endTime = (newStart + 90).coerceAtMost(maxVideoDurationSec)
                            } else {
                                endTime = newEnd
                                startTime = (newEnd - 90).coerceAtLeast(0f)
                            }
                        } else {
                            startTime = newStart
                            endTime = newEnd
                        }
                    }
                },
                valueRange = 0f..maxVideoDurationSec,
                steps = 0,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Black.copy(alpha = 0.5f)
                )
            )

            VideoSeeker(
                modifier = Modifier,
                duration = exoPlayer.duration.coerceAtLeast(0L),
                currentPosition = currentPosition.longValue,
                onSeek = { position ->
                    exoPlayer.seekTo(position)
                }
            )
        }
    }
}