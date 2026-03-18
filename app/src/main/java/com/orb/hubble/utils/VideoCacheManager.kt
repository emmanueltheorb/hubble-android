package com.orb.hubble.utils

import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.SimpleCache
import com.orb.hubble.data.CacheEntry
import com.orb.hubble.data.local.CacheEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Clock

@UnstableApi
class VideoCacheManager @Inject constructor(
    private val cache: SimpleCache,
    private val cacheEntryDao: CacheEntryDao,
    private val cacheDataSourceFactory: DataSource.Factory
) {
    fun getCache(): SimpleCache = cache

    fun getDataSourceFactory(): DataSource.Factory = cacheDataSourceFactory

    suspend fun logCacheEntry(videoUrl: String) {
        withContext(Dispatchers.IO) {
            cacheEntryDao.upsert(
                CacheEntry(
                    url = videoUrl,
                    insertedAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    suspend fun prefetchVideo(videoUrl: String, bytesToPrefetch: Long = 1024 * 1024 * 2) {
        withContext(Dispatchers.IO) {
            try {
                val datasource = cacheDataSourceFactory.createDataSource() as CacheDataSource
                val dataSpec = DataSpec(videoUrl.toUri(), 0, bytesToPrefetch)

                CacheWriter(
                    datasource,
                    dataSpec,
                    null,
                    null
                ).cache()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}