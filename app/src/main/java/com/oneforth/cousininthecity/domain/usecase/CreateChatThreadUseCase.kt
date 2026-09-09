package com.oneforth.cousininthecity.domain.usecase

import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import com.oneforth.cousininthecity.domain.repository.UserRepository
import javax.inject.Inject

class CreateChatThreadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(title: String): Result<ChatThread> {
        val deviceId = userRepository.getOrCreateDeviceId()
        return chatRepository.createThread(deviceId, title)
    }
}
