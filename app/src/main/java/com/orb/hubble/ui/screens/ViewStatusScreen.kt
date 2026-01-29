package com.orb.hubble.ui.screens

import android.util.Log
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orb.hubble.R
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.data.Message
import com.orb.hubble.data.Post
import com.orb.hubble.data.messageList
import com.orb.hubble.data.postsList10
import com.orb.hubble.data.postsList4
import com.orb.hubble.ui.components.DragState
import com.orb.hubble.ui.components.InputField
import com.orb.hubble.ui.components.MessagesSection
import com.orb.hubble.ui.components.toColor
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Black2
import com.orb.hubble.ui.theme.Black3
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.GreyDrag
import com.orb.hubble.ui.theme.GreyDrag2
import com.orb.hubble.ui.theme.NunitoFontFamily
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White3
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ViewStatusScreen(
    modifier: Modifier = Modifier,
    userId: String = "3",
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        viewModel.setPostUserId(userId)
    }

    val uiState by viewModel.state.collectAsState()
    val posts by viewModel.userPosts.collectAsState()
    //val posts = postsList4
    val isLoading by viewModel.isLoadingPosts.collectAsState()
    var currentIndex by remember { mutableIntStateOf(0) }

    //val messages by viewModel.messagesFromDb.collectAsStateWithLifecycle()
    val messages = messageList
    val textInput = remember { mutableStateOf("") }
    var repliedMessage: Message? by remember { mutableStateOf(null) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value.toInt()
    val screenHeight = LocalWindowInfo.current.containerSize.height.dp
    val bottomSheetHeight = screenHeight * 0.2655f

    val context = LocalActivity.current as ComponentActivity

    val showComments = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
//        confirmValueChange = { sheetValue ->
//            sheetValue != SheetValue.Hidden
//        }
    )

    LaunchedEffect(currentIndex) {
        if (!isLoading) {
            repliedMessage = null
            textInput.value = ""
            val time: Instant? = if (messages.isNotEmpty()) messages.last().insertedAt else null

            viewModel.joinPost(postId = posts[currentIndex].id, from = time)
        }
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
            .padding(top = topPadding.dp)
    ) {
        if (!isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.defaultMinSize(minHeight = 10.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        ProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.25.dp),
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
                                    if (!showComments.value) {
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
                                    } else {
                                        showComments.value = false
                                    }
                                })
                            }
                    )
                    BottomRow(
                        bottomPadding = bottomPadding,
                        onCommentsClicked = {
                            showComments.value = true
                        },
                        post = posts[currentIndex]
                    )
                }

                AnimatedVisibility(
                    showComments.value,
                    modifier = Modifier
                        .align(Alignment.BottomCenter),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing)
                    )
                    ) {
                    MyBottomSheet(
                        modifier = Modifier
                            .align(Alignment.BottomCenter),
                        sheetHeight = bottomSheetHeight,
                        bottomPadding = bottomPadding,
                        onDismiss = { showComments.value = false },
                        dragHandle = { CommentsDragHandle() }
                    ) {
                        val controller = LocalSoftwareKeyboardController.current

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 45.dp)
                                .height(bottomSheetHeight)
                        ) {
                            MessagesSection(
                                messages = messages,
                                bottomPadding = if (repliedMessage != null) 87.1.dp else 55.dp,
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
                                            .padding(horizontal = 13.dp)
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "@${repliedMessage!!.user.username}",
                                            fontWeight = FontWeight.Bold,
                                            style = Typography.bodySmall,
                                            color = repliedMessage!!.user.color.toColor()
                                        )
                                        Spacer(Modifier.width(4.5.dp))
                                        Text(
                                            text = repliedMessage!!.content,
                                            color = White3,
                                            style = Typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { repliedMessage = null },
                                            shape = CircleShape,
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = White1
                                            ),
                                            modifier = Modifier.size(19.8.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_close),
                                                contentDescription = "cancel reply",
                                                tint = Black1,
                                                modifier = Modifier.size(17.1.dp)
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

//                if (showComments.value) {
//                    ModalBottomSheet(
//                        sheetState = sheetState,
//                        sheetMaxWidth = 900.dp,
//                        containerColor = Black2,
//                        dragHandle = {
//                            CommentsDragHandle()
//                        },
//                        onDismissRequest = { showComments.value = false }
//                    ) {
//                        val view = LocalView.current
//                        (view.parent as? DialogWindowProvider)?.window?.let { window ->
//                            SideEffect {
//                                WindowCompat.getInsetsController(
//                                    window,
//                                    view
//                                ).isAppearanceLightNavigationBars = false
//                            }
//                        }
//                        val controller = LocalSoftwareKeyboardController.current
//
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(bottomSheetHeight)
//                        ) {
//                            MessagesSection(
//                                messages = messages,
//                                bottomPadding = if (repliedMessage != null) 87.1.dp else 55.dp,
//                                onMessageDragged = { message ->
//                                    controller?.show()
//                                    repliedMessage = message
//                                    focusRequester.freeFocus()
//                                    focusManager.clearFocus()
//                                    focusRequester.requestFocus()
//                                }
//                            )
//                            Column(
//                                modifier = Modifier
//                                    .align(Alignment.BottomCenter)
//                            ) {
//                                if (repliedMessage != null) {
//                                    Row(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .background(Grey2)
//                                            .padding(horizontal = 13.dp)
//                                            .padding(vertical = 6.dp),
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Text(
//                                            text = "@${repliedMessage!!.user.username}",
//                                            fontWeight = FontWeight.Bold,
//                                            style = Typography.bodySmall,
//                                            color = repliedMessage!!.user.color.toColor()
//                                        )
//                                        Spacer(Modifier.width(4.5.dp))
//                                        Text(
//                                            text = repliedMessage!!.content,
//                                            color = White3,
//                                            style = Typography.bodySmall,
//                                            maxLines = 1,
//                                            overflow = TextOverflow.Ellipsis,
//                                            modifier = Modifier.weight(1f)
//                                        )
//                                        IconButton(
//                                            onClick = { repliedMessage = null },
//                                            shape = CircleShape,
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                containerColor = White1
//                                            ),
//                                            modifier = Modifier.size(19.8.dp)
//                                        ) {
//                                            Icon(
//                                                painter = painterResource(R.drawable.ic_close),
//                                                contentDescription = "cancel reply",
//                                                tint = Black1,
//                                                modifier = Modifier.size(17.1.dp)
//                                            )
//                                        }
//                                    }
//                                }
//                                HorizontalDivider(
//                                    color = Grey2
//                                )
//                                InputField(
//                                    textValue = textInput,
//                                    onSendClicked = {
//                                        viewModel.sendMessage(
//                                            content = textInput.value,
//                                            postId = posts[currentIndex].id,
//                                            userId = userId,
//                                            repliedTo = repliedMessage?.id
//                                        )
//                                        textInput.value = ""
//                                        repliedMessage = null
//                                    },
//                                    focusRequester = focusRequester
//                                )
//                            }
//                        }
//                    }
//                }
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
        val gapWidth = 5.4.dp.toPx() // Gap between segments
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
    bottomPadding: Int,
    onCommentsClicked: () -> Unit,
    post: Post
) {
    Box(
        modifier = modifier
            .background(Black3)
            .padding(bottom = bottomPadding.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(55.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(
                color = Grey2,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = modifier.size(40.5.dp),
                    color = Color(0xFF3F3F3F),
                    shape = CircleShape
                ) {
                    Image(
                        painter = painterResource(R.drawable.place_holder_pic),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                }
                Text(
                    text = "Asguardian",//post.user.username,
                    color = Color.White,
                    modifier = modifier.padding(horizontal = 15.dp)
                )
                Surface(
                    modifier = modifier
                        .size(17.dp),
                    color = "#D1A6F0".toColor(),//post.user.color.toColor(),
                    shape = CircleShape
                ) {
                    Image(
                        modifier = Modifier.padding(2.dp),
                        painter = painterResource(R.drawable.plus_icon_thick),
                        contentScale = ContentScale.Fit,
                        contentDescription = "Follow user"
                    )
                }
                Spacer(modifier = modifier.weight(1f))
                IconButton(onCommentsClicked) {
                    Icon(
                        painter = painterResource(R.drawable.chat_hb_filled),
                        contentDescription = "View comments Button",
                        tint = Color.White,
                        modifier = modifier.size(45.dp)
                    )
                }
            }

        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Black2)
            .height(45.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            color = Grey2,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
        Surface(
            modifier = Modifier
                .width(33.dp)
                .height(5.dp),
            shape = RoundedCornerShape(5.dp),
            color = GreyDrag
        ) {

        }
        Image(
            painter = painterResource(R.drawable.at_icon_3),
            contentDescription = "Click to see who replied you",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(color = GreyDrag2),
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-40).dp)
        )
        Image(
            painter = painterResource(R.drawable.three_dot_icon),
            contentDescription = "Comments drop down menu",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(color = GreyDrag2),
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-15).dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBottomSheet(
    modifier: Modifier = Modifier,
    sheetHeight: Dp,
    bottomPadding: Int,
    sheetColor: Color = Black2,
    onDismiss: () -> Unit,
    dragHandle: @Composable (() -> Unit),
    content: @Composable (() -> Unit)
) {
    val density = LocalDensity.current
    val sheetHeightPx = with(density) { sheetHeight.toPx() }

    val dragState = remember {
        AnchoredDraggableState(
            initialValue = SheetPosition.Expanded,
            anchors = DraggableAnchors {
                SheetPosition.Expanded at 0f
                SheetPosition.Hidden at sheetHeightPx
            }
        )
    }

    LaunchedEffect(dragState) {
        snapshotFlow { dragState.settledValue }
            .collectLatest { settledState ->
                if (settledState != SheetPosition.Expanded) {
                    onDismiss()
                }
            }
    }

    BackHandler {
        onDismiss()
    }

    Surface(
        modifier = modifier
            .height(sheetHeight)
            .offset {
                IntOffset(
                    x = 0,
                    y = dragState.requireOffset().toInt().coerceAtLeast(0)
                )
            }
            .anchoredDraggable(
                state = dragState,
                orientation = Orientation.Vertical
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = sheetColor
    ) {
        Box(
            modifier = modifier
                .padding(bottom = bottomPadding.dp)
        ) {
            content()
            dragHandle()
        }
    }
}

enum class SheetPosition { Expanded, Hidden }