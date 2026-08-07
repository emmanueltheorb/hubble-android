package com.orb.hubble.ui.components

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.emoji2.text.EmojiCompat
import androidx.media3.datasource.DataSource
import com.orb.hubble.R
import com.orb.hubble.data.Message
import com.orb.hubble.data.Post
import com.orb.hubble.data.PostType
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Black2
import com.orb.hubble.ui.theme.Black3
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.GreyDrag
import com.orb.hubble.ui.theme.GreyDrag2
import com.orb.hubble.ui.theme.GreyFollow
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White3
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Composable
fun PostView(
    modifier: Modifier = Modifier,
    dataSourceFactory: DataSource.Factory,
    posts: List<Post>,
    messages: List<Message>,
    messageId: String? = null,
    startIndex: Int? = null,
    onGoToNextPage: () -> Unit,
    onGoToPrevPage: () -> Unit,
    onJoinPost: (postId: String, time: Instant?) -> Unit,
    onPreloadNextPost: (url: String?, postType: String) -> Unit,
    onSendMessage: (content: String, postId: String, repliedTo: String?) -> Unit,
    onGoBack: () -> Unit
) {
    val initialPostIndex = remember(posts) {
        if (startIndex == null) {
            val firstUnviewed = posts.indexOfFirst { !it.viewed }
            if (firstUnviewed != -1) firstUnviewed else 0
        } else {
            startIndex
        }
    }

    var currentIndex by remember { mutableIntStateOf(initialPostIndex) }

    val textInput = remember { mutableStateOf("") }
    var repliedMessage: Message? by remember { mutableStateOf(null) }

    val controller = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val screenHeight = LocalWindowInfo.current.containerSize.height.dp
    val bottomSheetHeight = screenHeight * 0.2655f

    val context = LocalActivity.current as ComponentActivity

    val showComments = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (messageId != null) {
            showComments.value = true
            delay(200.milliseconds)
            if (messages.isNotEmpty()) {
                val index = messages.indexOfFirst { it.id == messageId }
                repliedMessage = messages[index]
            }
            controller?.show()
            focusRequester.freeFocus()
            focusManager.clearFocus()
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(currentIndex) {
        repliedMessage = null
        textInput.value = ""
        val time: Instant? = if (messages.isNotEmpty()) messages.last().insertedAt else null

        onJoinPost(posts[currentIndex].id, time)
        val nextIndex = currentIndex + 1
        if (nextIndex < posts.size) {
            val nextPost = posts[nextIndex]
            onPreloadNextPost(nextPost.contentUrl, nextPost.type)
        }
    }

//    DisposableEffect(Unit) {
//        context.enableEdgeToEdge(
//            navigationBarStyle = SystemBarStyle.dark(
//                android.graphics.Color.TRANSPARENT
//            )
//        )
//
//        onDispose {
//            context.enableEdgeToEdge(
//                navigationBarStyle = SystemBarStyle.dark(
//                    android.graphics.Color.TRANSPARENT
//                )
//            )
//        }
//    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            //.padding(top = topPadding.dp)
    ) {
        val currentPost = posts[currentIndex]
        if (currentPost.type.equals(PostType.VIDEO.name, ignoreCase = true)) {
            VideoPlayer(
                videoUrl = currentPost.contentUrl!!,
                thumbnailString = currentPost.thumbnailString,
                description = currentPost.description,
                dataSourceFactory = dataSourceFactory,
                paddingTop = 1.dp,
                //paddingBottom = if (bottomPadding == 0) 60.dp else 55.dp + bottomPadding.dp,
                paddingBottom = 55.dp,
                onGoLeft = {
                    if (!showComments.value) {
                        if (currentIndex > 0) {
                            currentIndex--
                        } else {
                            onGoToPrevPage()
                        }
                    } else {
                        showComments.value = false
                    }
                },
                onGoRight = {
                    if (!showComments.value) {
                        if (currentIndex < posts.size - 1) {
                            currentIndex++
                        } else {
                            onGoToNextPage()
                        }
                    } else {
                        showComments.value = false
                    }
                },
                onCenterTapped = {
//                    if (showComments.value) {
//                        showComments.value = false
//                    }
                },
                pausePlay = messageId != null
            )
        }
        if (currentPost.type.equals(PostType.IMAGE.name, ignoreCase = true)) {
            ImageViewer(
                imageUrl = currentPost.contentUrl!!,
                thumbnailString = currentPost.thumbnailString,
                paddingTop = 1.dp,
                paddingBottom = 55.dp,
                onGoLeft = {
                    if (!showComments.value) {
                        if (currentIndex > 0) {
                            currentIndex--
                        } else {
                            onGoToPrevPage()
                        }
                    } else {
                        showComments.value = false
                    }
                },
                onGoRight = {
                    if (!showComments.value) {
                        if (currentIndex < posts.size - 1) {
                            currentIndex++
                        } else {
                            onGoToNextPage()
                        }
                    } else {
                        showComments.value = false
                    }
                }
            )
        }
        if (currentPost.type.equals(PostType.TEXT.name, ignoreCase = true)) {
            TextPostViewer(
                contentText = currentPost.contentText!!,
                backgroundColor = Color.Cyan.copy(alpha = 0.8f),
                topPadding = 1.dp,
                bottomPadding = 55.dp,
                onGoLeft = {
                    if (!showComments.value) {
                        if (currentIndex > 0) {
                            currentIndex--
                        } else {
                            onGoToPrevPage()
                        }
                    } else {
                        showComments.value = false
                    }
                },
                onGoRight = {
                    if (!showComments.value) {
                        if (currentIndex < posts.size - 1) {
                            currentIndex++
                        } else {
                            onGoToNextPage()
                        }
                    } else {
                        showComments.value = false
                    }
                }
            )
        }
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
                        .height(1.dp),
                    //.height(2.25.dp),
                    currentIndex = currentIndex,
                    posts = posts
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .padding(horizontal = 2.dp) //formerly 15.dp
                        .pointerInput(Unit) {
                            detectTapGestures {
                                /* do nothing */
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBox(
                        iconId = R.drawable.go_back_left_icon,
                        contentDescription = "go back to home screen",
                        boxSize = 35,
                        onIconClicked = onGoBack
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconBox(
                        iconId = R.drawable.more_vert1,
                        contentDescription = "viewing status menu",
                        boxSize = 35,
                        onIconClicked = {}
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            BottomRow(
                onCommentsClicked = {
                    showComments.value = true
                },
                post = posts[currentIndex]
            )
        }

        if (showComments.value) {
            val alpha by
            animateFloatAsState(
                targetValue = if (showComments.value) 0.32f else 0f,
                animationSpec = tween(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = SolidColor(Color.Black), alpha = alpha)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (showComments.value) {
                                showComments.value = false
                            }
                        }
                    }
            )
        }

        AnimatedVisibility(
            showComments.value,
            modifier = Modifier
                .align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
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
                onDismiss = { showComments.value = false },
                dragHandle = { CommentsDragHandle() }
            ) {
                //keyboard controller was here

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 45.dp)
                        .imePadding()
                        .height(bottomSheetHeight)
                ) {
                    MessagesSection(
                        messages = messages,
                        messageId = messageId,
                        bottomPadding = if (repliedMessage != null) {
                            87.1.dp
                        } else 55.dp,
                        onMessageDragged = { message ->
                            controller?.show()
                            repliedMessage = message
                            focusRequester.freeFocus()
                            focusManager.clearFocus()
                            focusRequester.requestFocus()
                        }
                    )
                    MessageInput(
                        modifier = Modifier
                            .align(Alignment.BottomCenter),
                        textInput = textInput,
                        repliedMessage = repliedMessage,
                        onCancelReplyClicked = {
                            repliedMessage = null
                        },
                        onSendClicked = {
                            controller?.hide()
                            focusManager.clearFocus()
                            onSendMessage(
                                textInput.value,
                                posts[currentIndex].id,
                                repliedMessage?.id
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
    onCommentsClicked: () -> Unit,
    post: Post
) {
    Box(
        modifier = modifier
            .background(Black3)
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(55.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = modifier.size(35.dp),//40.5.dp
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
                    text = post.page.name,//"Asguardian",
                    color = White1,
                    modifier = modifier
                        //.padding(horizontal = 15.dp), only
                        .padding(start = 10.dp)
                        .padding(end = 15.dp),
                    style = Typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )// no style before
                )
                Surface(
                    modifier = modifier
                        .size(17.dp),
                    color = GreyFollow,//post.page.color.toColor(),
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
                        tint = White1,
                        modifier = modifier.size(35.dp)//45.dp
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

@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    textInput: MutableState<String>,
    repliedMessage: Message?,
    onCancelReplyClicked: () -> Unit,
    onSendClicked: () -> Unit,
    focusRequester: FocusRequester
) {
    Column(
        modifier = modifier
            //.imePadding()
    ) {
        if (repliedMessage != null) {
            RepliedMessage(
                repliedMessage = repliedMessage,
                onCancelReplyClicked = onCancelReplyClicked
            )
        }
        if (repliedMessage == null) {
            HorizontalDivider(
                color = Grey2
            )
        }
        InputField(
            textValue = textInput,
            onSendClicked = onSendClicked,
            focusRequester = focusRequester
        )
    }
}

@Composable
fun RepliedMessage(
    repliedMessage: Message?,
    onCancelReplyClicked: () -> Unit
) {
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
            color = repliedMessage.user.color.toColor()
        )
        Spacer(Modifier.width(4.5.dp))
        Text(
            text = repliedMessage.content,
            color = White3,
            style = Typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onCancelReplyClicked() },
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

@Composable
fun DescriptionText(
    modifier: Modifier = Modifier,
    text: String,
//    containerHeight: Int,
    containerWidth: Int
) {
    val textMeasurer = rememberTextMeasurer()

    val isOnlyEmoji = remember(text) {
        text.all { it.isEmoji() }
    }

    val normalStyle = TextStyle(fontSize = 20.sp)
    val measureResult = textMeasurer.measure(
        text = AnnotatedString(text),
        style = normalStyle,
        constraints = Constraints(maxWidth = containerWidth)
    )

    val finalFontSize = when {
        isOnlyEmoji && measureResult.lineCount == 1 -> 45.sp
        measureResult.lineCount > 1 && text.hasTextOnSubsequentLines(measureResult) -> 16.sp
        else -> 20.sp
    }

    Text(
        text = text,
        fontSize = finalFontSize,
        modifier = modifier,
        style = LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified),
        textAlign = TextAlign.Center,
        color = Color.White,
        fontWeight = FontWeight.Medium
    )

//    BasicText(
//        text = text,
//        modifier = modifier,
//        autoSize = TextAutoSize.StepBased(
//            minFontSize = 16.sp,
//            maxFontSize = 24.sp
//        ),
//        maxLines = 1,
//        color = { Color.White }
//    )
}

fun String.hasTextOnSubsequentLines(result: TextLayoutResult): Boolean {
    if (result.lineCount <= 1) return false

    val secondLineStart = result.getLineStart(1)
    val remainingText = this.substring(secondLineStart)

    return !remainingText.isOnlyEmojiWithCompat() || remainingText.any { !it.isWhitespace() }
}

fun Char.isEmoji(): Boolean {
    val type = Character.getType(this).toByte()
    return type == Character.SURROGATE ||
            type == Character.OTHER_SYMBOL
}

fun String.isEmojiOnly(): Boolean {
    if (this.isBlank()) return false

    //This regex looks for emoji UI symbols, pictographs, and modifiers
    val emojiRegex = "[\\p{So}\\p{Cn}]+".toRegex()
    //We remove whitespace and check if the remaining characters match the emoji patterns
    val textWithoutWhitespace = this.replace("\\s".toRegex(), "")
    return emojiRegex.matches(textWithoutWhitespace)
}

fun String.isOnlyEmojiWithCompat(): Boolean {
    val emojiCompat = EmojiCompat.get()
    if (emojiCompat.loadState != EmojiCompat.LOAD_STATE_SUCCEEDED) {
        Log.d("emojis", "EmojiCompat not loaded")
        return false
    }

    val cleanText = this.filter { !it.isWhitespace() }
    if (cleanText.isEmpty()) return false

    var index = 0
    var emojiCount = 0

    while (index < cleanText.length) {
        val emojiStart = emojiCompat.getEmojiStart(cleanText, index)

        if (emojiStart == -1) {
            //if we hit a character that's not an emoji, the whole jumbo logic fails
            return false
        } else {
            index = emojiCompat.getEmojiEnd(cleanText, index)
            emojiCount++
        }
    }

    return emojiCount > 0
}