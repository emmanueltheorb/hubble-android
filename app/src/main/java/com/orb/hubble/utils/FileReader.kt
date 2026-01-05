package com.orb.hubble.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FileReader @Inject constructor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun uriToFileInfo(filePath: String, fileName: String): FileInfo {
        return withContext(ioDispatcher) {
            val file = File(filePath)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
//            val bytes = context.contentResolver.openInputStream(contentUri)?.use { inputStream ->
//                inputStream.readBytes()
//            } ?: byteArrayOf()

            val mimeType = context.contentResolver.getType(contentUri) ?: ""

            FileInfo(
                name = fileName,
                mimeType = mimeType,
//                bytes = bytes
            )
        }
    }
}

class FileInfo(
    val name: String,
    val mimeType: String,
//    val bytes: ByteArray
)