package com.orb.hubble.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.orb.hubble.data.PickMediaScreenViewModel
import com.orb.hubble.ui.theme.Black1

@Composable
fun TestMediaScreen(
    modifier: Modifier = Modifier,
    viewModel: PickMediaScreenViewModel = hiltViewModel(),
    onMediaSelected: (String) -> Unit,
) {
    val pagingItems = viewModel.mediaPager.collectAsLazyPagingItems()

    LazyVerticalGrid(
        modifier = Modifier
            .background(color = Black1)
            .fillMaxSize(),
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
                    onMediaSelected = { uri ->
                        onMediaSelected(uri.toString())
                    }
                )
            } else {
                Box(Modifier.size(128.dp).background(Color.DarkGray))
            }
        }
    }
}