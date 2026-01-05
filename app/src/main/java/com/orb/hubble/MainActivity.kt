package com.orb.hubble

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.orb.hubble.data.postsList10
import com.orb.hubble.navigation.MyNavigationHost
import com.orb.hubble.ui.components.FieldStyle
import com.orb.hubble.ui.components.OverlayText
import com.orb.hubble.ui.components.OverlayTextConfig
import com.orb.hubble.ui.components.StatusIndicatorExamples
import com.orb.hubble.ui.components.StatusItem
import com.orb.hubble.ui.components.StatusView
import com.orb.hubble.ui.screens.HomeScreen
import com.orb.hubble.ui.screens.PostScreen
import com.orb.hubble.ui.screens.TestScreen
import com.orb.hubble.ui.screens.TextEditor
import com.orb.hubble.ui.screens.TextOverlay
import com.orb.hubble.ui.screens.ViewStatusScreen
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Blue1
import com.orb.hubble.ui.theme.HubbleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(
                Color.TRANSPARENT
            )
        )
        setContent {
            WindowCompat.setDecorFitsSystemWindows(window,false)
            HubbleTheme {
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