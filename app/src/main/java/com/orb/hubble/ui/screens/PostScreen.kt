package com.orb.hubble.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import com.orb.hubble.R
import com.orb.hubble.data.PostScreenViewModel
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Black3
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PostScreen(
    modifier: Modifier = Modifier,
    viewModel: PostScreenViewModel = hiltViewModel(),
    postType: String = "Media",
    onMediaSelected: (String) -> Unit,
    onNavigate: () -> Unit
) {
    val context = LocalActivity.current
    var hasPermissions by remember { mutableStateOf(false) }
    var goBack by remember { mutableStateOf(false) }

    val pagingItems = viewModel.mediaPager.collectAsLazyPagingItems()

    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value.toInt()
    val screenHeight = LocalWindowInfo.current.containerSize.height.dp

    // System Image Picker Launcher
    val pickVisualMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onMediaSelected(it.toString()) }
    }
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onMediaSelected(it.toString()) }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
        if (!hasPermissions) goBack = true
    }

    // Check permissions on launch
    LaunchedEffect(Unit) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(requiredPermissions)
    }

    if (hasPermissions) {
        if (postType == "Media") {
            Box(
                modifier = Modifier
                    .background(Black1)
                    .padding(top = topPadding.dp)
                    .padding(bottom = bottomPadding.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                MediaGrid(
                    onMediaSelected = { uri ->
                        onMediaSelected(uri.toString())
                    },
                    pagingItems = pagingItems
                )
                MoreFolder {
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
            }
        }

        if (postType == "Text") {

        }
    }
    if (goBack) {
        onNavigate()
        Text("Permissions required to access media")
    }
}

@Composable
fun MediaGrid(
    onMediaSelected: (Uri) -> Unit,
    pagingItems: LazyPagingItems<MediaItem>
) {
    LazyVerticalGrid(
        modifier = Modifier.background(color = Black1),
        columns = GridCells.Adaptive(minSize = 128.dp)
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = pagingItems.itemContentType { "media" }
        ) { index ->
            val item = pagingItems[index]
            if (item != null) {
                MediaThumbnail(
                    mediaItem = item,
                    onMediaSelected = onMediaSelected
                )
            } else {
                Box(Modifier.size(128.dp).background(Color.Transparent))
            }
        }
    }
}

@Composable
fun MediaThumbnail(
    mediaItem: MediaItem,
    onMediaSelected: (Uri) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(128.dp)
            .padding(1.dp)
    ) {
        val model = remember(mediaItem.uri) {
            mediaItem.thumbnail
                ?: ImageRequest.Builder(context)
                    .data(mediaItem.uri)
                    .precision(Precision.INEXACT)
                    .size(250, 250)
                    .crossfade(true)
                    .build()
        }

        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxSize().background(Black3))
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        onMediaSelected(mediaItem.uri)
                    },
                contentScale = ContentScale.Crop
            )
        }

        if (mediaItem.mimeType.startsWith("video/")) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_play),
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                    )
                    Text(
                        text = formatDuration(mediaItem.duration),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))

    return if (hours > 0) {
        String.format(
            locale = java.util.Locale.getDefault(),
            format = "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    } else {
        String.format(
            locale = java.util.Locale.getDefault(),
            format = "%02d:%02d",
            minutes,
            seconds
        )
    }
}

@Stable
data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val dateAdded: Long,
    val duration: Long = 0,
    val folder: String = "",
    val thumbnail: Bitmap? = null
)

@Composable
fun MoreFolder(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.padding(18.dp)
    ) {
        Surface(
            modifier = modifier
                .size(55.dp),
            shape = RoundedCornerShape(16.dp),
            color = Grey2,
            contentColor = White3
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.file_icon),
                    contentDescription = null,
                    modifier = modifier.size(34.dp)
                )
            }
        }
    }
}