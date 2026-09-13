package com.oneforth.cousininthecity.domain.usecase

import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import com.oneforth.cousininthecity.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetChatThreadsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<List<ChatThread>> = flow {
        val deviceId = userRepository.getOrCreateDeviceId()
        emitAll(chatRepository.getThreadsFlow(deviceId))
    }
    
    suspend fun refresh() {
        val deviceId = userRepository.getOrCreateDeviceId()
        chatRepository.refreshThreads(deviceId)
    }
    
    suspend fun togglePin(threadId: String, isPinned: Boolean) {
        chatRepository.togglePinStatus(threadId, isPinned)
    }
}

