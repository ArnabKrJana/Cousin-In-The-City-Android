package com.oneforth.cousininthecity.data.mapper

import com.oneforth.cousininthecity.data.local.entity.MessageEntity
import com.oneforth.cousininthecity.data.local.entity.ThreadEntity
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.model.MessageRole

fun ThreadEntity.toDomain(): ChatThread {
    return ChatThread(
        id = id,
        title = title,
        deviceId = deviceId,
        isPinned = isPinned,
        lastUpdatedMillis = lastUpdated
    )
}

fun ChatThread.toEntity(): ThreadEntity {
    return ThreadEntity(
        id = id,
        title = title,
        deviceId = deviceId,
        isPinned = isPinned,
        lastUpdated = lastUpdatedMillis
    )
}

fun MessageEntity.toDomain(): ChatMessage {
    val messageRole = try {
        MessageRole.valueOf(role.uppercase())
    } catch (e: Exception) {
        MessageRole.USER
    }
    
    return ChatMessage(
        id = id,
        role = messageRole,
        content = content,
        intentType = intentType,
        actionData = actionData,
        timestamp = timestamp
    )
}

fun ChatMessage.toEntity(threadId: String): MessageEntity {
    return MessageEntity(
        id = id,
        threadId = threadId,
        role = role.name,
        content = content,
        intentType = intentType,
        actionData = actionData,
        timestamp = timestamp
    )
}

