package com.orb.hubble.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import com.orb.hubble.R

data class Comment(
    val id: String,
    val username: String,
    val content: String,
    val timestamp: String,
    val likes: Int,
    val isLiked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    comments: List<Comment>,
    onCommentSubmit: (String) -> Unit,
    onCommentLike: (String) -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val bottomSheetHeight = screenHeight * 0.8f // 80% of screen height

    // Animation for sliding up/down
    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else bottomSheetHeight,
        animationSpec = tween(durationMillis = 400),
        label = "bottom_sheet_animation"
    )

    // Background dim animation
    val dimAlpha by animateDpAsState(
        targetValue = if (isVisible) 0.6f.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "dim_animation"
    )

    // Show dim background first, then the sheet
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Small delay to show dim first
            delay(50)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2f) // Ensure it's above other content
    ) {
        // Dim background
        if (isVisible || dimAlpha > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha.value * 1.6f))
                    .zIndex(1f)
            ) {
                // Click on dim area to dismiss
                if (isVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Optional: Add a hint that you can swipe down
                        Text(
                            text = "Swipe down to close",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }

        // Bottom Sheet Content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomSheetHeight)
                .offset(y = offsetY)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .zIndex(2f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Comments (${comments.size})") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(painter = painterResource(R.drawable.chat_hb), contentDescription = "Close")
                            }
                        },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
                bottomBar = {
                    CommentInputField(onCommentSubmit = onCommentSubmit)
                }
            ) { paddingValues ->
                CommentList(
                    comments = comments,
                    onCommentLike = onCommentLike,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun CommentInputField(
    onCommentSubmit: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            placeholder = { Text("Add a comment...") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onCommentSubmit(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.chat_hb),
                        contentDescription = "Send comment",
                        tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            },
            singleLine = false,
            maxLines = 3
        )
    }
}

@Composable
fun CommentList(
    comments: List<Comment>,
    onCommentLike: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(comments) { comment ->
            CommentItem(
                comment = comment,
                onLikeClick = { onCommentLike(comment.id) }
            )
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onLikeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Username and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = comment.username,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = comment.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment content
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Like button and count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.chat_hb),
                        contentDescription = "Like comment",
                        tint = if (comment.isLiked) Color.Red
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = comment.likes.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}