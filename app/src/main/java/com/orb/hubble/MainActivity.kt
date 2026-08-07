package com.orb.hubble

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orb.hubble.navigation.MyNavigationHost
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Blue1
import com.orb.hubble.ui.theme.HubbleTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.orb.hubble.data.FlowViewModel
import com.orb.hubble.ui.components.CrossLayout
import com.orb.hubble.ui.components.RangeSliderTestScreen
import com.orb.hubble.ui.components.TwoDimensionalLazyLayout
import com.orb.hubble.ui.components.compresser.BlurredImagePost
import com.orb.hubble.ui.components.compresser.FileManager
import com.orb.hubble.ui.components.compresser.ImageCompressor
import com.orb.hubble.ui.components.compresser.ThumbnailUtils
import com.orb.hubble.ui.components.crossPager.SampleCrossPagerUse
import com.orb.hubble.ui.components.crossPager.ScrollTestColumn
import com.orb.hubble.ui.components.getStickerByPrefix
import com.orb.hubble.ui.screens.StickerSource
import com.orb.hubble.ui.theme.Black3
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
            ),
            statusBarStyle = SystemBarStyle.dark(
                Color.TRANSPARENT
            ),
//            statusBarStyle = SystemBarStyle.auto(
//                lightScrim = Color.TRANSPARENT,
//                darkScrim = Color.TRANSPARENT
//            )
        )
        setContent {
            HubbleTheme {
                MyNavigationHost()
//                BlurredImagePost(
//                    thumbnailUtils = ThumbnailUtils(applicationContext)
//                )
                //SampleCrossPagerUse()
                //ScrollTestColumn()
                //RangeSliderTestScreen()
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