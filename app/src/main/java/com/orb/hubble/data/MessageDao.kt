package com.orb.hubble.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Upsert
    suspend fun upsertMessage(message: Message)

    @Transaction
    @Query("SELECT * FROM messages WHERE postId = :postId ORDER BY insertedAt ASC")
    fun getMessagesForPost(postId: String): Flow<List<MessageWithReply>>

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
}