package com.orb.hubble.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.orb.hubble.repository.HubbleRepository
import com.orb.hubble.repository.PostRepository
import com.orb.hubble.repository.UserRepository
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
import javax.inject.Inject
import kotlin.collections.plus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@UnstableApi
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: HubbleRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val videoCacheManager: VideoCacheManager
) : ViewModel() {
    val dataSourceFactory = videoCacheManager.getDataSourceFactory()

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    private val _isLoadingPosts = MutableStateFlow(true)
    val isLoadingPosts: StateFlow<Boolean> = _isLoadingPosts.asStateFlow()

    private val _currentPostId = MutableStateFlow<String?>(null)
    private val _currentPostPageId = MutableStateFlow<String?>(null)

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
    val pagePosts = _currentPostPageId
        .flatMapLatest { pageId ->
            if (pageId.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                repository.getPagePosts(pageId)
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
            val postMap = posts.groupBy { it.pageId }

            when (tab) {
                PostFilterTab.ALL -> postMap
                PostFilterTab.FOLLOWING -> postMap.filterKeys { it in state.followedPagesIds }
                PostFilterTab.CONTACTS -> postMap.filterKeys { it in state.contactsUserIds }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyMap()
        )

    val myPosts: StateFlow<List<Post>> =
        combine(postsFromDb, _state) { posts, state ->

            posts.groupBy { it.pageId }[state.pageId] ?: emptyList()

        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val postsState: StateFlow<PostsState> =
        combine(postsFromDb, _state, _selectedTab) { posts, state, tab ->
            val statuses = posts.groupBy { it.pageId }
                .map { (pageId, posts) ->
                    Status(
                        pageId = pageId,
                        ownerId = posts.first().page.ownerId,
                        posts = posts,
                        username = posts.first().page.name,
                        profilePic = ""
                    )
                }

            PostsState(
                allTabRecentPosts = statuses
                    .filter { it.pageId != state.pageId && !it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                allTabViewedPosts = statuses
                    .filter { it.pageId != state.pageId && it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                followingTabRecentPosts = statuses
                    .filter { it.pageId in state.followedPagesIds && it.pageId != state.pageId && !it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                followingTabViewedPosts = statuses
                    .filter { it.pageId in state.followedPagesIds && it.pageId != state.pageId && it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                contactsTabRecentPosts = statuses
                    .filter { it.ownerId in state.contactsUserIds && it.pageId != state.pageId && !it.seenAll }
                    .sortedByDescending { it.lastestUpdate },
                contactsTabViewedPosts = statuses
                    .filter { it.ownerId in state.contactsUserIds && it.pageId != state.pageId && it.seenAll }
                    .sortedByDescending { it.lastestUpdate }
            )

        }
            .flowOn(Dispatchers.Default)
            .onStart { _isLoadingPosts.value = true }
            .onEach { _isLoadingPosts.value = false }
            .catch {
                _isLoadingPosts.value = true
                emit(PostsState())
                _state.update {
                    it.copy(error = "Error loading posts")
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PostsState())

    val statuses: StateFlow<List<Status>> = combine(postsState, _selectedTab) { state, tab ->
        when (tab) {
            PostFilterTab.ALL -> state.allTabRecentPosts + state.allTabViewedPosts
            PostFilterTab.FOLLOWING -> state.followingTabRecentPosts + state.followingTabViewedPosts
            PostFilterTab.CONTACTS -> state.contactsTabRecentPosts + state.contactsTabViewedPosts
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun createPost(
        contentUrl: String,
        description: String? = null,
        contentText: String? = null,
        tags: List<String>? = null,
        pageId: String
    ) {
        viewModelScope.launch {
            repository.createPost(
                CreatePostRequest(
                    contentUrl,
                    description,
                    contentText,
                    tags
                ),
                pageId
            )
        }
    }

    fun sendMessage(content: String, postId: String, repliedTo: String? = null) {
        viewModelScope.launch {
            repository.sendMessage(
                PostMessageRequest(
                    state.value.userId,
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

data class PostsState(
    val allTabRecentPosts: List<Status> = emptyList(),
    val allTabViewedPosts: List<Status> = emptyList(),
    val followingTabRecentPosts: List<Status> = emptyList(),
    val followingTabViewedPosts: List<Status> = emptyList(),
    val contactsTabRecentPosts: List<Status> = emptyList(),
    val contactsTabViewedPosts: List<Status> = emptyList()
)