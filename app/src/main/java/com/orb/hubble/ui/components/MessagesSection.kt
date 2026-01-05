package com.orb.hubble.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orb.hubble.data.Message
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Black2
import com.orb.hubble.ui.theme.Grey1
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt
import com.orb.hubble.R
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.Grey3
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White3

@Composable
fun MessagesSection(
    modifier: Modifier = Modifier,
    messages: List<Message>,
    bottomPadding: Dp = 55.dp,
    onMessageDragged: (Message) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(bottom = bottomPadding)
    ) {
        items(messages) { message ->
            MessageItem(
                message = message,
                onMessageDragged = onMessageDragged
            )
        }
    }
}

enum class DragState { Left, Right }

@Composable
fun MessageItem(
    modifier: Modifier = Modifier,
    message: Message,
    onMessageDragged: (Message) -> Unit
) {
    val dragState = remember {
        AnchoredDraggableState(
            initialValue = DragState.Left,
            anchors = DraggableAnchors {
                DragState.Left at 0f
                DragState.Right at 225f
            }
        )
    }

    LaunchedEffect(dragState) {
        snapshotFlow { dragState.settledValue }
            .collectLatest {
                if (it == DragState.Right) {
                    onMessageDragged(message)
                    dragState.animateTo(
                        DragState.Left
                    )
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.7.dp)
            .padding(bottom = 12.dp)
            .padding(horizontal = 12.dp)
            .offset {
                IntOffset(
                    x = dragState.requireOffset().roundToInt(),
                    y = 0
                )
            }
            .anchoredDraggable(
                state = dragState,
                orientation = Orientation.Horizontal
            )
    ) {
        if (message.repliedTo != null && message.repliedMessage != null) {
            Row(
                modifier = modifier
                    .padding(start = 54.dp)
            ) {
                Text(
                    text = "@${message.repliedMessage.user.username}",
                    fontWeight = FontWeight.Bold,
                    style = Typography.labelSmall,
                    color = message.repliedMessage.user.color.toColor()
                )
                Spacer(modifier.width(2.7.dp))
                Text(
                    text = message.repliedMessage.content,
                    color = White3,
                    style = Typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = modifier.size(40.5.dp),
                color = Color(0xFF3F3F3F),
                shape = CircleShape
            ) {

            }
            Spacer(modifier.width(12.dp))
            Column(
                modifier = modifier
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
                Text(
                    text = message.content,
                    color = White3,
                    style = Typography.bodySmall
                )
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
        Color(this.toColorInt())
    } catch (e: IllegalArgumentException) {
        Color.White
    }
}