package com.orb.hubble.ui.components

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.orb.hubble.R
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.StickerSource
import java.lang.reflect.Field

@Composable
fun OverlayStickers(
    modifier: Modifier = Modifier,
    onStickerClicked: (OverlaySticker) -> Unit
) {
    val context = LocalContext.current

    val stickers = remember {
        context.getStickerByPrefix()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(75.dp)
    ) {
        items(stickers) { sticker ->

            val imageRequest = remember(sticker.source) {
                val coilData = when (val source = sticker.source) {
                    is StickerSource.Resource -> source.resId  // Int
                    is StickerSource.Uri -> source.uri         // Uri
                    is StickerSource.Url -> source.url         // String
                    is StickerSource.Asset -> source.path      // String for asset path
                }

                ImageRequest.Builder(context)
                    .data(coilData)
                    .crossfade(true)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .size(75.dp)
                    .padding(2.5.dp)
                    .clickable {
                        onStickerClicked(sticker)
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

fun Context.getStickerByPrefix(): List<OverlaySticker> {
    val prefix = "stk"
    val fields: Array<Field> = R.drawable::class.java.fields

    return fields
        .filter { it.name.startsWith(prefix) }
        .mapNotNull { field ->
            try {
                @DrawableRes val resId = field.getInt(null)
                val resName = field.name
                OverlaySticker(
                    id = resName,
                    source = StickerSource.Resource(resId)
                )
            } catch (e: IllegalAccessException) {
                null
            }
        }
}

@Composable
fun OverlaySticker(
    modifier: Modifier = Modifier,
    sticker: OverlaySticker
) {
    val context = LocalContext.current

    val imageRequest = remember(sticker.source) {
        val coilData = when (val source = sticker.source) {
            is StickerSource.Resource -> source.resId  // Int
            is StickerSource.Uri -> source.uri         // Uri
            is StickerSource.Url -> source.url         // String
            is StickerSource.Asset -> source.path      // String for asset path
        }

        ImageRequest.Builder(context)
            .data(coilData)
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        modifier = Modifier,
        contentScale = ContentScale.Fit
    )
}