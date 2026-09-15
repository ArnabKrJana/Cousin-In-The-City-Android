package com.oneforth.cousininthecity.domain.usecase

import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import androidx.paging.PagingData

class GetChatHistoryUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(threadId: String): Flow<PagingData<ChatMessage>> {
        return repository.getHistoryFlow(threadId)
    }
    
    suspend fun refresh(threadId: String) {
        repository.refreshHistory(threadId)
    }
}

