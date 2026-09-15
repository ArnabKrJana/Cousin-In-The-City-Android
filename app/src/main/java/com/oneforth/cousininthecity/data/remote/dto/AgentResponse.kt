package com.oneforth.cousininthecity.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AgentResponse(
    @SerializedName("message") val message: String,
    @SerializedName("intentType") val intentType: String? = null,
    @SerializedName("actionData") val actionData: Map<String, String>? = null
)
