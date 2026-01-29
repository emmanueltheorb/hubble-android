package com.orb.hubble.ui.components

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

@Composable
fun VideoCropper(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pickedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }

    var startTime by remember { mutableFloatStateOf(0f) }
    var endTime by remember { mutableFloatStateOf(10f) }

    val maxVideoDurationSec = (videoDurationMs / 1000f).coerceAtLeast(1f)

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val durationMs = getVideoDuration(context, it)
            if (durationMs >= 1000L) {
                pickedVideoUri = uri
                videoDurationMs = durationMs

                startTime = 0f
                endTime = 10f.coerceAtMost(durationMs / 1000f)
            } else {
                Toast.makeText(context, "Please select a video at least 1 second long", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Button(
                onClick = {
                    videoPickerLauncher.launch("video/*")
                }
            ) {
                Text("Pick Video")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        pickedVideoUri?.let { uri ->
            item {
                Text("Video Player")
                VideoViewer(
                    videoUri = uri,
                    startTimeMs = (startTime * 1000).toLong(),
                    endTimeMs = (endTime * 1000).toLong()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Crop Range")

                RangeSlider(
                    value = startTime..endTime,
                    onValueChange = { range ->
                        val newStart = range.start.coerceIn(0f, maxVideoDurationSec)
                        val newEnd = range.endInclusive.coerceIn(0f, maxVideoDurationSec)

                        if (newEnd >= newStart) {
                            val difference = newEnd -newStart
                            if (difference > 10) {
                                if (range.start != startTime) {
                                    startTime = newStart
                                    endTime = (newStart + 10).coerceAtMost(maxVideoDurationSec)
                                } else {
                                    endTime = newEnd
                                    startTime = (newEnd - 10).coerceAtLeast(0f)
                                }
                            } else {
                                startTime = newStart
                                endTime = newEnd
                            }
                        }
                    },
                    valueRange = 0f..1f.coerceAtLeast(maxVideoDurationSec),
                    steps = 0
                )
                Text("Crop from ${startTime.toInt()}s to ${endTime.toInt()}s")

                Button(
                    onClick = {
                        val outputFile = File(context.cacheDir, "trimmed_video.mp4")
                        val startTimeMs = (startTime * 1000).toLong()
                        val endTimeMs = (endTime * 1000).toLong()

                        CoroutineScope(Dispatchers.IO).launch {
                            trimVideo(context, uri, outputFile, startTimeMs, endTimeMs,
                                onSuccess = {
                                    pickedVideoUri = outputFile.toUri()
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "Cropping Done!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onError = { error ->
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Text("Crop Video")
                }
            }

        }

    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoViewer(
    modifier: Modifier = Modifier,
    videoUri: Uri,
    startTimeMs: Long,
    endTimeMs: Long
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(videoUri, startTimeMs, endTimeMs) {
        player.setMediaItem(MediaItem.fromUri(videoUri))
        player.prepare()
        player.seekTo(startTimeMs)
        player.playWhenReady = true
    }

    LaunchedEffect(player, startTimeMs, endTimeMs) {
        while (true) {
            if (player.currentPosition >= endTimeMs) {
                player.pause()
                player.seekTo(startTimeMs)
                player.playWhenReady = true
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {

        onDispose {
            player.release()
        }
    }

    val presentation = rememberPresentationState(player)

    Box {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.height(300.dp)
        )
        if (presentation.coverSurface) {
            Box(
                modifier = Modifier
                    .background(Color.Black)
            )
        }
    }
}

fun trimVideo(
    context: Context,
    inputUri: Uri,
    outputFile: File,
    startTimeMs: Long,
    endTimeMs: Long,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val inputPath = getRealPathFromUri(context, inputUri) ?: return onError("Failed to get file path from Uri")

    try {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)


        var videoTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                videoTrackIndex = i
                break
            }
        }

        if (videoTrackIndex == -1) {
            onError("No video track found!")
            return
        }

        extractor.selectTrack(videoTrackIndex)
        extractor.seekTo(startTimeMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val format = extractor.getTrackFormat(videoTrackIndex)
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackIndex = muxer.addTrack(format)

        val buffer = ByteBuffer.allocate(4 * 1024 * 1024)
        val info = MediaCodec.BufferInfo()

        muxer.start()

        while (true) {
            val sampleTime = extractor.sampleTime
            if (sampleTime < 0 || sampleTime > endTimeMs * 1000) break

            info.size * extractor.readSampleData(buffer, 0)
            if (info.size < 0) break

            info.presentationTimeUs = sampleTime
            //info.flags = extractor.sampleFlags
            info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME
            muxer.writeSampleData(trackIndex, buffer, info)

            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()

        onSuccess()
    } catch (e: Exception) {
        e.printStackTrace()
        onError(e.localizedMessage ?: "Unknown error")
    }
}

fun getRealPathFromUri(context: Context, uri: Uri): String? {
    return when {
        DocumentsContract.isDocumentUri(context, uri) -> {
            val documentId = DocumentsContract.getDocumentId(uri)
            if (documentId.startsWith("raw:")) documentId.removePrefix("raw:") else null
        }
        uri.scheme.equals("content", ignoreCase = true) -> {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val tempFile = File(context.cacheDir, "temp_video.mp4")
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    tempFile.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        else -> uri.path
    }
}

fun getVideoDuration(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    } finally {
        retriever.release()
    }
}