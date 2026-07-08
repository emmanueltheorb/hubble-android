package com.orb.hubble.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orb.hubble.data.PostMessageRequest
import com.orb.hubble.repository.HubbleRepository
import com.orb.hubble.repository.PostRepository
import com.orb.hubble.service.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SendMessageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ApiService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val messageId = inputData.getString("messageId") ?: return@withContext Result.failure()
            Log.d("sendJob", "messageId is $messageId")
            val userId = inputData.getString("userId") ?: return@withContext Result.failure()
            Log.d("sendJob", "userId is $userId")
            val content = inputData.getString("content") ?: return@withContext Result.failure()
            Log.d("sendJob", "content is $content")
            val postId = inputData.getString("postId") ?: return@withContext Result.failure()
            Log.d("sendJob", "postId is $postId")
            val repliedTo = inputData.getString("repliedTo")
            Log.d("sendJob", "repliedTo is $repliedTo")

            apiService.sendMessage(
                messageId = messageId,
                userId = userId,
                postId = postId,
                content = content,
                repliedTo = repliedTo
            )
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}