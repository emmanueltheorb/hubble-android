package com.orb.hubble.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orb.hubble.repository.HubbleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: HubbleRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    init {
        observeConnectionState()
        observePosts()
        observeMessages()
    }

    private fun observeConnectionState() {
        repository.connectionState
            .onEach { event ->
                _uiState.update { currentState ->
                    when (event) {
                        is WebsocketEvent.Connected -> currentState.copy(
                            isConnected = true,
                            loading = false,
                            error = null
                        )
                        WebsocketEvent.Disconnected -> currentState.copy(
                            isConnected = false,
                            loading = false
                        )
                        is WebsocketEvent.Error -> currentState.copy(
                            error = event.error,
                            loading = false
                        )
                        else -> currentState
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observePosts() {
        repository.newPosts
            .onEach { post ->
                _uiState.update { currentState ->
                    val updatedPostsList = updatePostsList(
                        postsList = currentState.postsList,
                        post = post
                    )

                    currentState.copy(
                        postsList = updatedPostsList
                    )
                }
            }
            .launchIn(viewModelScope)

        repository.missedPosts
            .onEach { post ->
                _uiState.update { currentState ->
                    val updatedPostsList = updatePostsList(
                        postsList = currentState.postsList,
                        post = post
                    )

                    currentState.copy(
                        postsList = updatedPostsList
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updatePostsList(
        postsList: Map<Int, List<Post>>,
        post: Post
    ): Map<Int, List<Post>> {
        val userPosts = postsList[post.userId] ?: emptyList()

        val updatedUserPosts = (userPosts + post)

        return if (updatedUserPosts.isEmpty()) {
            // Remove user if no posts left
            postsList - post.userId
        } else {
            postsList + (post.userId to updatedUserPosts)
        }
    }

    private fun updateMessageList(
        messagesList: Map<Int, List<Message>>,
        message: Message
    ): Map<Int, List<Message>> {
        val postMessages = messagesList[message.postId] ?: emptyList()

        val updatedPostMessages = (postMessages + message)

        return messagesList + (message.postId to updatedPostMessages)
    }

    private fun observeMessages() {
        repository.messages
            .onEach { message ->
                _uiState.update { currentState ->
                    val updatedMessagesList = updateMessageList(
                        currentState.messagesList,
                        message
                    )

                    currentState.copy(
                        messagesList = updatedMessagesList
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun connect(userId: Int) {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            repository.connect(userId)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun joinPost(postId: Int, from: Instant?) {
        viewModelScope.launch {
            repository.joinPost(postId, from)
        }
    }

    fun createPost(
        contentUrl: String,
        description: String? = null,
        contentText: String? = null,
        tags: List<String>? = null
        ) {
        viewModelScope.launch {
            repository.createPost(
                CreatePostRequest(
                    contentUrl,
                    description,
                    contentText,
                    tags
                )
            )
        }
    }

    fun sendMessage(content: String, postId: Int, userId: Int, repliedTo: Int? = null) {
        viewModelScope.launch {
            repository.sendMessage(
                PostMessageRequest(
                    userId,
                    postId,
                    content,
                    repliedTo
                )
            )
        }
    }

    fun syncContacts(contacts: List<Contact>) {
        viewModelScope.launch {
            repository.syncContacts(contacts)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.disconnect()
        }
    }
}

data class HomeScreenState(
    val isConnected: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val postsList: Map<Int, List<Post>> = emptyMap(),
    val messagesList: Map<Int, List<Message>> = emptyMap()
)