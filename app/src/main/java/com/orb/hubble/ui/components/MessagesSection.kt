package com.orb.hubble.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orb.hubble.data.Message
import com.orb.hubble.ui.theme.Black2
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt
import com.orb.hubble.R
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.Grey3
import com.orb.hubble.ui.theme.NunitoFontFamily
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White3
import com.orb.hubble.ui.theme.WhiteHighlight
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MessagesSection(
    modifier: Modifier = Modifier,
    messages: List<Message>,
    messageId: String? = null,
    bottomPadding: Dp = 55.dp, //formerly 55.dp
    onMessageDragged: (Message) -> Unit
) {
    val listState = rememberLazyListState()
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(messages) {
        if (messageId != null && messages.isNotEmpty()) {
            val index = messages.indexOfFirst { it.id == messageId }

            if (index != -1) {
                listState.animateScrollToItem(index = index)
                highlightedMessageId = messageId
                delay(3000)
                highlightedMessageId = null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .padding(bottom = bottomPadding),
        state = listState
    ) {
        itemsIndexed(messages) { index, message ->
            val topPadding = if (index == 0) 10 else 0
            MessageItem(
                message = message,
                topExtraPadding = topPadding,
                onMessageDragged = onMessageDragged,
                isHighlighted = message.id == highlightedMessageId
            )
        }
    }
}
//roma-hexaplaric-trilaterally.ngrok-free.dev/media/media_1767546101460__original.mp4?v=63934765355	https://roma-hexaplaric-trilaterally.ngrok-free.dev/media/media_1767546101460__thumb.jpg?v=63934765355	Test		1	text	["tag1","tag2","tag3"]	2026-01-05T17:02:35Z	3	2026-01-04T17:02:36Z	0	{"id":"3","username":"user2","color":"#817f7f7c","phoneNumber":"08009876543"}

enum class DragState { Left, Idle, Right }

@Composable
fun MessageItem(
    modifier: Modifier = Modifier,
    message: Message,
    topExtraPadding: Int,
    onMessageDragged: (Message) -> Unit,
    isHighlighted: Boolean = false
) {
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

    val alpha by
    animateFloatAsState(
        targetValue = 0.2f,
        animationSpec = tween(3000),
    )

    val backgroundColor by animateColorAsState(
        targetValue = WhiteHighlight.copy(alpha = alpha),
        animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing),
        label = "HighlightAnimation"
    )

    LaunchedEffect(dragState) {
        snapshotFlow { dragState.settledValue }
            .collectLatest { settledState ->
                if (settledState != DragState.Idle) {
                    onMessageDragged(message)
                    dragState.animateTo(
                        DragState.Idle
                    )
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isHighlighted) backgroundColor else Color.Transparent)
            .padding(top = 2.7.dp + topExtraPadding.dp)
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
        if (message.repliedTo != null && message.repliedMessage != null) {
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
                    text = message.user.username,
                    style = Typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    color = message.user.color.toColor()
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = message.content,
                        color = White3,
                        style = Typography.bodySmall
                    )
                    if (message.pending) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Icon(
                                painter = painterResource(R.drawable.pending_icon),
                                tint = WhiteHighlight,
                                contentDescription = "message send pending",
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputField(
    modifier: Modifier = Modifier,
    textValue: MutableState<String>,
    onSendClicked: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 55.dp)
            .background(color = Black2)
            .padding(horizontal = 12.dp)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 45.dp),
            shape = RoundedCornerShape(22.5.dp),
            color = Grey2,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp)
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(27.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.emojis_icon),
                        contentDescription = "emojis icon",
                        tint = White1,
                        modifier = Modifier.size(24.3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 22.5.dp)
                        .focusRequester(focusRequester),
                    value = textValue.value,
                    onValueChange = {
                        textValue.value = it
                    },
                    textStyle = Typography.bodyMedium.copy(
                        color = White3,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    cursorBrush = SolidColor(White1),
                    maxLines = 6,
                    decorationBox = { innerTextField ->
                        Box {
                            if (textValue.value.isEmpty()) {
                                Text(
                                    text = "Message",
                                    style = Typography.bodyMedium.copy(
                                        color = Grey3,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 19.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        IconButton(
            onClick = onSendClicked,
            shape = CircleShape,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Grey2
            ),
            modifier = Modifier.size(45.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.send_icon),
                contentDescription = "send message icon",
                tint = White1,
                modifier = Modifier.size(22.5.dp)
            )
        }
    }
}

fun String.toColor(): Color {
    return try {
        if (this.isEmpty()) return Color.White
        Color(this.toColorInt())
    } catch (e: IllegalArgumentException) {
        Color.White
    }
}