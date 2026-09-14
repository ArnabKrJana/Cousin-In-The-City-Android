package com.oneforth.cousininthecity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oneforth.cousininthecity.data.local.entity.ThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {

    @Query("SELECT * FROM chat_threads WHERE deviceId = :deviceId ORDER BY isPinned DESC, lastUpdated DESC")
    fun getThreadsFlow(deviceId: String): Flow<@JvmSuppressWildcards List<ThreadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ThreadEntity): @JvmSuppressWildcards Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<ThreadEntity>): @JvmSuppressWildcards List<Long>

    @Query("UPDATE chat_threads SET isPinned = :isPinned WHERE id = :threadId")
    suspend fun updatePinnedStatus(threadId: String, isPinned: Boolean): @JvmSuppressWildcards Int

    @Query("UPDATE chat_threads SET lastUpdated = :timestamp WHERE id = :threadId")
    suspend fun updateLastUpdated(threadId: String, timestamp: Long): @JvmSuppressWildcards Int

    @Query("DELETE FROM chat_threads WHERE id = :threadId")
    suspend fun deleteThread(threadId: String): @JvmSuppressWildcards Int
}

