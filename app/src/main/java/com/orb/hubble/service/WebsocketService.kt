package com.orb.hubble.service

import android.util.Log
import com.orb.hubble.data.ClientEvent
import com.orb.hubble.data.Message
import com.orb.hubble.data.Post
import com.orb.hubble.data.WebsocketEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class WebsocketService @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
            pingInterval = 3.milliseconds
        }
    }

    private val _events = MutableSharedFlow<WebsocketEvent>()
    val events: SharedFlow<WebsocketEvent> = _events.asSharedFlow()

    private var websocketSession: DefaultClientWebSocketSession? = null
    private var job: Job? = null

    suspend fun connect(userId: Int, url: String = "wss://roma-hexaplaric-trilaterally.ngrok-free.dev/ws") {
        try {
            client.webSocket("$url?user_id=$userId") {
                websocketSession = this

                // Send Connection Event
                _events.emit(WebsocketEvent.Connected("Connected"))

                // Listen for incoming messages
                job = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    Log.d("Websocket", "Received: $text")
                                    handleIncomingMessage(text)
                                }
                                is Frame.Binary -> {
                                    // Handle Binary if needed
                                }
                                else -> {}
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Websocket", "Error in message loop: ${e.message}")
                        _events.emit(WebsocketEvent.Error(e.message ?: "Connection error"))
                    }
                }

                job?.join()
            }
        } catch (e: Exception) {
            Log.e("Websocket", "Connection Error: ${e.message}")
            _events.emit(WebsocketEvent.Error(e.message ?: "Connection failed"))
        } finally {
            _events.emit(WebsocketEvent.Disconnected)
        }
    }

    suspend fun sendEvent(event: ClientEvent) {
        try {
            val jsonString = when (event) {
                is ClientEvent.CreatePost -> event.toJson(json)
                is ClientEvent.FollowUser -> event.toJson(json)
                is ClientEvent.JoinPost -> event.toJson(json)
                is ClientEvent.PostMessage -> event.toJson(json)
                is ClientEvent.SyncContact -> event.toJson(json)
            }

            websocketSession?.send(Frame.Text(jsonString))
        } catch (e: Exception) {
            Log.e("Websocket", "Send error: ${e.message}")
            _events.emit(WebsocketEvent.Error("Failed to send message: ${e.message}"))
        }
    }

    private suspend fun handleIncomingMessage(text: String) {
        try {
            val jsonObject = json.parseToJsonElement(text).jsonObject

            when (val type = jsonObject["type"]?.jsonPrimitive?.contentOrNull) {
                "new_post" -> {
                    val post = json.decodeFromJsonElement<Post>(
                        jsonObject["post"] ?: return
                    )
                    _events.emit(WebsocketEvent.NewPost(post))
                }
                "missed_post" -> {
                    val post = json.decodeFromJsonElement<Post>(
                        jsonObject["post"] ?: return
                    )
                    _events.emit(WebsocketEvent.MissedPost(post = post))
                }
                "post_message" -> {
                    val message = json.decodeFromJsonElement<Message>(
                        jsonObject["message"] ?: return
                    )
                    _events.emit(WebsocketEvent.PostMessage(message = message))
                }
                else -> {
                    // Handle connection messages or other types
                    if (text.contains("Connected")) {
                        _events.emit(WebsocketEvent.Connected(text))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Websocket", "Error parsing message: ${e.message}")
        }
    }

    suspend fun disconnect() {
        job?.cancel()
        websocketSession?.close()
        client.close()
    }
}