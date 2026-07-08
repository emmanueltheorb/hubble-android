package com.orb.hubble.service

import android.util.Log
import com.orb.hubble.data.PostState
import com.orb.hubble.utils.FileReader
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.File

class ApiService(
    private val httpClient: HttpClient = MyHttpClient.client,
    private val fileReader: FileReader
) {

    fun createMediaPost(filePath: String, fileName: String, postState: PostState): Flow<ProgressUpdate> = channelFlow {
        val info = fileReader.uriToFileInfo(filePath, fileName)
        val file = File(filePath)
        val fileLength = file.length()

        httpClient.submitFormWithBinaryData(
            url = "https://roma-hexaplaric-trilaterally.ngrok-free.dev/api/upload",
            formData = formData {
                append("user_id", "f0c7cfbc-604b-437e-bcce-2f6e17e063b7")
                append("page_id", "7043d14d-1922-423c-a24a-3c882693b323")
                append("description", postState.description)
                append("content_text", postState.contentText)
                append("content_url", postState.contentUrl)
                append("thumbnail_url", postState.thumbnailUrl)
                append("thumbnail_at", postState.thumbnailAt)
                append("type", postState.type)
                postState.tags.forEach { tag ->
                    append("tags[]", tag)
                }
                append(
                    "content",
                    InputProvider(fileLength) { file.inputStream().asInput() },
                    Headers.build {
                        append(HttpHeaders.ContentType, info.mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=${info.name}")
                    })
            }
        ) {
            onUpload { bytesSentTotal, totalBytes ->
                if (totalBytes != null) {
                    if (totalBytes > 0L) {
                        send(ProgressUpdate(bytesSentTotal, totalBytes))
                    }
                }
            }
        }
    }

    fun createTextPost(postState: PostState): Flow<ProgressUpdate> = channelFlow {
        httpClient.submitFormWithBinaryData(
            url = "https://roma-hexaplaric-trilaterally.ngrok-free.dev/api/upload",
            formData = formData {
                append("user_id", postState.userId)
                append("page_id", postState.pageId)
                append("description", postState.description)
                append("content", postState.content)
                append("content_text", postState.contentText)
                append("content_url", postState.contentUrl)
                append("thumbnail_url", postState.thumbnailUrl)
                append("thumbnail_at", postState.thumbnailAt)
                append("type", postState.type)
                postState.tags.forEach { tag ->
                    append("tags[]", tag)
                }
            }
        ) {
            onUpload { bytesSentTotal, totalBytes ->
                if (totalBytes != null) {
                    if (totalBytes > 0L) {
                        send(ProgressUpdate(bytesSentTotal, totalBytes))
                    }
                }
            }
        }
    }

    suspend fun sendMessage(messageId: String, userId: String, postId: String, content: String, repliedTo: String?) {
        httpClient.submitFormWithBinaryData(
            url = "https://roma-hexaplaric-trilaterally.ngrok-free.dev/api/send_message",
            formData = formData {
                append("message_id", messageId)
                append("user_id", userId)
                append("post_id", postId)
                append("content", content)
                append("replied_to", repliedTo ?: "")
            }
        )
        Log.d("http", "sendMessage function ran")
    }
}

data class ProgressUpdate(
    val bytesSent: Long,
    val totalBytes: Long
)

object MyHttpClient {
    val SelectiveLogging = createClientPlugin("SelectiveLogging") {
        onResponse { response ->
            val bodyText = response.bodyAsText()
            println("Response Body: $bodyText")
        }
    }
    val client by lazy {
        HttpClient(CIO) {
//            install(Logging) {
//                level = LogLevel.ALL
//                logger = Logger.ANDROID
//
//                filter { request ->
//                    val contentType = request.contentType()
//                    contentType?.match(ContentType.MultiPart.FormData) == false
//                }
//            }

            install(SelectiveLogging)

            install(HttpTimeout) {
                requestTimeoutMillis = 600_000L // 10 minutes
                connectTimeoutMillis = 30_000L // 30 seconds
                socketTimeoutMillis = 600_000L // 10 minutes
            }
        }
    }
}