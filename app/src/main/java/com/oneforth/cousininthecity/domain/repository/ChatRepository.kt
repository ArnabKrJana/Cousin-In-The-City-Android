package com.oneforth.cousininthecity.domain.repository

import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.ChatThread

interface ChatRepository {
    suspend fun getThreads(deviceId: String): Result<List<ChatThread>>
    suspend fun createThread(deviceId: String, title: String): Result<ChatThread>
    suspend fun getHistory(threadId: String): Result<List<ChatMessage>>
    suspend fun sendMessage(prompt: String, conversationId: String): Result<ChatMessage>
}
