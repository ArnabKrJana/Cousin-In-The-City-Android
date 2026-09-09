package com.oneforth.cousininthecity.domain.usecase

import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import javax.inject.Inject

class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(threadId: String): Result<List<ChatMessage>> {
        return chatRepository.getHistory(threadId)
    }
}
