package com.orb.hubble.data.workers

import android.content.Context
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.orb.hubble.data.local.CacheEntryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@HiltWorker
class MediaCleanupWorker @OptIn(UnstableApi::class)
@AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val simpleCache: SimpleCache,
    private val cacheEntryDao: CacheEntryDao
): CoroutineWorker(appContext, workerParams) {
    @OptIn(UnstableApi::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val twentyFourHoursAgo = Clock.System.now().toEpochMilliseconds() - 24.hours.inWholeMilliseconds

            val expiredEntries = cacheEntryDao.getExpiredEntries(twentyFourHoursAgo)

            if (expiredEntries.isEmpty()) {
                return@withContext Result.success()
            }

            expiredEntries.forEach { entry ->
                simpleCache.removeResource(entry.url)
                cacheEntryDao.deleteEntry(entry.url)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

//class CustomWorkerFactory @OptIn(UnstableApi::class)
//@Inject constructor(
//    private val simpleCache: SimpleCache,
//    private val cacheEntryDao: CacheEntryDao
//): WorkerFactory() {
//    override fun createWorker(
//        appContext: Context,
//        workerClassName: String,
//        workerParameters: WorkerParameters
//    ): ListenableWorker? {
//        return when (workerClassName) {
//            MediaCleanupWorker::class.qualifiedName ->
//            MediaCleanupWorker(
//                appContext = appContext,
//                workerParams = workerParameters,
//                simpleCache = simpleCache,
//                cacheEntryDao = cacheEntryDao
//            )
//            else -> null
//        }
//    }
//}