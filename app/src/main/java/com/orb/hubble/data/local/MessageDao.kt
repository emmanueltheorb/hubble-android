package com.orb.hubble.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.orb.hubble.data.Message
import com.orb.hubble.data.MessageWithPostAndReply
import com.orb.hubble.data.MessageWithReply
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Upsert
    suspend fun upsertMessage(message: Message)

    @Transaction
    @Query("SELECT * FROM messages WHERE postId = :postId ORDER BY insertedAt ASC")
    fun getMessagesForPost(postId: String): Flow<List<MessageWithReply>>

    @Transaction
    @Query("SELECT * FROM messages WHERE repliedTo = :userId ORDER BY insertedAt DESC")
    fun getReplies(userId: String): Flow<List<MessageWithReply>>

    @Transaction
    @Query("SELECT * FROM messages WHERE repliedUserId = :userId ORDER BY insertedAt DESC")
    fun getReplyLogs(userId: String): Flow<List<MessageWithPostAndReply>>

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
}