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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    private var uploadJob: Job? = null

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    // Add these to your ViewModel
    private val _selectedTab = MutableStateFlow(PostFilterTab.ALL)
    val selectedTab: StateFlow<PostFilterTab> = _selectedTab.asStateFlow()

    val postsFromDb = repository.getAllPosts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Filter posts based on current tab
    val filteredPosts: StateFlow<Map<Int, List<Post>>> =
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
        observePosts()
        observeMessages()
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
                _state.update { currentState ->
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

    fun setUserId(userId: Int) {
        _state.update {
            it.copy(
                userId = userId
            )
        }
    }

    fun connect() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            repository.connect(state.value.userId)
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

    fun uploadFile(filePath: String, fileName: String) {
        uploadJob = postRepository
            .uploadFile(filePath, fileName)
            .onStart {

                _state.update {
                    it.copy(
                        isUploading = true,
                        isUploadComplete = false,
                        errorMessage = null,
                        progress = 0f
                    )
                }
            }
            .onEach { progressUpdate ->
                _state.update {
                    val calculatedProgress = if (progressUpdate.totalBytes > 0) {
                        progressUpdate.bytesSent / progressUpdate.totalBytes.toFloat()
                    } else 0f
                    it.copy(
                        progress = calculatedProgress
                    )
                }
            }
            .onCompletion { cause ->
                if (cause == null) {
                    _state.update {
                        it.copy(
                            isUploading = false,
                            isUploadComplete = true
                        )
                    }
                } else if (cause is CancellationException) {
                    _state.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = "The upload was cancelled!",
                            isUploadComplete = false,
                            progress = 0f
                        )
                    }
                }
            }
            .catch { cause ->
                Log.e("UploadError", "Full exception: ", cause)
                val message = when (cause) {
                    is OutOfMemoryError -> "File too large!"
                    is FileNotFoundException -> "File not found!"
                    is UnresolvedAddressException -> "No internet!"
                    else -> "${cause.message}"
                }
                _state.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = message
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun cancelUpload() {
        uploadJob?.cancel()
    }

    fun transformOverlayText(
        id: String,
        offset: Offset,
        rotation: Float,
        scale: Float
    ) {
        _state.update {
            val (width, height) = it.mediaSize
            it.copy(
                overlayTexts = it.overlayTexts.map { overlayText ->
                    if (overlayText.id == id) {
                        overlayText.copy(
                            offsetRatioX = offset.x / width,
                            offsetRatioY = offset.y / height,
                            rotation = rotation,
                            scale = scale
                        )
                    } else overlayText
                }
            )
        }
    }

    fun transformOverlaySticker(
        id: String,
        offset: Offset,
        rotation: Float,
        scale: Float,
        parentSize: Size,
        currentStickerBaseSize: Size
    ) {
        _state.update {
            val (width, height) = parentSize
            it.copy(
                overlayStickers = it.overlayStickers.map { overlaySticker ->
                    // Calculate the current visual width relative to the parent width
                    val relativeWidth = (currentStickerBaseSize.width * scale) / width

                    if (overlaySticker.id == id) {
                        overlaySticker.copy(
                            offsetRatioX = offset.x / width,
                            offsetRatioY = offset.y / height,
                            rotation = rotation,
                            scale = scale,
                            normalizedWidth = relativeWidth // Store the essential size data
                        )
                    } else overlaySticker
                }
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addText() {
        val id = Uuid.random().toString()

        val overlayText = TextOverlay(
            id = id,
            text = "",
            offsetRatioX = 0.45f,
            offsetRatioY = 0.48f,
            config = OverlayTextConfig(
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontFamily = NunitoFontFamily,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                ),
                fieldStyle = FieldStyle.Filled,
                containerColor = Color.White
            )
        )

        _state.update {
            it.copy(
                overlayTexts = it.overlayTexts + overlayText,
                overlayInteraction = OverlayInteraction.Selected(id),
                selectedTextOverlay = overlayText
            )
        }
    }

    fun addSticker(sticker: OverlaySticker) {
        _state.update {
            it.copy(
                overlayStickers = it.overlayStickers + sticker
            )
        }
    }

    fun deleteOverlay(id: String) {
        _state.update {
            it.copy(
                overlayTexts = it.overlayTexts.filter { overlayText ->
                    overlayText.id != id
                }
            )
        }
    }

    fun selectOverlay(id: String) {
        _state.update {
            it.copy(
                overlayInteraction = OverlayInteraction.Selected(id),
                selectedTextOverlay = it.overlayTexts.first { overlayText ->
                    overlayText.id == id
                }
            )
        }
    }

    fun updateText(id: String, text: String) {
        _state.update {
//            it.copy(
//                overlayTexts = it.overlayTexts.map { overlayText ->
//                    if (overlayText.id == id) {
//                        overlayText.copy(
//                            text = text
//                        )
//                    } else overlayText
//                }
//            )
            val updatedOverlayTexts = it.overlayTexts.map { overlayText ->
                if (overlayText.id == id) {
                    overlayText.copy(text = text)
                } else overlayText
            }

            // Also update selectedTextOverlay if it's the same id
            val updatedSelectedOverlay = if (it.selectedTextOverlay.id == id) {
                it.selectedTextOverlay.copy(text = text)
            } else it.selectedTextOverlay

            it.copy(
                overlayTexts = updatedOverlayTexts,
                selectedTextOverlay = updatedSelectedOverlay
            )
        }
    }

    fun changeColor(id: String, color: Color) {
        _state.update {
            val updatedOverlayTexts = it.overlayTexts.map { overlayText ->
                if (overlayText.id == id) {
                    overlayText.copy(
                        config = overlayText.config.copy(containerColor = color)
                    )
                } else overlayText
            }

            // Also update selectedTextOverlay if it's the same id
            val updatedSelectedOverlay = if (it.selectedTextOverlay.id == id) {
                it.selectedTextOverlay.copy(
                    config = it.selectedTextOverlay.config.copy(containerColor = color)
                )
            } else it.selectedTextOverlay

            it.copy(
                overlayTexts = updatedOverlayTexts,
                selectedTextOverlay = updatedSelectedOverlay
            )
        }
    }

    fun updateTextConfig(id: String, config: OverlayTextConfig) {
        _state.update {
            it.copy(
                overlayTexts = it.overlayTexts.map { overlayText ->
                    if (overlayText.id == id) {
                        overlayText.copy(config = config)
                    } else overlayText
                }
            )
        }
    }

    fun editOverlay(id: String) {
        _state.update {
            it.copy(
                overlayInteraction = OverlayInteraction.Editing(id)
            )
        }
    }

    fun doneEditing() {
        _state.update {
            it.copy(
                overlayInteraction = OverlayInteraction.None
            )
        }
    }

    fun updateScreenSize(size: IntSize) {
        _state.update {
            it.copy(
                screenSize = size
            )
        }
    }

    fun updateContainerSize(size: IntSize) {
        _state.update {
            it.copy(
                mediaSize = size
            )
        }
    }

    fun saveMedia(mediaUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val backgroundMediaBytes = getBytesFromUri(mediaUri, context)
                val isVideo = context.contentResolver.getType(mediaUri)?.contains("video") == true
                val fileName = generateFileName(isVideo)
                val outputPath = storageStrategy.getFilePath(fileName)

                if (isVideo) {
                    videoExporter
                        .exportVideo(
                            inputUri = mediaUri,
                            overlayTexts = state.value.overlayTexts,
                            overlayStickers = state.value.overlayStickers,
                            outputPath = outputPath
                        )
                        .onSuccess { path ->
                            uploadFile(filePath = path, fileName = fileName)
                            //shareSheet.shareFile(path)
                        }
                        .onFailure { exception ->
                            Log.e("Export", "Video export failed", exception)
                        }
                } else {
                    mediaExporter
                        .exportMedia(
                            backgroundMediaBytes = backgroundMediaBytes,
                            overlayTexts = state.value.overlayTexts,
                            overlayStickers = state.value.overlayStickers,
                            templateSize = state.value.mediaSize,
                            screenSize = state.value.screenSize,
                            saveToStorageStrategy = storageStrategy,
                            fileName = fileName
                        )
                        .onSuccess {
                            uploadFile(filePath = it, fileName = fileName)
                            //shareSheet.shareFile(it)
                        }
                        .onFailure {
                            it.printStackTrace()
                        }

                }
            } catch (e: Exception) {
                Log.e("Uri loading Error", "Failed to load image: ${e.message}")
            }

        }
    }

    private fun generateFileName(isVideo: Boolean): String {
        return if (isVideo) {
            "media_${System.currentTimeMillis()}.mp4"
        } else {
            "media_${System.currentTimeMillis()}.jpg"
        }
    }

    private suspend fun getBytesFromUri(uri: Uri, context: Context): ByteArray =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: byteArrayOf()
        }
}

enum class PostFilterTab {
    ALL, FOLLOWING, CONTACTS
}

data class HomeScreenState @OptIn(ExperimentalUuidApi::class) constructor(
    val userId: Int = 3,
    val isConnected: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val postsList: Map<Int, List<Post>> = emptyMap(),
    val messagesList: Map<Int, List<Message>> = emptyMap(),
    val selectedTab: PostFilterTab = PostFilterTab.ALL,
    val followedUserIds: Set<Int> = emptySet(),
    val contactUserIds: Set<Int> = emptySet(),
    val screenSize: IntSize = IntSize.Zero,
    val mediaSize: IntSize = IntSize.Zero,
    val isLeavingWithoutSaving: Boolean = false,
    val overlayTexts: List<TextOverlay> = listOf(
        TextOverlay(
            id = "1",
            text = "Guh",
            offsetRatioX = 0.5f,
            offsetRatioY = 0.5f,
            config = OverlayTextConfig(
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontFamily = NunitoFontFamily,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                ),
                fieldStyle = FieldStyle.Filled,
                containerColor = Color(0xFF0056FE)
            )
        )
    ),
    val overlayStickers: List<OverlaySticker> = emptyList(),
    val selectedTextOverlay: TextOverlay = TextOverlay(
        id = Uuid.random().toString(),
        text = "",
        offsetRatioX = 0.45f,
        offsetRatioY = 0.48f,
        config = OverlayTextConfig(
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontFamily = NunitoFontFamily,
                textAlign = TextAlign.Center,
                color = Color.Black
            ),
            fieldStyle = FieldStyle.Filled,
            containerColor = Color.White
        )
    ),
    val overlayInteraction: OverlayInteraction = OverlayInteraction.None,
    val hasLeftEditor: Boolean = false,
    val isUploading: Boolean = false,
    val isUploadComplete: Boolean = false,
    val progress: Float = 0f,
    val errorMessage: String? = null
)