package com.orb.hubble.repository

import android.net.Uri
import com.orb.hubble.utils.FileReader
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

class PostRepository(
    private val httpClient: HttpClient = MyHttpClient.client,
    private val fileReader: FileReader
) {

    fun uploadFile(filePath: String, fileName: String): Flow<ProgressUpdate> = channelFlow {
        val info = fileReader.uriToFileInfo(filePath, fileName)
        val file = File(filePath)
        val fileLength = file.length()
        val tags = listOf("tag1", "tag2", "tag3")

        httpClient.submitFormWithBinaryData(
            url = "https://roma-hexaplaric-trilaterally.ngrok-free.dev/api/upload",
            formData = formData {
                append("user_id", "f1961098-22e1-4fd0-a0f1-7e61c21d120f")
                append("description", "Test")
                append("content_text", "")
                append("content_url", "")
                append("thumbnail_url", "")
                append("thumbnail_at", "1")
                append("type", "video")
                tags.forEach { tag ->
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
}

data class ProgressUpdate(
    val bytesSent: Long,
    val totalBytes: Long
)

@Serializable
data class UploadResponse(
    val id: String,
    val description: String,
    @SerialName("content_url")
    val contentUrl: String
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