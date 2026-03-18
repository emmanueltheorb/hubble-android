package com.orb.hubble.repository

import com.orb.hubble.data.CacheEntry
import com.orb.hubble.data.local.CacheEntryDao
import com.orb.hubble.data.ClientEvent
import com.orb.hubble.data.Contact
import com.orb.hubble.data.CreatePostRequest
import com.orb.hubble.data.FollowRequest
import com.orb.hubble.data.Message
import com.orb.hubble.data.MessageWithPostAndReply
import com.orb.hubble.data.local.MessageDao
import com.orb.hubble.data.Post
import com.orb.hubble.data.local.PostDao
import com.orb.hubble.data.PostMessageRequest
import com.orb.hubble.data.WebsocketEvent
import com.orb.hubble.di.ApplicationScope
import com.orb.hubble.service.WebsocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Singleton
class HubbleRepository @Inject constructor(
    private val websocketService: WebsocketService,
    private val postDao: PostDao,
    private val cacheEntryDao: CacheEntryDao,
    private val messagesDao: MessageDao,
    @param:ApplicationScope private val externalScope: CoroutineScope
) {

    init {
        observeAndPersistPosts()
        observeAndPersistMessages()
    }

    val connectionState: Flow<WebsocketEvent> = websocketService.events

    private fun observeAndPersistPosts() {
        websocketService.events
            .onEach { event ->
                when (event) {
                    is WebsocketEvent.NewPost -> {
                        postDao.upsertPost(event.post)
                        if (event.post.type.equals("video", ignoreCase = true)) {
                            cacheEntryDao.upsert(
                                CacheEntry(
                                    url = event.post.contentUrl ?: "",
                                    insertedAt = Clock.System.now().toEpochMilliseconds()
                                )
                            )
                        }
                    }
                    is WebsocketEvent.MissedPost -> {
                        postDao.upsertPost(event.post)
                        if (event.post.type.equals("video", ignoreCase = true)) {
                            cacheEntryDao.upsert(
                                CacheEntry(
                                    url = event.post.contentUrl ?: "",
                                    insertedAt = Clock.System.now().toEpochMilliseconds()
                                )
                            )
                        }
                    }
                    // You can add message persistence here later
                    else -> Unit
                }
            }
            .launchIn(externalScope) // Crucial: This runs independently of any ViewModel
    }

    private fun observeAndPersistMessages() {
        websocketService.events
            .onEach { event ->
                when (event) {
                    is WebsocketEvent.PostMessage -> messagesDao.upsertMessage(event.message)
                    else -> Unit
                }
            }
            .launchIn(externalScope)
    }

    fun getAllPosts(): Flow<List<Post>> = postDao.getPostsOrderedByPageIdAndTimestamp()

    fun getPagePosts(pageId: String): Flow<List<Post>> = postDao.getPagePosts(pageId)

    fun getAllMessages(postId: String): Flow<List<Message>> = messagesDao.getMessagesForPost(postId)
        .map { messagesWithReplies ->
            messagesWithReplies.map { item ->
                item.message.copy(repliedMessage = item.repliedMessage)
            }
        }
        .flowOn(Dispatchers.Default)

    fun getReplies(userId: String): Flow<List<Message>> = messagesDao.getReplies(userId)
        .map { messageWithReplies ->
            messageWithReplies.map { item ->
                item.message.copy(repliedMessage = item.repliedMessage)
            }
        }
        .flowOn(Dispatchers.Default)

    fun getReplyLogs(userId: String): Flow<List<MessageWithPostAndReply>> = messagesDao.getReplyLogs(userId)
        .flowOn(Dispatchers.IO)

    fun getPost(postId: String): Flow<List<Post>> = postDao.getPost(postId)

//    val newPosts: Flow<Post> = websocketService.events
//        .map { event ->
//            when (event) {
//                is WebsocketEvent.NewPost -> event.post
//                else -> null
//            }
//        }
//        .filterNotNull()
//
//    val missedPosts: Flow<Post> = websocketService.events
//        .map { event ->
//            when (event) {
//                is WebsocketEvent.MissedPost -> event.post
//                else -> null
//            }
//        }
//        .filterNotNull()

//    val messages: Flow<Message> = websocketService.events
//        .map { event ->
//            when (event) {
//                is WebsocketEvent.PostMessage -> event.message
//                else -> null
//            }
//        }
//        .filterNotNull()

    suspend fun connect(userId: String) {
        websocketService.connect(userId)
    }

    suspend fun syncContacts(contacts: List<Contact>) {
        websocketService.sendEvent(ClientEvent.SyncContact(contacts))
    }

    @OptIn(ExperimentalTime::class)
    suspend fun joinPost(postId: String, from: Instant? = null) {
        websocketService.sendEvent(ClientEvent.JoinPost(postId, from))
    }

    suspend fun createPost(post: CreatePostRequest, pageId: String) {
        websocketService.sendEvent(ClientEvent.CreatePost(post, pageId))
    }

    suspend fun sendMessage(message: PostMessageRequest) {
        websocketService.sendEvent(ClientEvent.PostMessage(message))
    }

    suspend fun followPage(pages: FollowRequest) {
        websocketService.sendEvent(ClientEvent.FollowPage(pages))
    }

    suspend fun disconnect() {
        websocketService.disconnect()
    }
}