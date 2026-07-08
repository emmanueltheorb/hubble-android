package com.orb.hubble.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.orb.hubble.data.local.MessageDao
import com.orb.hubble.repository.HubbleRepository
import com.orb.hubble.utils.ConnectivityObserver
import com.orb.hubble.utils.VideoCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@UnstableApi
@HiltViewModel
class ReplyLogViewModel @Inject constructor(
    private val repository: HubbleRepository,
    private val videoCacheManager: VideoCacheManager,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {
    val dataSourceFactory = videoCacheManager.getDataSourceFactory()
    val isConnected = connectivityObserver
        .isConnected
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        )

    private val _isLoadingPosts = MutableStateFlow(true)
    val isLoadingPosts: StateFlow<Boolean> = _isLoadingPosts.asStateFlow()

    private val _isLoadingMessageList = MutableStateFlow(true)
    val isLoadingMessageList: StateFlow<Boolean> = _isLoadingMessageList.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>("f0c7cfbc-604b-437e-bcce-2f6e17e063b7")

    @OptIn(ExperimentalCoroutinesApi::class)
    val messageList = _currentUserId
        .flatMapLatest { userId ->
            if (userId.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                repository.getReplyLogs(userId)
            }
        }
        .onStart { _isLoadingMessageList.value = true }
        .onEach { _isLoadingMessageList.value = false }
        .catch {
            _isLoadingMessageList.value = true
            emit(emptyList())
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    @OptIn(ExperimentalUuidApi::class)
    fun sendMessage(content: String, postId: String, repliedTo: String? = null) {
        if (_currentUserId.value != null) {
            val messageId = Uuid.random().toString()
            val user = User(
                id = _currentUserId.value!!,
                username = "AsguarL",
                color = "",
                phoneNumber = ""
            )
            val postMessageRequest = PostMessageRequest(
                messageId,
                _currentUserId.value!!,
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
            } else {
                repository.setupSendMessageWorker(postMessageRequest)
            }
        }
    }
}