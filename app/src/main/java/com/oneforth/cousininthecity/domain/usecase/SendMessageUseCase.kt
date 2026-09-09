package com.oneforth.cousininthecity.domain.usecase

import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(prompt: String, conversationId: String): Result<ChatMessage> {
        return chatRepository.sendMessage(prompt, conversationId)
    }
}
