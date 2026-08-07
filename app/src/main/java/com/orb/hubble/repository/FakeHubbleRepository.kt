package com.orb.hubble.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.orb.hubble.data.CacheEntry
import com.orb.hubble.data.local.CacheEntryDao
import com.orb.hubble.data.ClientEvent
import com.orb.hubble.data.Contact
import com.orb.hubble.data.FollowRequest
import com.orb.hubble.data.Message
import com.orb.hubble.data.MessageWithPostAndReply
import com.orb.hubble.data.Page
import com.orb.hubble.data.local.MessageDao
import com.orb.hubble.data.Post
import com.orb.hubble.data.local.PostDao
import com.orb.hubble.data.PostMessageRequest
import com.orb.hubble.data.PostState
import com.orb.hubble.data.User
import com.orb.hubble.data.WebsocketEvent
import com.orb.hubble.data.workers.SendMessageWorker
import com.orb.hubble.di.ApplicationScope
import com.orb.hubble.service.ApiService
import com.orb.hubble.service.ProgressUpdate
import com.orb.hubble.service.WebsocketService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Singleton
class FakeHubbleRepository @Inject constructor(
    private val websocketService: WebsocketService,
    private val apiService: ApiService,
    private val postDao: PostDao,
    private val cacheEntryDao: CacheEntryDao,
    private val messagesDao: MessageDao,
    @param:ApplicationScope private val externalScope: CoroutineScope,
    @param:ApplicationContext private val context: Context
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

                    else -> Unit
                }
            }
            .launchIn(externalScope)
    }

    private fun observeAndPersistMessages() {
        websocketService.events
            .onEach { event ->
                when (event) {
                    is WebsocketEvent.PostMessage -> messagesDao.upsertMessage(
                        Message(
                            id = event.message.id,
                            content = event.message.content,
                            postId = event.message.postId,
                            repliedTo = event.message.repliedTo,
                            repliedUserId = event.message.repliedUserId,
                            insertedAt = event.message.insertedAt,
                            user = event.message.user,
                            pending = false
                        )
                    )

                    else -> Unit
                }
            }
            .launchIn(externalScope)
    }

    fun getAllPosts(): Flow<List<Post>> = postDao.getPostsOrderedByPageIdAndTimestamp()

    fun getPagePosts(pageId: String): Flow<List<Post>> = postDao.getPagePosts(pageId)

    // =========================================================================================
    // FAKE DATA GENERATORS & CUSTOMIZED FLOWS
    // =========================================================================================

    private val fakeUsers = listOf(
        User("u1", "chloe_tunes", "#FF5733", "555-0101"),
        User("u2", "jordan.beats", "#33FF57", "555-0102"),
        User("u3", "alexx_vibes", "#3357FF", "555-0103"),
        User("u4", "sammy_synth", "#F033FF", "555-0104"),
        User("u5", "taylor_fan99", "#FF3333", "555-0105")
    )

    private val teenMusicSlang = listOf(
        "bro this is literally so good 🔥",
        "mid tbh",
        "L take",
        "W",
        "fr fr",
        "im obsessed",
        "skip",
        "the bridge makes me ascend 😭",
        "no literally",
        "im crying this is beautiful",
        "who let them cook??",
        "banger after banger",
        "it's giving 2014 vibes",
        "slay",
        "ate and left no crumbs",
        "underrated af",
        "overrated idc",
        "this transitions so well",
        "been on repeat for 3 hours",
        "can't stop listening",
        "rent free in my head",
        "bruh what is this production 💀",
        "the lyrics are sending me",
        "so real for this",
        "my top spotify wrapped for sure",
        "they fell off",
        "we making it out of the hood with this one",
        "gatekeeping this",
        "tiktok is gonna ruin this song",
        "i need tour dates NOW"
    )

    private val meUser = User("me", "my_account", "#FFC0CB", "555-0000")

    @OptIn(ExperimentalTime::class)
    private val dummyPage = Page("page1", "Music Heads", "#FFFFFF", "555-1111", "admin1")

    @OptIn(ExperimentalTime::class)
    private val dummyPost = Post(
        id = "post1", contentUrl = null, thumbnailUrl = null, description = "AOTY discussion",
        type = "text", tags = listOf("music", "aoty"), expiresAt = Clock.System.now(),
        pageId = "page1", thumbnailAt = null, insertedAt = Clock.System.now(), page = dummyPage
    )

    /**
     * Helper to simulate messages dropping in sequentially over time like a live chat.
     */
    private fun <T> emitWithLiveSimulation(items: List<T>): Flow<List<T>> = flow {
        val currentList = mutableListOf<T>()

        // Show first 10 immediately to populate the UI
        val initialBatchSize = 10.coerceAtMost(items.size)
        currentList.addAll(items.take(initialBatchSize))
        emit(currentList.toList())

        var currentIndex = initialBatchSize
        while (currentIndex < items.size) {
            // Random delay between 1s and 3.5s
            delay(Random.nextLong(1000, 3500))

            // Randomly push 1, 2, or 3 messages back-to-back to simulate conversation bursts
            val burstSize = Random.nextInt(1, 4)
            for (i in 0 until burstSize) {
                if (currentIndex < items.size) {
                    currentList.add(items[currentIndex])
                    currentIndex++
                }
            }
            emit(currentList.toList())
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getAllMessages(postId: String): Flow<List<Message>> {
        // Generating 50 fake messages for a single post (everyone chatting in one thread)
        val generatedMessages = (1..50).map { i ->
            Message(
                id = "msg_$i",
                content = teenMusicSlang.random(),
                postId = postId,
                repliedTo = if (i > 5 && Random.nextBoolean()) "msg_${
                    Random.nextInt(
                        1,
                        i
                    )
                }" else null,
                repliedUserId = if (Random.nextBoolean()) fakeUsers.random().id else null,
                insertedAt = Clock.System.now(),
                user = fakeUsers.random(),
                pending = false
            )
        }

        return emitWithLiveSimulation(generatedMessages).flowOn(Dispatchers.Default)
    }

    @OptIn(ExperimentalTime::class)
    fun getReplies(userId: String): Flow<List<Message>> {
        val generatedReplies = (1..50).map { i ->
            Message(
                id = "rep_$i",
                content = teenMusicSlang.random(),
                postId = "post_${Random.nextInt(1, 5)}", // Different posts
                repliedTo = "my_msg_$i",
                repliedUserId = userId,
                insertedAt = Clock.System.now(),
                user = fakeUsers.random(),
                pending = false
            )
        }

        return emitWithLiveSimulation(generatedReplies).flowOn(Dispatchers.Default)
    }

    @OptIn(ExperimentalTime::class)
    fun getReplyLogs(userId: String): Flow<List<MessageWithPostAndReply>> {

//        emit(
//            listOf(
//                MessageWithPostAndReply(
//                    message = Message(
//                        id = "rep_1",
//                        content = teenMusicSlang.random(),
//                        postId = "post_1",
//                        repliedTo = "my_msg_1",
//                        repliedUserId = userId,
//                        insertedAt = Clock.System.now(),
//                        user = fakeUsers.random(),
//                        pending = false
//                    ),
//                    repliedMessage = Message(
//                        id = "rep_0",
//                        content = teenMusicSlang.random(),
//                        postId = "post_0}",
//                        repliedTo = "my_msg_0",
//                        repliedUserId = userId,
//                        insertedAt = Clock.System.now(),
//                        user = fakeUsers.random(),
//                        pending = false
//                    ),
//                    postSummary = Post(
//                        id = "post1", contentUrl = null, thumbnailUrl = null, description = "AOTY discussion",
//                        type = "text", tags = listOf("music", "aoty"), expiresAt = Clock.System.now(),
//                        pageId = "page1", thumbnailAt = null, insertedAt = Clock.System.now(), page = dummyPage
//                    )
//                ),
//                MessageWithPostAndReply(
//                    message = Message(
//                        id = "rep_2",
//                        content = teenMusicSlang.random(),
//                        postId = "post_1",
//                        repliedTo = "my_msg_1",
//                        repliedUserId = userId,
//                        insertedAt = Clock.System.now(),
//                        user = fakeUsers.random(),
//                        pending = false
//                    ),
//                    repliedMessage = Message(
//                        id = "rep_0",
//                        content = teenMusicSlang.random(),
//                        postId = "post_0}",
//                        repliedTo = "my_msg_0",
//                        repliedUserId = userId,
//                        insertedAt = Clock.System.now(),
//                        user = fakeUsers.random(),
//                        pending = false
//                    ),
//                    postSummary = Post(
//                        id = "post1", contentUrl = null, thumbnailUrl = null, description = "AOTY discussion",
//                        type = "text", tags = listOf("music", "aoty"), expiresAt = Clock.System.now(),
//                        pageId = "page1", thumbnailAt = null, insertedAt = Clock.System.now(), page = dummyPage
//                    )
//                )
//            )
//        )

        // Generating 50 fake log events of teens replying specifically to "me" across different posts
        val replyScenarios = listOf(
            "nah ur trippin, that album was a solid 10",
            "omg wait u actually like that synth wave part?? W",
            "bro u need to listen to their older stuff before judging",
            "agreed 100%, that concert was life changing",
            "L take honestly, the production was terrible",
            "ur so right, gatekeeping this track forever",
            "dude u literally read my mind with this comment"
        )

        val generatedLogs = (1..50).map { i ->
            val randomPost = dummyPost.copy(
                id = "random_post_$i",
                description = "Music Post #$i",
                insertedAt = Clock.System.now()
            )

            val myOriginalMessage = Message(
                id = "my_original_msg_$i",
                content = "I honestly think the vocal mixing on track 4 was kind of weird.",
                postId = randomPost.id,
                repliedTo = null,
                repliedUserId = null,
                insertedAt = Clock.System.now(),
                user = meUser,
                pending = false
            )

            val teenReply = Message(
                id = "teen_reply_$i",
                content = replyScenarios.random() + " " + teenMusicSlang.random(),
                postId = randomPost.id,
                repliedTo = myOriginalMessage.id,
                repliedUserId = userId,
                insertedAt = Clock.System.now(),
                user = fakeUsers.random(),
                pending = false
            )

            MessageWithPostAndReply(
                message = teenReply,
                repliedMessage = myOriginalMessage,
                postSummary = randomPost
            )
        }

        return emitWithLiveSimulation(generatedLogs).flowOn(Dispatchers.IO)
    }

    // =========================================================================================
    // END OF CUSTOMIZED FLOWS
    // =========================================================================================

    fun getPost(postId: String): Flow<List<Post>> = postDao.getPost(postId)

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

    fun createPost(
        filePath: String,
        fileName: String,
        postState: PostState,
        isVideo: Boolean
    ): Flow<ProgressUpdate> {
        return apiService.createMediaPost(filePath, fileName, postState, isVideo)
    }

    fun createTextPost(postState: PostState): Flow<ProgressUpdate> {
        return apiService.createTextPost(postState)
    }

    suspend fun sendMessage(message: PostMessageRequest) {
        websocketService.sendEvent(ClientEvent.PostMessage(message))
    }

    fun setupSendMessageWorker(messageRequest: PostMessageRequest) {
        val inputData = workDataOf(
            "messageId" to messageRequest.messageId,
            "userId" to messageRequest.userId,
            "postId" to messageRequest.postId,
            "content" to messageRequest.content,
            "repliedTo" to messageRequest.repliedTo
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val sendMessageRequest = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(sendMessageRequest)
    }

    suspend fun followPage(pages: FollowRequest) {
        websocketService.sendEvent(ClientEvent.FollowPage(pages))
    }

    suspend fun disconnect() {
        websocketService.disconnect()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun insertMessageInDb(message: PostMessageRequest, user: User) {
        val newMessage = Message(
            id = message.messageId,
            content = message.content,
            postId = message.postId,
            repliedTo = message.repliedTo,
            repliedUserId = null,
            insertedAt = Clock.System.now(),
            user = user,
            pending = true
        )

        messagesDao.upsertMessage(newMessage)
    }
}