package com.oneforth.cousininthecity.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.oneforth.cousininthecity.data.local.UserPreferencesDataSource
import com.oneforth.cousininthecity.data.local.dao.MessageDao
import com.oneforth.cousininthecity.data.local.dao.ThreadDao
import com.oneforth.cousininthecity.data.local.entity.ThreadEntity
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
import java.util.regex.Pattern
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: CousinApi,
    private val threadDao: ThreadDao,
    private val messageDao: MessageDao,
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ChatRepository {

    override fun getThreadsFlow(deviceId: String): Flow<List<ChatThread>> {
        return threadDao.getThreadsFlow(deviceId).map { entities -> 
            entities.map { it.toDomain() }
        }
    }

    override fun getHistoryFlow(threadId: String): Flow<PagingData<ChatMessage>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { messageDao.getMessagesForThreadPagingSource(threadId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
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
            ensureThreadExists(threadId)
            val remoteMessages = api.getHistory(threadId)
            val entities = remoteMessages.map { dto ->
                val role = try { MessageRole.valueOf(dto.role.uppercase()) } catch (_: Exception) { MessageRole.USER }
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
        ensureThreadExists(conversationId)

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
        }.onFailure { e ->
            Log.e("ChatRepositoryImpl", "sendMessage failed", e)
            messageDao.deleteMessage(userMessage.id)
        }
    }

    override suspend fun togglePinStatus(threadId: String, isPinned: Boolean) {
        threadDao.updatePinnedStatus(threadId, isPinned)
    }

    override suspend fun deleteThread(threadId: String): Result<Unit> {
        threadDao.deleteThread(threadId)
        
        return runCatching {
            api.deleteThread(threadId)
        }
    }

    private suspend fun ensureThreadExists(threadId: String) {
        val existingThread = threadDao.getThreadById(threadId)
        if (existingThread == null) {
            val deviceId = userPreferencesDataSource.getOrCreateDeviceId()
            val newThread = ThreadEntity(
                id = threadId,
                title = "Relocation Chat",
                deviceId = deviceId,
                isPinned = false,
                lastUpdated = System.currentTimeMillis()
            )
            threadDao.insertThread(newThread)
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
