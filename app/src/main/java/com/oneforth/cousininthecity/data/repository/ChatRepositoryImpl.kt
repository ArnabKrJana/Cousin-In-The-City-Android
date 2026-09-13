package com.oneforth.cousininthecity.data.repository

import com.oneforth.cousininthecity.data.mapper.toDomain
import com.oneforth.cousininthecity.data.remote.CousinApi
import com.oneforth.cousininthecity.data.remote.dto.ChatInputDto
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.model.MessageRole
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: CousinApi
) : ChatRepository {

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
        return runCatching {
            val remoteThread = api.createThread(deviceId, title).toDomain()
            memoryThreads.add(0, remoteThread)
            memoryHistory[remoteThread.id] = mutableListOf()
            remoteThread
        }.recover {
            val newThread = ChatThread(
                id = System.currentTimeMillis().toString(),
                title = title,
                deviceId = deviceId
            )
            memoryThreads.add(0, newThread)
            memoryHistory[newThread.id] = mutableListOf()
            newThread
        }
    }

    override suspend fun getHistory(threadId: String): Result<List<ChatMessage>> {
        return runCatching {
            val remoteMessages = api.getHistory(threadId).map { it.toDomain() }
            val existingMemory = memoryHistory[threadId] ?: mutableListOf()
            val combined = (remoteMessages + existingMemory).distinctBy { it.id }
            memoryHistory[threadId] = combined.toMutableList()
            combined
        }.recover {
            memoryHistory[threadId] ?: emptyList()
        }
    }

    override suspend fun sendMessage(prompt: String, conversationId: String): Result<ChatMessage> {
        val userMessage = ChatMessage(role = MessageRole.USER, content = prompt)
        if (memoryHistory.containsKey(conversationId)) {
            memoryHistory[conversationId]?.add(userMessage)
        } else {
            memoryHistory[conversationId] = mutableListOf(userMessage)
        }

        return runCatching {
            val response = api.chat(ChatInputDto(prompt = prompt, conversationId = conversationId))
            val assistantMessage = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = response.message,
                intentType = response.intentType,
                actionData = response.actionData
            )

            memoryHistory[conversationId]?.add(assistantMessage)
            assistantMessage
        }
    }
}
