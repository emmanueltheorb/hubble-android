package com.orb.hubble.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.data.Post
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun TestScreen(
    modifier: Modifier = Modifier,
    userId: Int,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()

    val contentUrl = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }
    val contentText = remember { mutableStateOf("") }

    val messageContent = remember { mutableStateOf("") }
    val postId = remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        viewModel.connect()
    }

    when {
        uiState.loading -> {
            CircularProgressIndicator()
        }
        !uiState.isConnected -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Disconnected")
                Button(onClick = { viewModel.connect() }) {
                    Text("Reconnect")
                }
            }
        }
        else -> {
            LazyColumn(
                modifier.padding(top = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(uiState.postsList[userId] ?: emptyList()) { post ->
                    PostItem(
                        post = post,
                        onJoinClick = { viewModel.joinPost(post.id, null) }
                    )
                }
                items(uiState.messagesList[0] ?: emptyList()) { message ->
                    Text(message.content)
                }
                item {
                    PostField(
                        contentUrl = contentUrl,
                        description = description,
                        contentText = contentText
                    ) {
                        viewModel.createPost(
                            contentUrl.value,
                            description.value,
                            contentText.value
                        )
                    }
                }
                item {
                    MessageInput(
                        messageContent,
                        postId
                    ) {
                        viewModel.sendMessage(
                            content = messageContent.value,
                            postId = postId.value.toInt(),
                            userId = userId
                        )
                    }
                }
            }
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackBar or handle error
        }
    }
}

@Composable
fun PostItem(post: Post, onJoinClick: () -> Unit) {
    Column {
        Text(post.contentUrl ?: "null post")
        Text(post.id.toString())
        Text(post.description ?: "no description found")
        Button(onClick = onJoinClick) {
            Text("Join Post")
        }
    }
}

@Composable
fun MessageInput(
    messageContent: MutableState<String>,
    postId: MutableState<String>,
    onSendMessageClicked: () -> Unit
) {
    Column {
        TextField(
            value = messageContent.value,
            onValueChange = {
                messageContent.value = it
            },
            placeholder = {Text("message")}
        )
        TextField(
            value = postId.value,
            onValueChange = {
                postId.value = it
            },
            placeholder = {Text("post id")}
        )
        Button(
            onClick = onSendMessageClicked
        ) {
            Text("Send Message")
        }
    }
}

@Composable
fun PostField(
    contentUrl: MutableState<String>,
    description: MutableState<String>,
    contentText: MutableState<String>,
    onPostClicked: () -> Unit
) {
    Column {
        TextField(
            value = contentUrl.value,
            onValueChange = {
                contentUrl.value = it
            },
            placeholder = {Text("contentUrl")}
        )
        TextField(
            value = description.value,
            onValueChange = {
                description.value = it
            },
            placeholder = {Text("description")}
        )
        TextField(
            value = contentText.value,
            onValueChange = {
                contentText.value = it
            },
            placeholder = {Text("contentText")}
        )
        Button(onClick = onPostClicked) {
            Text("Post")
        }
    }
}