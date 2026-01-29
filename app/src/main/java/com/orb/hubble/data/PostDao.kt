package com.orb.hubble.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Upsert
    suspend fun upsertPost(post: Post)

    @Delete
    suspend fun deletePost(post: Post)

    @Query("DELETE FROM post WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredPosts(currentTime: String)

    @Query("SELECT * FROM post ORDER BY insertedAt ASC")
    fun getPostsOrderedByTimestamp(): Flow<List<Post>>

    @Query("SELECT * FROM post ORDER BY userId ASC")
    fun getPostsOrderedByUserId(): Flow<List<Post>>

    @Query("SELECT * FROM post WHERE userId = :userId ORDER BY insertedAt ASC")
    fun getUserPosts(userId: String): Flow<List<Post>>
}