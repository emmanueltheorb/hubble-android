package com.orb.hubble

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.orb.hubble.navigation.MyNavigationHost
import com.orb.hubble.ui.components.FieldStyle
import com.orb.hubble.ui.components.OverlayText
import com.orb.hubble.ui.components.OverlayTextConfig
import com.orb.hubble.ui.components.StatusIndicatorExamples
import com.orb.hubble.ui.components.StatusItem
import com.orb.hubble.ui.components.StatusView
import com.orb.hubble.ui.components.VideoCropper
import com.orb.hubble.ui.screens.HomeScreen
import com.orb.hubble.ui.screens.PostScreen
import com.orb.hubble.ui.screens.TextEditor
import com.orb.hubble.ui.screens.TextOverlay
import com.orb.hubble.ui.screens.ViewStatusScreen
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Blue1
import com.orb.hubble.ui.theme.HubbleTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorkerFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.work.Configuration
import com.orb.hubble.ui.components.VideoPlayer
import com.orb.hubble.ui.screens.ReplyLogScreen
import com.orb.hubble.utils.VideoCacheManager
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var videoCacheManager: VideoCacheManager

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(
                Color.TRANSPARENT
            )
        )
        setContent {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            HubbleTheme {
                //ReplyLogScreen()
                MyNavigationHost()
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    AsyncImage(
//                        model = ImageRequest.Builder(LocalContext.current)
//                            .data("https://roma-hexaplaric-trilaterally.ngrok-free.dev/media/media_1767549860531__thumb.jpg?v=63934769068")
//                            .crossfade(true)
//                            .build(),
//                        contentDescription = "Selected image"
//                    )
//
//                }
//                val videoUrl =
//                    "https://roma-hexaplaric-trilaterally.ngrok-free.dev/media/media_1769796091740__original.mp4?v=63937015275"
//
//                VideoPlayer(
//                    videoUrl = videoUrl,
//                    dataSourceFactory = videoCacheManager.getDataSourceFactory()
//                )
            }
        }
    }
}

@Composable
fun ColorScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Black1),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = modifier.size(50.dp),
            shape = CircleShape,
            color = Blue1
        ) {

        }
    }
}