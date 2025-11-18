package com.orb.hubble.repository

import com.orb.hubble.data.ClientEvent
import com.orb.hubble.data.Contact
import com.orb.hubble.data.CreatePostRequest
import com.orb.hubble.data.FollowRequest
import com.orb.hubble.data.Message
import com.orb.hubble.data.Post
import com.orb.hubble.data.PostMessageRequest
import com.orb.hubble.data.WebsocketEvent
import com.orb.hubble.service.WebsocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class HubbleRepository(private val websocketService: WebsocketService) {
    val connectionState: Flow<WebsocketEvent> = websocketService.events

    val newPosts: Flow<Post> = websocketService.events
        .map { event ->
            when (event) {
                is WebsocketEvent.NewPost -> event.post
                else -> null
            }
        }
        .filterNotNull()

    val missedPosts: Flow<Post> = websocketService.events
        .map { event ->
            when (event) {
                is WebsocketEvent.MissedPost -> event.post
                else -> null
            }
        }
        .filterNotNull()

    val messages: Flow<Message> = websocketService.events
        .map { event ->
            when (event) {
                is WebsocketEvent.PostMessage -> event.message
                else -> null
            }
        }
        .filterNotNull()

    suspend fun connect(userId: Int) {
        websocketService.connect(userId)
    }

    suspend fun syncContacts(contacts: List<Contact>) {
        websocketService.sendEvent(ClientEvent.SyncContact(contacts))
    }

    @OptIn(ExperimentalTime::class)
    suspend fun joinPost(postId: Int, from: Instant? = null) {
        websocketService.sendEvent(ClientEvent.JoinPost(postId, from))
    }

    suspend fun createPost(post: CreatePostRequest) {
        websocketService.sendEvent(ClientEvent.CreatePost(post))
    }

    suspend fun sendMessage(message: PostMessageRequest) {
        websocketService.sendEvent(ClientEvent.PostMessage(message))
    }

    suspend fun followUser(users: FollowRequest) {
        websocketService.sendEvent(ClientEvent.FollowUser(users))
    }

    suspend fun disconnect() {
        websocketService.disconnect()
    }
}