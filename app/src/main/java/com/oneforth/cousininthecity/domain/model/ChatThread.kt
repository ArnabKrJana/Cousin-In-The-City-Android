package com.oneforth.cousininthecity.domain.model

data class ChatThread(
    val id: String,
    val deviceId: String,
    val title: String,
    val updatedAt: String? = null
)
