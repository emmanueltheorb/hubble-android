package com.orb.hubble.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.orb.hubble.R
import com.orb.hubble.data.Message
import com.orb.hubble.data.MessageWithPostAndReply
import com.orb.hubble.data.ReplyLogViewModel
import com.orb.hubble.ui.components.DragState
import com.orb.hubble.ui.components.InputField
import com.orb.hubble.ui.components.MessageInput
import com.orb.hubble.ui.components.PostView
import com.orb.hubble.ui.components.toColor
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.NunitoFontFamily
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White3
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@UnstableApi
@Composable
fun ReplyLogScreen(
    modifier: Modifier = Modifier,
    viewModel: ReplyLogViewModel = hiltViewModel()
) {
    val postList by viewModel.postList.collectAsState()
    val messageList by viewModel.messageList.collectAsState()
    val postMessages by viewModel.messageListForPost.collectAsStateWithLifecycle()

    val isLoadingMessageList by viewModel.isLoadingMessageList.collectAsStateWithLifecycle()
    val isLoadingPosts by viewModel.isLoadingPosts.collectAsStateWithLifecycle()
    var showPostView by remember { mutableStateOf(false) }
    var showInputField by remember { mutableStateOf(false) }
    var messageClicked by remember { mutableStateOf<String?>(null) }

    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value.toInt()

    val scope = rememberCoroutineScope()

    val textInput = remember { mutableStateOf("") }
    var repliedMessage: Message? by remember { mutableStateOf(null) }

    val controller = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    if (!isLoadingMessageList) {
        Box(
            modifier = modifier
                .background(Black1)
                .statusBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier.height(900.dp)
            ) {
                items(messageList) { message ->
                    ReplyLogMessageItem(
                        message = message,
                        onMessageClicked = { message ->
                            viewModel.setPostId(message.postId)
                            scope.launch {
                                snapshotFlow { isLoadingPosts }.first { isLoading ->
                                    !isLoading
                                }
                                delay(500)
                                messageClicked = message.id
                                showPostView = true
                            }
                        },
                        onMessageDragged = { message ->
                            showInputField = true
                            controller?.show()
                            repliedMessage = message
                            focusRequester.freeFocus()
                            focusManager.clearFocus()
                            focusRequester.requestFocus()
                        }
                    )
                }
            }

            if (showInputField) {
                MessageInput(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding(),
                    textInput = textInput,
                    repliedMessage = repliedMessage,
                    onCancelReplyClicked = {
                        repliedMessage = null
                        textInput.value = ""
                        showInputField = false
                    },
                    onSendClicked = {
                        if (repliedMessage != null) {
                            viewModel.sendMessage(
                                textInput.value,
                                repliedMessage?.postId!!,
                                repliedMessage?.id
                            )
                        }
                    },
                    bottomPadding = bottomPadding,
                    focusRequester = focusRequester
                )
            }

            if (showPostView) {
                Box {
                    BackHandler {
                        showPostView = false
                    }
                    PostView(
                        dataSourceFactory = viewModel.dataSourceFactory,
                        posts = postList,
                        messages = postMessages,
                        startIndex = 0,
                        messageId = messageClicked,
                        onGoToNextPage = {},
                        onGoToPrevPage = {},
                        onJoinPost = { postId, time ->

                        },
                        onPreloadNextPost = { contentUrl, postType ->

                        },
                        onSendMessage = { messageContent, postId, repliedTo ->

                        },
                        onGoBack = {
                            showPostView = false
                        },
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun ReplyLogMessageItem(
    modifier: Modifier = Modifier,
    message: MessageWithPostAndReply,
    onMessageClicked: (Message) -> Unit,
    onMessageDragged: (Message) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 7.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val dragState = remember {
        AnchoredDraggableState(
            initialValue = DragState.Idle,
            anchors = DraggableAnchors {
                DragState.Left at -225f
                DragState.Idle at 0f
                DragState.Right at 225f
            }
        )
    }

    LaunchedEffect(dragState) {
        snapshotFlow { dragState.settledValue }
            .collectLatest { settledState ->
                if (settledState != DragState.Idle) {
                    onMessageDragged(message.message)
                    dragState.animateTo(
                        DragState.Idle
                    )
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onMessageClicked(message.message) })
            .padding(top = 2.7.dp)
            .padding(bottom = 12.dp)
            .padding(horizontal = 12.dp)
            .pointerInput(Unit) {
                var totalHorizontalDrag = 0f
                var isDragTriggered = false

                detectHorizontalDragGestures(
                    onDragStart = {
                        totalHorizontalDrag = 0f
                        isDragTriggered = false
                    },
                    onDragEnd = {
                        coroutineScope.launch { dragState.settle(spring()) }
                    },
                    onDragCancel = {
                        coroutineScope.launch { dragState.settle(spring()) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!isDragTriggered) {
                            totalHorizontalDrag += dragAmount
                            if (abs(totalHorizontalDrag) > dragThresholdPx) {
                                isDragTriggered = true
                            }
                        }

                        if (isDragTriggered) {
                            dragState.dispatchRawDelta(dragAmount)
                            change.consume()
                        }
                    }
                )
            }
            .offset {
                IntOffset(
                    x = dragState.requireOffset().roundToInt(),
                    y = 0
                )
            }
    ) {
        if (message.repliedMessage != null) {
            val userName = "@${message.repliedMessage.user.username} "
            val content = message.repliedMessage.content
            val annotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        //fontStyle = Typography.bodySmall,
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = message.repliedMessage.user.color.toColor()
                    )
                ) {
                    pushStringAnnotation(
                        tag = userName,
                        annotation = userName
                    )
                    append(userName)
                }
                withStyle(
                    style = SpanStyle(
                        //fontStyle = Typography.bodySmall,
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = White3
                    )
                ) {
                    pushStringAnnotation(
                        tag = content,
                        annotation = content
                    )
                    append(content)
                }
            }
            Row(
                modifier = Modifier
                    .padding(start = 54.dp)
            ) {
                Text(
                    text = annotatedString,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(40.5.dp),
                color = Color(0xFF3F3F3F),
                shape = CircleShape
            ) {

            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .padding(top = 2.7.dp)
                    .wrapContentHeight()
            ) {
                Text(
                    text = message.message.user.username,
                    style = Typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    color = message.message.user.color.toColor()
                )
                Text(
                    text = message.message.content,
                    color = White3,
                    style = Typography.bodySmall
                )
            }
            Spacer(Modifier.width(12.dp))
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .padding(end = 5.dp)
            ) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    color = if (message.postSummary.backgroundColor == null) Color(0xFF3F3F3F) else message.postSummary.backgroundColor.toColor(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    if (message.postSummary.contentText != null) {
                        BasicText(
                            text = message.postSummary.contentText,
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 8.sp,
                                maxFontSize = 16.sp
                            ),
                            maxLines = 3,
                            color = { Color.White }
                        )
                    } else if (message.postSummary.contentUrl != null) {
                        val model = remember(message.postSummary.contentUrl) {
                            ImageRequest.Builder(context)
                                .data(message.postSummary.contentUrl)
                                .precision(Precision.INEXACT)
                                .size(30, 30)
                                .crossfade(true)
                                .build()
                        }

                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            clipToBounds = true
                        )
                    }
                }
            }
        }
    }
}

//NativeAlloc concurrent copying GC freed 2249KB AllocSpace bytes, 0(0B) LOS objects, 49% free, 11MB/22MB, paused 289us,55us total 112.459ms
//2026-03-06 09:06:19.682  1658-15241 OpenGLRenderer          com.orb.hubble                       I  Davey! duration=1499ms; Flags=0, FrameTimelineVsyncId=47269742, IntendedVsync=153229653763774, Vsync=153230453097406, InputEventId=0, HandleInputStart=153230454098420, AnimationStart=153230454105555, PerformTraversalsStart=153231038073159, DrawStart=153231118189461, FrameDeadline=153230436462966, FrameInterval=153230453403316, FrameStartTime=16652784, SyncQueued=153231138536128, SyncStart=153231138615503, IssueDrawCommandsStart=153231138786180, SwapBuffers=153231139542690, FrameCompleted=153231153096701, DequeueBufferDuration=23750, QueueBufferDuration=370104, GpuCompleted=153231153096701, SwapBuffersCompleted=153231140374097, DisplayPresentTime=0, CommandSubmissionCompleted=153231139542690,
//2026-03-06 09:06:19.697  1658-1658  Compatibil...geReporter com.orb.hubble                       D  Compat change id reported: 171228096; UID 10473; state: ENABLED
//2026-03-06 09:06:19.721  1658-1658  Choreographer           com.orb.hubble                       I  Skipped 43 frames!  The application may be doing too much work on its main thread.
//2026-03-06 09:06:19.903  1658-15241 OpenGLRenderer          com.orb.hubble                       I  Davey! duration=908ms; Flags=0, FrameTimelineVsyncId=47270122, IntendedVsync=153230469702192, Vsync=153231185760423, InputEventId=0, HandleInputStart=153231196773680, AnimationStart=153231196780920, PerformTraversalsStart=153231291341805, DrawStart=153231312696544, FrameDeadline=153231185833784, FrameInterval=153231196269461, FrameStartTime=16652517, SyncQueued=153231337832430, SyncStart=153231337910659, IssueDrawCommandsStart=153231338113419, SwapBuffers=153231358476336, FrameCompleted=153231378231805, DequeueBufferDuration=25157, QueueBufferDuration=465573, GpuCompleted=153231378231805, SwapBuffersCompleted=153231359761805, DisplayPresentTime=0, CommandSubmissionCompleted=153231358476336,
//2026-03-06 09:06:21.964  1658-15266 OpenGLRenderer          com.orb.hubble                       I  Davey! duration=909ms; Flags=0, FrameTimelineVsyncId=47271224, IntendedVsync=153232517884535, Vsync=153232967496338, InputEventId=0, HandleInputStart=153232980923158, AnimationStart=153232980930190, PerformTraversalsStart=153233397506544, DrawStart=153233397628054, FrameDeadline=153233000762074, FrameInterval=153232980522429, FrameStartTime=16652289, SyncQueued=153233415150346, SyncStart=153233415252742, IssueDrawCommandsStart=153233415498575, SwapBuffers=153233416500606, FrameCompleted=153233427709200, DequeueBufferDuration=33281, QueueBufferDuration=412448, GpuCompleted=153233427709200, SwapBuffersCompleted=153233417515033, DisplayPresentTime=153224224185089, CommandSubmissionCompleted=153233416500606,
//2026-03-06 09:06:22.008  1658-1658  Choreographer           com.orb.hubble                       I  Skipped 30 frames!  The application may be doing too much work on its main thread.
//2026-03-06 09:06:22.382  1658-15172 OpenGLRenderer          com.orb.hubble                       I  Davey! duration=866ms; Flags=0, FrameTimelineVsyncId=47271450, IntendedVsync=153232984054539, Vsync=153233483592339, InputEventId=0, HandleInputStart=153233484486127, AnimationStart=153233484497273, PerformTraversalsStart=153233666025919, DrawStart=153233811308366, FrameDeadline=153233450427096, FrameInterval=153233483709512, FrameStartTime=16651260, SyncQueued=153233830325502, SyncStart=153233830409564, IssueDrawCommandsStart=153233830616491, SwapBuffers=153233833529096, FrameCompleted=153233850870710, DequeueBufferDuration=24114, QueueBufferDuration=419896, GpuCompleted=153233850870710, SwapBuffersCompleted=153233834737794, DisplayPresentTime=153224240841443, CommandSubmissionCompleted=153233833529096,
//2026-03-06 09:06:24.946  1658-15172 OpenGLRenderer          com.orb.hubble                       I  Davey! duration=1578ms; Flags=0, FrameTimelineVsyncId=47272451, IntendedVsync=153234832556280, Vsync=153235248883405, InputEventId=0, HandleInputStart=153235261566699, AnimationStart=153235261573939, PerformTraversalsStart=153236220095032, DrawStart=153236385076282, FrameDeadline=153235282209081, FrameInterval=153235261120449, FrameStartTime=16653085, SyncQueued=153236398569928, SyncStart=153236398850084, IssueDrawCommandsStart=153236399054407, SwapBuffers=153236400411386, FrameCompleted=153236411297428, DequeueBufferDuration=25261, QueueBufferDuration=309219, GpuCompleted=153236411297428, SwapBuffersCompleted=153236401287324, DisplayPresentTime=153224506958422, CommandSubmissionCompleted=153236400411386,
//2026-03-06 09:06:24.958  1658-1658  Choreographer           com.orb.hubble                       I  Skipped 70 frames!  The application may be doing too much work on its main thread.
//2026-03-06 09:06:24.970  1658-15049 com.orb.hubble          com.orb.hubble                       I  NativeAlloc concurrent copying GC freed 4821KB AllocSpace bytes, 0(0B) LOS objects, 49% free, 10MB/21MB, paused 184us,192us total 114.802ms
//2026-03-06 09:06:25.561  1658-15266 OpenGLRenderer          com.orb.hubble                       I  Davey! duration=1761ms; Flags=0, FrameTimelineVsyncId=47272663, IntendedVsync=153235265311893, Vsync=153236430807903, InputEventId=0, HandleInputStart=153236434337376, AnimationStart=153236434349511, PerformTraversalsStart=153236997907688, DrawStart=153236998004511, FrameDeadline=153236431266021, FrameInterval=153236433255188, FrameStartTime=16649943, SyncQueued=153237014545032, SyncStart=153237014761699, IssueDrawCommandsStart=153237014910917, SwapBuffers=153237015817428, FrameCompleted=153237026994824, DequeueBufferDuration=26614, QueueBufferDuration=427032, GpuCompleted=153237026994824, SwapBuffersCompleted=153237016860396, DisplayPresentTime=153224523584828, CommandSubmissionCompleted=153237015817428,
//2026-03-06 09:06:25.614  1658-1658  Choreographer           com.orb.hubble                       I  Skipped 38 frames!  The application may be doing too much work on its main thread.


//2026-03-03 18:22:51.227 29191-29191 DragManager             com.orb.hubble                       D  DecorView return
//2026-03-03 18:22:51.242 29191-29191 DragManager             com.orb.hubble                       D  DecorView return
//2026-03-03 18:22:51.257 29191-29191 DragManager             com.orb.hubble                       D  DecorView return