package com.orb.hubble.data

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.orb.hubble.data.workers.SendMessageWorker
import com.orb.hubble.repository.FakePostRepository
import com.orb.hubble.repository.HomeScreenPostState
import com.orb.hubble.repository.HubbleRepository
import com.orb.hubble.repository.PostRepository
import com.orb.hubble.repository.UserRepository
import com.orb.hubble.utils.ConnectivityObserver
import com.orb.hubble.utils.SaveToStorageStrategy
import com.orb.hubble.utils.StorageShareSheet
import com.orb.hubble.utils.VideoCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.plus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@UnstableApi
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val connectivityObserver: ConnectivityObserver,
    private val repository: HubbleRepository,
    private val userRepository: UserRepository,
    private val postRepository: FakePostRepository,
    private val videoCacheManager: VideoCacheManager
) : ViewModel() {
    val dataSourceFactory = videoCacheManager.getDataSourceFactory()
    val isConnected = connectivityObserver
        .isConnected
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        )

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    private val _isLoadingPosts = MutableStateFlow(true)
    val isLoadingPosts: StateFlow<Boolean> = _isLoadingPosts.asStateFlow()

    private val _currentPostId = MutableStateFlow<String?>(null)
    private val _currentPostPageId = MutableStateFlow<String?>(null)

    val selectedTab: StateFlow<PostFilterTab> = postRepository.selectedTab

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

    val myPosts: StateFlow<Status?> = postRepository.myPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val postsState: StateFlow<HomeScreenPostState> = postRepository.homeScreenPostState
        .onStart { _isLoadingPosts.value = true }
        .onEach { _isLoadingPosts.value = false }
        .catch {
            _isLoadingPosts.value = true
            emit(HomeScreenPostState())
            _state.update {
                it.copy(error = "Error loading posts")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeScreenPostState())

    val statuses: StateFlow<List<Status>> = postRepository.homeScreenStatuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        connect()
        observeConnectionState()
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

    fun setUserId(userId: String) {
        _state.update {
            it.copy(
                userId = userId
            )
        }
    }

    fun setPostPageId(pageId: String) {
        _currentPostPageId.value = pageId
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

    fun preloadNextPost(url: String?, postType: String) {
        if (postType.equals("Video", ignoreCase = true)) {
            viewModelScope.launch {
                videoCacheManager.prefetchVideo(url!!)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun sendMessage(content: String, postId: String, repliedTo: String? = null) {
        val messageId = Uuid.random().toString()
        val user = User(
            id = _state.value.userId,
            username = "AsguarL",
            color = "",
            phoneNumber = ""
        )
        val postMessageRequest = PostMessageRequest(
            messageId,
            state.value.userId,
            postId,
            content,
            repliedTo
        )

        viewModelScope.launch {
            repository.insertMessageInDb(postMessageRequest, user)
        }

        if (isConnected.value) {
            viewModelScope.launch {
                repository.sendMessage(postMessageRequest)
            }
            Log.d("isConnected", "isConnected is ${isConnected.value} instead of true")
        } else {
            repository.setupSendMessageWorker(postMessageRequest)
            Log.d("isConnected", "isConnected is ${isConnected.value} instead of false")
        }
    }

    fun syncContacts(contacts: List<Contact>) {
        viewModelScope.launch {
            repository.syncContacts(contacts)
        }
    }

    fun setSelectedTab(tab: PostFilterTab) {
        postRepository.setSelectedTab(tab)
    }

    private fun observeUserRelations() {
        userRepository.followedPages
            .onEach { followedPages ->
                if (followedPages.isNotEmpty()) {
                    _state.update { state ->
                        state.copy(followedPagesIds = followedPages.toSet())
                    }
                }
            }
            .launchIn(viewModelScope)

        userRepository.contacts
            .onEach { contacts ->
                if (contacts.isNotEmpty()) {
                    _state.update { state ->
                        state.copy(contactsUserIds = contacts.toSet())
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadUserRelations() {
        viewModelScope.launch {
            try {
                val userId = state.value.userId
                val pageId = state.value.pageId
                userRepository.getFollowedPages(pageId)
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
    val userId: String = "f0c7cfbc-604b-437e-bcce-2f6e17e063b7",
    val pageId: String = "25ff15a5-4728-42b4-a2e9-171662b949f5",
    val isConnected: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val selectedTab: PostFilterTab = PostFilterTab.ALL,
    val followedPagesIds: Set<String> = emptySet(),
    val contactsUserIds: Set<String> = emptySet()
)