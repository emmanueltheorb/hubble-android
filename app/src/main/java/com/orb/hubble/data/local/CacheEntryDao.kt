package com.orb.hubble.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.orb.hubble.data.CacheEntry

@Dao
interface CacheEntryDao {

    @Upsert
    suspend fun upsert(entry: CacheEntry)

    @Query("SELECT * FROM media_cache_entries WHERE insertedAt < :expirationThreshold")
    suspend fun getExpiredEntries(expirationThreshold: Long): List<CacheEntry>

    @Query("DELETE FROM media_cache_entries WHERE url = :url")
    suspend fun deleteEntry(url: String)
}