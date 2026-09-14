package com.oneforth.cousininthecity.domain.repository

import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.ChatThread
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getThreadsFlow(deviceId: String): Flow<List<ChatThread>>
    fun getHistoryFlow(threadId: String): Flow<List<ChatMessage>>
    
    suspend fun refreshThreads(deviceId: String): Result<Unit>
    suspend fun refreshHistory(threadId: String): Result<Unit>
    
    suspend fun createThread(deviceId: String, title: String): Result<ChatThread>
    suspend fun sendMessage(prompt: String, conversationId: String): Result<ChatMessage>
    
    suspend fun togglePinStatus(threadId: String, isPinned: Boolean)
    suspend fun deleteThread(threadId: String): Result<Unit>
}

