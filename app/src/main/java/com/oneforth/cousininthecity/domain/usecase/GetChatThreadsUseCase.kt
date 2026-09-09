package com.oneforth.cousininthecity.domain.usecase

import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import com.oneforth.cousininthecity.domain.repository.UserRepository
import javax.inject.Inject

class GetChatThreadsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<List<ChatThread>> {
        val deviceId = userRepository.getOrCreateDeviceId()
        return chatRepository.getThreads(deviceId)
    }
}
