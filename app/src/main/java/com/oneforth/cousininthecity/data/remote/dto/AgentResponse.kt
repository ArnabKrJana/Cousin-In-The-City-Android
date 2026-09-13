package com.oneforth.cousininthecity.data.remote.dto

data class AgentResponse(
    val message: String,
    val intentType: String? = null,
    val actionData: Map<String, String>? = null
)
