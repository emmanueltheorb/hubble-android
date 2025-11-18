package com.orb.hubble.ui.screens

import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.orb.hubble.R
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.data.Message
import com.orb.hubble.data.Post
import com.orb.hubble.data.messageList
import com.orb.hubble.data.postsList10
import com.orb.hubble.ui.components.InputField
import com.orb.hubble.ui.components.MessagesSection
import com.orb.hubble.ui.components.toColor
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Black2
import com.orb.hubble.ui.theme.Blue1
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ViewStatusScreen(
    modifier: Modifier = Modifier,
    userId: Int,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        Log.d("DEBUG", "ViewStatusScreen ViewModel: $viewModel")
    }

    val uiState by viewModel.uiState.collectAsState()
    val posts = postsList10
    var currentIndex by remember { mutableIntStateOf(0) }

    val messages = messageList
    val textInput = remember { mutableStateOf("") }
    var repliedMessage: Message? by remember { mutableStateOf(null) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value.toInt()
    val screenHeight = LocalWindowInfo.current.containerSize.height.dp
    val bottomSheetHeight = screenHeight * 0.295f

    val context = LocalActivity.current as ComponentActivity

    val showComments = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(currentIndex) {
        repliedMessage = null
        textInput.value = ""
        val time: Instant? = if (messages.isNotEmpty()) messages.last().insertedAt else null

        viewModel.joinPost(postId = posts[currentIndex].id, from = time)
    }

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

        onDispose { }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(bottom = bottomPadding.dp)
            .padding(top = topPadding.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.defaultMinSize(minHeight = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                ProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    currentIndex = currentIndex,
                    posts = posts
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                if (currentIndex > 0) {
                                    currentIndex--
                                }
                            } else {
                                if (currentIndex < posts.size - 1) {
                                    currentIndex++
                                }
                            }
                        })
                    }
            )
            BottomRow(
                onCommentsClicked = {
                    showComments.value = true
                },
                post = posts[currentIndex]
            )
        }

        if (showComments.value) {
            ModalBottomSheet(
                sheetState = sheetState,
                sheetMaxWidth = 900.dp,
                containerColor = Black2,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                onDismissRequest = { showComments.value = false }
            ) {
                val view = LocalView.current
                (view.parent as? DialogWindowProvider)?.window?.let { window ->
                    SideEffect {
                        WindowCompat.getInsetsController(
                            window,
                            view
                        ).isAppearanceLightNavigationBars = false
                    }
                }
                val controller = LocalSoftwareKeyboardController.current

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomSheetHeight)
                ) {
                    MessagesSection(
                        messages = messages,
                        bottomPadding = if (repliedMessage != null) 99.dp else 60.dp,
                        onMessageDragged = { message ->
                            controller?.show()
                            repliedMessage = message
                            focusRequester.freeFocus()
                            focusManager.clearFocus()
                            focusRequester.requestFocus()
                        }
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {
                        if (repliedMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Grey2)
                                    .padding(horizontal = 15.dp)
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "@${repliedMessage!!.user.username}",
                                    fontWeight = FontWeight.Bold,
                                    style = Typography.bodySmall,
                                    color = repliedMessage!!.user.color.toColor()
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = repliedMessage!!.content,
                                    color = White1,
                                    style = Typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.wrapContentWidth()
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { repliedMessage = null },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = White1
                                    ),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = "cancel reply",
                                        tint = Black1,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = Grey2
                        )
                        InputField(
                            textValue = textInput,
                            onSendClicked = {
                                viewModel.sendMessage(
                                    content = textInput.value,
                                    postId = posts[currentIndex].id,
                                    userId = userId,
                                    repliedTo = repliedMessage?.id
                                )
                                textInput.value = ""
                                repliedMessage = null
                            },
                            focusRequester = focusRequester
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    posts: List<Post>
) {
    val activeColor = Color.White
    val inactiveColor = Color.White.copy(alpha = 0.4f)

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val segmentCount = posts.size
        val gapWidth = 6.dp.toPx() // Gap between segments
        val totalGapWidth = gapWidth * (segmentCount + 1)
        val availableWidth = totalWidth - totalGapWidth
        val segmentWidth = availableWidth / segmentCount

        posts.forEachIndexed { index, post ->
            val startX = gapWidth + index * (segmentWidth + gapWidth)
            val endX = startX + segmentWidth

            val color = if (index <= currentIndex) activeColor else inactiveColor

            // Draw the segment background (inactive)
            drawLine(
                color = color,
                start = Offset(startX, size.height / 2),
                end = Offset(endX, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun BottomRow(
    modifier: Modifier = Modifier,
    onCommentsClicked: () -> Unit,
    post: Post
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = modifier.size(45.dp),
            color = Color(0xFF3F3F3F),
            shape = CircleShape
        ) {

        }
        Text(
            text = post.user.username,
            modifier = modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = modifier.weight(1f))
        IconButton(onCommentsClicked) {
            Icon(
                painter = painterResource(R.drawable.chat_hb_filled),
                contentDescription = "View comments Button",
                tint = Color.White,
                modifier = modifier.size(50.dp)
            )
        }
    }
}