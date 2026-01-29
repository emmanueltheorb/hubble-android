package com.orb.hubble.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orb.hubble.repository.HubbleRepository
import com.orb.hubble.repository.PostRepository
import com.orb.hubble.repository.UserRepository
import com.orb.hubble.ui.components.FieldStyle
import com.orb.hubble.ui.components.OverlayTextConfig
import com.orb.hubble.ui.screens.OverlaySticker
import com.orb.hubble.ui.screens.TextOverlay
import com.orb.hubble.ui.theme.NunitoFontFamily
import com.orb.hubble.utils.SaveToStorageStrategy
import com.orb.hubble.utils.StorageShareSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import javax.inject.Inject
import kotlin.collections.plus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: HubbleRepository,
    private val userRepository: UserRepository,
    private val mediaExporter: StorageMediaExporter,
    private val videoExporter: VideoMediaExporter,
    private val storageStrategy: SaveToStorageStrategy,
    private val shareSheet: StorageShareSheet,
    private val postRepository: PostRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    private val _isLoadingPosts = MutableStateFlow(true)
    val isLoadingPosts: StateFlow<Boolean> = _isLoadingPosts.asStateFlow()

    private val _currentPostId = MutableStateFlow<String?>(null)
    private val _currentPostUserId = MutableStateFlow<String?>(null)

    // Add these to your ViewModel
    private val _selectedTab = MutableStateFlow(PostFilterTab.ALL)
    val selectedTab: StateFlow<PostFilterTab> = _selectedTab.asStateFlow()

    val postsFromDb = repository.getAllPosts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val messagesFromDb = _currentPostId
        .flatMapLatest { postId ->
            if (postId.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                repository.getAllMessages(postId)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val userPosts = _currentPostUserId
        .flatMapLatest { userId ->
            if (userId.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                repository.getUserPosts(userId)
                    .onStart { _isLoadingPosts.value = true }
                    .onEach { _isLoadingPosts.value = false }
                    .catch {
                        _isLoadingPosts.value = false
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Filter posts based on current tab
    val filteredPosts: StateFlow<Map<String, List<Post>>> =
        combine(_state, _selectedTab, postsFromDb) { state, tab, posts ->
            val postMap = posts.groupBy { it.userId }

            when (tab) {
                PostFilterTab.ALL -> postMap
                PostFilterTab.FOLLOWING -> postMap.filterKeys { it in state.followedUserIds }
                PostFilterTab.CONTACTS -> postMap.filterKeys { it in state.contactUserIds }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyMap()
        )

    init {
        connect()
        observeConnectionState()
        //observePosts()
        //observeMessages()
        observeUserRelations()
    }

    private fun observeConnectionState() {
        repository.connectionState
            .onEach { event ->
                if (event is WebsocketEvent.Connected) {
                    // Connection is officially up, now safe to request data
                    loadUserRelations()
                }
                _state.update { currentState ->
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
        postsFromDb.value.onEach { post ->
            _state.update { currentState ->
                val updatedPostsList = updatePostsList(
                    postsList = currentState.postsList,
                    post = post
                )

                currentState.copy(
                    postsList = updatedPostsList
                )
            }
        }

//        repository.newPosts
//            .onEach { post ->
//                _state.update { currentState ->
//                    val updatedPostsList = updatePostsList(
//                        postsList = currentState.postsList,
//                        post = post
//                    )
//
//                    currentState.copy(
//                        postsList = updatedPostsList
//                    )
//                }
//            }
//            .launchIn(viewModelScope)
//
//        repository.missedPosts
//            .onEach { post ->
//                _state.update { currentState ->
//                    val updatedPostsList = updatePostsList(
//                        postsList = currentState.postsList,
//                        post = post
//                    )
//
//                    currentState.copy(
//                        postsList = updatedPostsList
//                    )
//                }
//            }
//            .launchIn(viewModelScope)
    }

    private fun updatePostsList(
        postsList: Map<String, List<Post>>,
        post: Post
    ): Map<String, List<Post>> {
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
        messagesList: Map<String, List<Message>>,
        message: Message
    ): Map<String, List<Message>> {
        val postMessages = messagesList[message.postId] ?: emptyList()

        val updatedPostMessages = (postMessages + message)

        return messagesList + (message.postId to updatedPostMessages)
    }

//    private fun observeMessages() {
//        repository.messages
//            .onEach { message ->
//                _state.update { currentState ->
//                    val updatedMessagesList = updateMessageList(
//                        currentState.messagesList,
//                        message
//                    )
//
//                    currentState.copy(
//                        messagesList = updatedMessagesList
//                    )
//                }
//            }
//            .launchIn(viewModelScope)
//    }

    fun setUserId(userId: String) {
        _state.update {
            it.copy(
                userId = userId
            )
        }
    }

    fun setPostUserId(userId: String) {
        _currentPostUserId.value = userId
    }

    fun connect() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            repository.connect(state.value.userId)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun joinPost(postId: String, from: Instant?) {
        _currentPostId.value = postId
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

    fun sendMessage(content: String, postId: String, userId: String, repliedTo: String? = null) {
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

    fun setSelectedTab(tab: PostFilterTab) {
        _selectedTab.value = tab
        _state.update { it.copy(selectedTab = tab) }
    }

    private fun observeUserRelations() {
        userRepository.followedUsers
            .onEach { followedUsers ->
                if (followedUsers.isNotEmpty()) {
                    _state.update { state ->
                        state.copy(followedUserIds = followedUsers.toSet())
                    }
                }
            }
            .launchIn(viewModelScope)

        userRepository.contacts
            .onEach { contacts ->
                if (contacts.isNotEmpty()) {
                    _state.update { state ->
                        state.copy(contactUserIds = contacts.toSet())
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadUserRelations() {
        viewModelScope.launch {
            try {
                val userId = state.value.userId
                userRepository.getFollowedUsers(userId)
                userRepository.getContacts(userId)
            } catch (e: Exception) {
                Log.e("HomeScreenViewModel", "Error loading user relations", e)
            }
        }
    }

//    override fun onCleared() {
//        super.onCleared()
//        viewModelScope.launch {
//            repository.disconnect()
//        }
//    }
}

enum class PostFilterTab {
    ALL, FOLLOWING, CONTACTS
}

data class HomeScreenState @OptIn(ExperimentalUuidApi::class) constructor(
    val userId: String = "f1961098-22e1-4fd0-a0f1-7e61c21d120f",
    val isConnected: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val postsList: Map<String, List<Post>> = emptyMap(),
    val messagesList: Map<String, List<Message>> = emptyMap(),
    val selectedTab: PostFilterTab = PostFilterTab.ALL,
    val followedUserIds: Set<String> = emptySet(),
    val contactUserIds: Set<String> = emptySet()
)