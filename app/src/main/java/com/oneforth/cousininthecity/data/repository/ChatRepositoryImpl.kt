package com.oneforth.cousininthecity.data.repository

import com.oneforth.cousininthecity.data.local.dao.MessageDao
import com.oneforth.cousininthecity.data.local.dao.ThreadDao
import com.oneforth.cousininthecity.data.mapper.toDomain
import com.oneforth.cousininthecity.data.mapper.toEntity
import com.oneforth.cousininthecity.data.remote.CousinApi
import com.oneforth.cousininthecity.data.remote.dto.ChatInputDto
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.model.MessageRole
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.util.regex.Pattern

class ChatRepositoryImpl @Inject constructor(
    private val api: CousinApi,
    private val threadDao: ThreadDao,
    private val messageDao: MessageDao
) : ChatRepository {

    override fun getThreadsFlow(deviceId: String): Flow<List<ChatThread>> {
        return threadDao.getThreadsFlow(deviceId).map { entities -> 
            entities.map { it.toDomain() }
        }
    }

    override fun getHistoryFlow(threadId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForThreadFlow(threadId).map { entities -> 
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshThreads(deviceId: String): Result<Unit> {
        return runCatching {
            val remoteThreads = api.getThreads(deviceId)
            val entities = remoteThreads.map { dto ->
                ChatThread(id = dto.id, deviceId = deviceId, title = dto.title).toEntity()
            }
            threadDao.insertThreads(entities)
        }
    }

    override suspend fun refreshHistory(threadId: String): Result<Unit> {
        return runCatching {
            val remoteMessages = api.getHistory(threadId)
            val entities = remoteMessages.map { dto ->
                val role = try { MessageRole.valueOf(dto.role.uppercase()) } catch (e: Exception) { MessageRole.USER }
                val cleanText = stripLegacyIntentTags(dto.content)
                ChatMessage(role = role, content = cleanText).toEntity(threadId)
            }
            messageDao.insertMessages(entities)
        }
    }

    override suspend fun createThread(deviceId: String, title: String): Result<ChatThread> {
        return runCatching {
            val remoteThread = api.createThread(deviceId, title)
            val domainThread = ChatThread(id = remoteThread.id, deviceId = deviceId, title = remoteThread.title)
            threadDao.insertThread(domainThread.toEntity())
            domainThread
        }.recover {
            val offlineThread = ChatThread(
                id = System.currentTimeMillis().toString(),
                deviceId = deviceId,
                title = title
            )
            threadDao.insertThread(offlineThread.toEntity())
            offlineThread
        }
    }

    override suspend fun sendMessage(prompt: String, conversationId: String): Result<ChatMessage> {
        val userMessage = ChatMessage(role = MessageRole.USER, content = prompt)
        messageDao.insertMessage(userMessage.toEntity(conversationId))
        
        threadDao.updateLastUpdated(conversationId, System.currentTimeMillis())

        return runCatching {
            val response = api.chat(ChatInputDto(prompt = prompt, conversationId = conversationId))
            val cleanText = stripLegacyIntentTags(response.message)
            val assistantMessage = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = cleanText,
                intentType = response.intentType,
                actionData = response.actionData
            )
            messageDao.insertMessage(assistantMessage.toEntity(conversationId))
            threadDao.updateLastUpdated(conversationId, System.currentTimeMillis())
            assistantMessage
        }.onFailure {
            // Option A Failure Strategy: Delete unsent user message so it doesnt hang
            messageDao.deleteMessage(userMessage.id)
        }
    }

    override suspend fun togglePinStatus(threadId: String, isPinned: Boolean) {
        threadDao.updatePinnedStatus(threadId, isPinned)
    }

    override suspend fun deleteThread(threadId: String): Result<Unit> {
        // Delete locally first for immediate UI feedback (Offline-first approach)
        threadDao.deleteThread(threadId)
        
        return runCatching {
            api.deleteThread(threadId)
        }
    }

    private fun stripLegacyIntentTags(text: String): String {
        var cleanText = text
        val jsonPattern = Pattern.compile("```(?:json|JSON)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL)
        val jsonMatcher = jsonPattern.matcher(cleanText)
        while (jsonMatcher.find()) {
            val jsonContent = jsonMatcher.group(1) ?: ""
            if (jsonContent.contains("\"name\"") || jsonContent.contains("\"parameters\"")) {
                cleanText = cleanText.replace(jsonMatcher.group(0) ?: "", "")
            }
        }
        
        val xmlPattern = Pattern.compile("<INTENT\\s+(.*?)\\s*/>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val xmlMatcher = xmlPattern.matcher(cleanText)
        while (xmlMatcher.find()) {
            cleanText = cleanText.replace(xmlMatcher.group(0) ?: "", "")
        }
        
        return cleanText.trim()
    }
}

