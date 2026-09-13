package com.oneforth.cousininthecity.domain.model

import java.util.UUID

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val intentType: String? = null,
    val actionData: Map<String, String>? = null,
    val timestamp: Long = System.currentTimeMillis()
)
