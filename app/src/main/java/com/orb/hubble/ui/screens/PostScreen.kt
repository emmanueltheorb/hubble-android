package com.orb.hubble.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
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
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import com.orb.hubble.R
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PostScreen(
    modifier: Modifier = Modifier,
    postType: String = "Media",
    onMediaSelected: (String) -> Unit,
    onNavigate: () -> Unit
) {
    val context = LocalActivity.current
    var hasPermissions by remember { mutableStateOf(false) }

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
                    }
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
    } else {
        onNavigate()
        Text("Permissions required to access media")
    }
}

@Composable
fun MediaGrid(
    onMediaSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val mediaItems = remember { mutableStateListOf<MediaItem>() }

    LaunchedEffect(Unit) {
        mediaItems.addAll(loadMediaFromFolders(context))
    }

    LazyVerticalGrid(
        modifier = Modifier.background(color = Black1),
        columns = GridCells.Adaptive(minSize = 128.dp)
    ) {
        items(mediaItems) { item ->
            MediaThumbnail(
                mediaItem = item,
                onMediaSelected = onMediaSelected
            )
        }
    }
}

private suspend fun loadMediaFromFolders(context: Context): List<MediaItem> =
    withContext(Dispatchers.IO) {
        val mediaItems = mutableListOf<MediaItem>()

        // Query for images
        loadImages(context, mediaItems)
        // Query for videos
        loadVideos(context, mediaItems)

        // Sort all items by date added (newest first)
        return@withContext mediaItems.sortedByDescending { it.dateAdded }
    }

private fun loadImages(context: Context, mediaItems: MutableList<MediaItem>) {
    val imageProjection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.RELATIVE_PATH,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val imageSortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        imageUri,
        imageProjection,
        null,
        null,
        imageSortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
        val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val dateAdded = cursor.getLong(dateColumn)
            val mimeType = cursor.getString(mimeTypeColumn)
            val relativePath = cursor.getString(relativePathColumn)
            val bucketName = cursor.getString(bucketColumn)
            val contentUri = ContentUris.withAppendedId(imageUri, id)

            val folder = determineFolder(relativePath, bucketName, mimeType, isVideo = false)

            if (folder in listOf("Camera", "DCIM", "Pictures", "Downloads", "WhatsApp", "Videos")) {
                mediaItems.add(
                    MediaItem(
                        id = id,
                        name = name,
                        uri = contentUri,
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        folder = folder
                    )
                )
            }
        }
    }
}

private fun loadVideos(context: Context, mediaItems: MutableList<MediaItem>) {
    val videoProjection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.RELATIVE_PATH,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.DURATION
    )

    val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val videoSortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        videoUri,
        videoProjection,
        null,
        null,
        videoSortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
        val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
        val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val dateAdded = cursor.getLong(dateColumn)
            val mimeType = cursor.getString(mimeTypeColumn)
            val relativePath = cursor.getString(relativePathColumn)
            val bucketName = cursor.getString(bucketColumn)
            val duration = cursor.getLong(durationColumn)
            val contentUri = ContentUris.withAppendedId(videoUri, id)

            val folder = determineFolder(relativePath, bucketName, mimeType, isVideo = true)

            if (folder in listOf("Camera", "DCIM", "Pictures", "Downloads", "WhatsApp", "Videos")) {
                mediaItems.add(
                    MediaItem(
                        id = id,
                        name = name,
                        uri = contentUri,
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        duration = duration,
                        folder = folder
                    )
                )
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
        val imageRequest = remember(mediaItem.uri) {
            ImageRequest.Builder(context)
                .data(mediaItem.uri)
                .apply {
                    if (mediaItem.mimeType.startsWith("video/")) {
                        videoFrameMillis(1000)
                    }
                }
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    onMediaSelected(mediaItem.uri)
                },
            contentScale = ContentScale.Crop,
            imageLoader = ImageLoader.Builder(context)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        )

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

private fun determineFolder(
    relativePath: String?,
    bucketName: String?,
    mimeType: String?,
    isVideo: Boolean
): String {
    // Check bucket name first (more reliable)
    when {
        bucketName?.equals("Camera", ignoreCase = true) == true -> return "Camera"
        bucketName?.equals("Pictures", ignoreCase = true) == true -> return "Pictures"
        bucketName?.equals("Download", ignoreCase = true) == true -> return "Downloads"
        bucketName?.equals("Downloads", ignoreCase = true) == true -> return "Downloads"
        bucketName?.equals("WhatsApp", ignoreCase = true) == true -> return "WhatsApp"
        bucketName?.equals("WhatsApp Images", ignoreCase = true) == true -> return "WhatsApp"
        bucketName?.equals("WhatsApp Video", ignoreCase = true) == true -> return "WhatsApp"
    }

    // Check relative path as fallback
    when {
        relativePath?.contains("DCIM/") == true -> return "DCIM"
        relativePath?.contains("Pictures") == true -> return "Pictures"
        relativePath?.contains("Download") == true -> return "Downloads"
        relativePath?.contains("WhatsApp") == true -> return "WhatsApp"
        relativePath?.contains("video/") == true -> return "Videos"
    }

    return "others"
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

data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val dateAdded: Long,
    val duration: Long = 0,
    val folder: String = ""
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