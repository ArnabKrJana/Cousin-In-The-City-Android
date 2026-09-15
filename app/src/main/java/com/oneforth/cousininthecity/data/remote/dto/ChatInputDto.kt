package com.oneforth.cousininthecity.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatInputDto(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("conversationId") val conversationId: String
)
