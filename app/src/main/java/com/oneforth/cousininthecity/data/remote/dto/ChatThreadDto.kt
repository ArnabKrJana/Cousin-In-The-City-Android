package com.oneforth.cousininthecity.data.remote.dto

data class ChatThreadDto(
    val id: String,
    val deviceId: String,
    val title: String,
    val updatedAt: String? = null
)
