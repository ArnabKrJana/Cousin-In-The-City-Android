package com.oneforth.cousininthecity.data.mapper

import com.oneforth.cousininthecity.data.remote.dto.AppUserDto
import com.oneforth.cousininthecity.data.remote.dto.ChatThreadDto
import com.oneforth.cousininthecity.data.remote.dto.MessageDto
import com.oneforth.cousininthecity.domain.model.AppUser
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.model.MessageRole

fun AppUserDto.toDomain(): AppUser = AppUser(
    deviceId = deviceId
)

fun ChatThreadDto.toDomain(): ChatThread = ChatThread(
    id = id,
    deviceId = deviceId,
    title = title,
    updatedAt = updatedAt
)

fun MessageDto.toDomain(): ChatMessage {
    val domainRole = when (role.uppercase()) {
        "USER" -> MessageRole.USER
        "ASSISTANT" -> MessageRole.ASSISTANT
        else -> MessageRole.ASSISTANT
    }
    return ChatMessage(
        role = domainRole,
        content = content
    )
}
