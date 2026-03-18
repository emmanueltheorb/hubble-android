package com.orb.hubble.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orb.hubble.data.Post
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

    @Query("SELECT * FROM post ORDER BY pageId ASC, insertedAt ASC")
    fun getPostsOrderedByPageIdAndTimestamp(): Flow<List<Post>>

    @Query("SELECT * FROM post ORDER BY pageId ASC")
    fun getPostsOrderedByPageId(): Flow<List<Post>>

    @Query("SELECT * FROM post WHERE pageId = :pageId ORDER BY insertedAt ASC")
    fun getPagePosts(pageId: String): Flow<List<Post>>

    @Query("SELECT * FROM post WHERE id = :postId")
    fun getPost(postId: String): Flow<List<Post>>
}