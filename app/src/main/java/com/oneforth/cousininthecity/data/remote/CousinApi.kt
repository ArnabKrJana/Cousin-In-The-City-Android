package com.oneforth.cousininthecity.data.remote

import com.oneforth.cousininthecity.data.remote.dto.AgentResponse
import com.oneforth.cousininthecity.data.remote.dto.AppUserDto
import com.oneforth.cousininthecity.data.remote.dto.ChatInputDto
import com.oneforth.cousininthecity.data.remote.dto.ChatThreadDto
import com.oneforth.cousininthecity.data.remote.dto.MessageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CousinApi {

    @POST("/api/chat/users/{deviceId}")
    suspend fun registerUser(
        @Path("deviceId") deviceId: String
    ): AppUserDto

    @GET("/api/chat/users/{deviceId}/threads")
    suspend fun getThreads(
        @Path("deviceId") deviceId: String
    ): List<ChatThreadDto>

    @POST("/api/chat/users/{deviceId}/threads")
    suspend fun createThread(
        @Path("deviceId") deviceId: String,
        @Query("title") title: String
    ): ChatThreadDto

    @GET("/api/chat/history/{threadId}")
    suspend fun getHistory(
        @Path("threadId") threadId: String
    ): List<MessageDto>

    @POST("/api/chat")
    suspend fun chat(
        @Body input: ChatInputDto
    ): AgentResponse
}
