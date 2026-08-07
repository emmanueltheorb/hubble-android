package com.orb.hubble.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.datasource.DataSource
import com.orb.hubble.data.Message
import com.orb.hubble.data.Status
import com.orb.hubble.ui.components.crossPager.CrossPager
import com.orb.hubble.ui.components.crossPager.animateToNextPage
import com.orb.hubble.ui.components.crossPager.animateToPreviousPage
import com.orb.hubble.ui.components.crossPager.rememberCrossPagerState
import kotlinx.coroutines.launch
import kotlin.time.Instant

@Composable
fun PostViewer(
    modifier: Modifier = Modifier,
    initialPageId: String?,
    postId: String?,
    statuses: List<Status>,
    dataSourceFactory: DataSource.Factory,
    messages: List<Message>,
    messageId: String?,
    onJoinPost: (String, Instant?) -> Unit,
    onPreloadNextPost: (String?, String) -> Unit,
    onSendMessage: (String, String, String?) -> Unit,
    onGoBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val initialIndex = remember(statuses) {
        statuses.indexOfFirst { it.pageId == initialPageId }.coerceAtLeast(0)
    }

    var currentPageIndex by remember(statuses) { mutableIntStateOf(initialIndex) }

    val crossPagerState = key(statuses) {
        rememberCrossPagerState(initialPage = initialIndex) { statuses.size }
    }

    var isSlidingNext by remember { mutableStateOf(true) }

    fun goToNextPage() {
        if (currentPageIndex < statuses.size - 1) {
            isSlidingNext = true
            scope.launch {
                crossPagerState.animateToNextPage()
            }
            currentPageIndex++
        }
    }

    fun goToPrevPage() {
        if (currentPageIndex > 0) {
            isSlidingNext = false
            scope.launch {
                crossPagerState.animateToPreviousPage()
            }
            currentPageIndex--
        }
    }

//    Box(
//        modifier = modifier
//            .fillMaxSize()
//            .pointerInput(Unit) {
//                detectDragGestures(
//                    onDragEnd = { /* Reset states if needed or handle drag momentum*/}
//                ) { change, dragAmount ->
//                    change.consume()
//
//                    val (x,y) = dragAmount
//                    val threshold = 50f
//
//                    when {
//                        y < -threshold -> goToNextPage()
//                        y > threshold -> goToPrevPage()
//
//                        x < -threshold -> goToNextPage()
//                        x > threshold -> goToPrevPage()
//                    }
//                }
//            }
//    ) {
//        AnimatedContent(
//            targetState = currentPageIndex,
//            transitionSpec = {
//                if (isSlidingNext) {
//                    slideInVertically(
//                        initialOffsetY = { it },
//                        animationSpec = tween(400)
//                    ) togetherWith slideOutVertically(
//                        targetOffsetY = { -it },
//                        animationSpec = tween(400)
//                    )
//                } else {
//                    slideInVertically(
//                        initialOffsetY = { -it },
//                        animationSpec = tween(400)
//                    ) togetherWith slideOutVertically(
//                        targetOffsetY = { it },
//                        animationSpec = tween(400)
//                    )
//                }
//            },
//            label = "PostsPageTransition"
//        ) { targetIndex ->
//            val currentStatus = statuses.getOrNull(targetIndex)
//
//            if (currentStatus != null) {
//                key(currentStatus.pageId) {
//                    PostView(
//                        posts = currentStatus.posts,
//                        messages = messages,
//                        messageId = messageId,
//                        onGoToNextPage = { goToNextPage() },
//                        onGoToPrevPage = { goToPrevPage() },
//                        dataSourceFactory = dataSourceFactory,
//                        onJoinPost = onJoinPost,
//                        onPreloadNextPost = onPreloadNextPost,
//                        onSendMessage = onSendMessage,
//                        onGoBack = onGoBack
//                    )
//                }
//            }
//        }
//    }

    CrossPager(
        state = crossPagerState
    ) { index ->
        val currentStatus = statuses.getOrNull(index)

        if (currentStatus != null) {
            PostView(
                posts = currentStatus.posts,
                messages = messages,
                messageId = messageId,
                onGoToNextPage = { goToNextPage() },
                onGoToPrevPage = { goToPrevPage() },
                dataSourceFactory = dataSourceFactory,
                onJoinPost = onJoinPost,
                onPreloadNextPost = onPreloadNextPost,
                onSendMessage = onSendMessage,
                onGoBack = onGoBack
            )
        }
    }
}