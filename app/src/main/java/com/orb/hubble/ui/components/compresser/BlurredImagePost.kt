package com.orb.hubble.ui.components.compresser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BlurredImagePost(
    modifier: Modifier = Modifier
) {
    val placeholderBitmap: MutableState<Bitmap?> = remember { mutableStateOf(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { contentUri ->
        if (contentUri == null) {
            return@rememberLauncherForActivityResult
        }

        val inputBytes = context
            .contentResolver
            .openInputStream(contentUri)
            ?.use { inputStream ->
                inputStream.readBytes()
            } ?: return@rememberLauncherForActivityResult

        val bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)

        val bitmapString = ThumbnailUtils.generateBase64Thumbnail(sourcedBitmap = bitmap)

        placeholderBitmap.value = ThumbnailUtils.decodeBase64Thumbnail(bitmapString)
    }

    Column {
        Box(modifier = modifier.fillMaxWidth()) {
            if (placeholderBitmap.value != null) {
                Image(
                    bitmap = placeholderBitmap.value!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .blur(16.dp),
                    contentScale = ContentScale.FillWidth
                )
            }

//        AsyncImage(
//            model = imageUrl,
//            contentDescription = "Post Image",
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )
        }

        Button(
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        ) {
            Text("Pick an Image")
        }
    }
}