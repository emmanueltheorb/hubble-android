package com.orb.hubble.ui.screens

import androidx.activity.SystemBarStyle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.orb.hubble.R
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.data.Message
import com.orb.hubble.data.Page
import com.orb.hubble.data.Post
import com.orb.hubble.data.Status
import com.orb.hubble.data.User
import com.orb.hubble.data.messageList
import com.orb.hubble.data.postList1
import com.orb.hubble.ui.components.InputField
import com.orb.hubble.ui.components.MessagesSection
import com.orb.hubble.ui.components.PostViewer
import com.orb.hubble.ui.components.VideoPlayer
import com.orb.hubble.ui.components.toColor
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Black2
import com.orb.hubble.ui.theme.Black3
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.GreyDrag
import com.orb.hubble.ui.theme.GreyDrag2
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White3
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ViewStatusScreen(
    modifier: Modifier = Modifier,
    pageId: String = "3",
    viewModel: HomeScreenViewModel = hiltViewModel(),
    onGoBack: () -> Unit
) {
    LaunchedEffect(pageId) {
        viewModel.setPostPageId(pageId)
    }

    val uiState by viewModel.state.collectAsState()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
//    val statuses = listOf(
//        Status(
//            pageId = "7043d14d-1922-423c-a24a-3c882693b323",
//            ownerId = "f0c7cfbc-604b-437e-bcce-2f6e17e063b7",
//            username = "valkyrie",
//            profilePic = "",
//            posts = postList1
//        )
//    )
    val isLoading by viewModel.isLoadingPosts.collectAsState()

    val messages by viewModel.messagesFromDb.collectAsStateWithLifecycle()
    //val messages = messageList

    val context = LocalActivity.current as ComponentActivity

    DisposableEffect(Unit) {
        context.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

        onDispose {
            context.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                ),
                navigationBarStyle = SystemBarStyle.dark(
                    android.graphics.Color.TRANSPARENT
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isLoading) {
            PostViewer(
                initialPageId = pageId,
                statuses = statuses,
                dataSourceFactory = viewModel.dataSourceFactory,
                messages = messages,
                onJoinPost = { postId, time ->
                    viewModel.joinPost(postId = postId, from = time)
                },
                onPreloadNextPost = { url, postType ->
                    viewModel.preloadNextPost(url = url, postType = postType)
                },
                onSendMessage = { content, postId, repliedTo ->
                    viewModel.sendMessage(content = content, postId = postId, repliedTo = repliedTo)
                },
                onGoBack = onGoBack
            )
        }
    }
}