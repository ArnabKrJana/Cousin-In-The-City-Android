package com.oneforth.cousininthecity.data.repository

import com.oneforth.cousininthecity.data.mapper.toDomain
import com.oneforth.cousininthecity.data.remote.CousinApi
import com.oneforth.cousininthecity.data.remote.dto.ChatInputDto
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.model.MessageRole
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class ChatRepositoryImpl @Inject constructor(
    private val api: CousinApi
) : ChatRepository {

    // In-memory cache for now (will be replaced by Room DB later)
    private val memoryThreads = mutableListOf<ChatThread>()
    private val memoryHistory = mutableMapOf<String, MutableList<ChatMessage>>()

    override suspend fun getThreads(deviceId: String): Result<List<ChatThread>> {
        return runCatching {
            val remoteThreads = api.getThreads(deviceId).map { it.toDomain() }
            (remoteThreads + memoryThreads).distinctBy { it.id }
        }.recover {
            memoryThreads
        }
    }

    override suspend fun createThread(deviceId: String, title: String): Result<ChatThread> {
        val newThread = ChatThread(
            id = System.currentTimeMillis().toString(),
            title = title,
            deviceId = deviceId
        )
        memoryThreads.add(0, newThread)
        memoryHistory[newThread.id] = mutableListOf()
        return Result.success(newThread)
    }

    override suspend fun getHistory(threadId: String): Result<List<ChatMessage>> {
        if (memoryHistory.containsKey(threadId)) {
            return Result.success(memoryHistory[threadId] ?: emptyList())
        }
        return runCatching {
            api.getHistory(threadId).map { it.toDomain() }
        }.recover {
            emptyList()
        }
    }

    override suspend fun sendMessage(prompt: String, conversationId: String): Result<ChatMessage> {
        if (memoryHistory.containsKey(conversationId)) {
            memoryHistory[conversationId]?.add(ChatMessage(role = MessageRole.USER, content = prompt))
        }

        return runCatching {
            val response = api.chat(ChatInputDto(prompt = prompt, conversationId = conversationId))
            val assistantMessage = ChatMessage(role = MessageRole.ASSISTANT, content = response.content)
            
            if (memoryHistory.containsKey(conversationId)) {
                memoryHistory[conversationId]?.add(assistantMessage)
            }
            
            assistantMessage
        }
    }
}
