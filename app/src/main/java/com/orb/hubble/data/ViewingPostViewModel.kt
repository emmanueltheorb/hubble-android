package com.orb.hubble.data

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.orb.hubble.repository.FakePostRepository
import com.orb.hubble.repository.HubbleRepository
import com.orb.hubble.repository.PostRepository
import com.orb.hubble.utils.ConnectivityObserver
import com.orb.hubble.utils.VideoCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@UnstableApi
@HiltViewModel
class ViewingPostViewModel @Inject constructor(
    private val connectivityObserver: ConnectivityObserver,
    private val repository: HubbleRepository,
    private val postRepository: PostRepository,
    private val videoCacheManager: VideoCacheManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val dataSourceFactory = videoCacheManager.getDataSourceFactory()

    val isConnected = connectivityObserver
        .isConnected
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        )

    private val pageId: String? = savedStateHandle["pageId"]
    private val singlePostId: String? = savedStateHandle["postId"]

    private val postIsFrom: PostIsFrom? = savedStateHandle["isFrom"]

    private var _statuses = MutableStateFlow<List<Status>>(emptyList())
    val statuses = _statuses.asStateFlow()

    private val _isLoadingPosts = MutableStateFlow(true)
    val isLoadingPosts = _isLoadingPosts.asStateFlow()

    private val _currentPostId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _currentPostId
        .flatMapLatest { postId ->
            if (postId.isNullOrEmpty()) flowOf(emptyList())
            else repository.getAllMessages(postId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _isLoadingPosts.value = true

            if (postIsFrom != null) {
                when (postIsFrom) {
                    PostIsFrom.HOME_SCREEN -> {
                        postRepository.homeScreenStatuses.collect { statuses ->
                            _statuses.value = statuses
                            _isLoadingPosts.value = false
                        }
                    }

                    PostIsFrom.MY_POSTS -> {
                        postRepository.myPosts.collect { status ->
                            if (status != null) {
                                _statuses.value = listOf(status)
                            }
                            _isLoadingPosts.value = false
                        }
                    }

                    PostIsFrom.EXPLORE -> {

                    }

                    PostIsFrom.SINGLE_POST -> {
                        if (!singlePostId.isNullOrEmpty()) {
                            repository.getPost(singlePostId).collect { postList ->
                                if (postList.isNotEmpty()) {
                                    val post = postList.first()

                                    _statuses.value = listOf(
                                        Status(
                                            pageId = post.pageId,
                                            ownerId = post.page.ownerId,
                                            username = post.page.name,
                                            posts = listOf(post),
                                            profilePic = ""
                                        )
                                    )
                                }
                                _isLoadingPosts.value = false
                            }
                        }
                    }

                    PostIsFrom.NOWHERE -> {

                    }
                }
            }
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
            id = "_state.value.userId",
            username = "AsguarL",
            color = "",
            phoneNumber = ""
        )
        val postMessageRequest = PostMessageRequest(
            messageId,
            "state.value.userId",
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
}

enum class PostIsFrom {
    HOME_SCREEN, MY_POSTS, EXPLORE, SINGLE_POST, NOWHERE
}