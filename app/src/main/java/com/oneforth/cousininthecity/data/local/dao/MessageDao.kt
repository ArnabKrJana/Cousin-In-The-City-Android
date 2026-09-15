package com.oneforth.cousininthecity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oneforth.cousininthecity.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

import androidx.paging.PagingSource

@Dao
interface MessageDao {

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp DESC")
    fun getMessagesForThreadPagingSource(threadId: String): PagingSource<Int, MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): @JvmSuppressWildcards Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>): @JvmSuppressWildcards List<Long>

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String): @JvmSuppressWildcards Int
}

