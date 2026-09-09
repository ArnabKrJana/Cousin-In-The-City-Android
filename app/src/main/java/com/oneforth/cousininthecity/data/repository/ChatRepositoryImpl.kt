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

    override suspend fun getThreads(deviceId: String): Result<List<ChatThread>> {
        return runCatching {
            api.getThreads(deviceId).map { it.toDomain() }
        }
    }

    override suspend fun createThread(deviceId: String, title: String): Result<ChatThread> {
        return runCatching {
            api.createThread(deviceId, title).toDomain()
        }
    }

    override suspend fun getHistory(threadId: String): Result<List<ChatMessage>> {
        return runCatching {
            api.getHistory(threadId).map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(prompt: String, conversationId: String): Result<ChatMessage> {
        return runCatching {
            val response = api.chat(ChatInputDto(prompt = prompt, conversationId = conversationId))
            ChatMessage(
                role = MessageRole.ASSISTANT,
                content = response.content
            )
        }
    }
}
