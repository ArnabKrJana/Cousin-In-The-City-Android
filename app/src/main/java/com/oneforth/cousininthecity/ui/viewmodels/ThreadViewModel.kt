package com.oneforth.cousininthecity.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.domain.usecase.CreateChatThreadUseCase
import com.oneforth.cousininthecity.domain.usecase.GetChatThreadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThreadUiState(
    val threads: List<ChatThread> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val getChatThreadsUseCase: GetChatThreadsUseCase,
    private val createChatThreadUseCase: CreateChatThreadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThreadUiState())
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()

    init {
        // Collect offline-first Flow
        viewModelScope.launch {
            getChatThreadsUseCase().collect { threadList ->
                _uiState.update { it.copy(threads = threadList) }
            }
        }
    }

    fun loadThreads() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // Silently refresh in background
                getChatThreadsUseCase.refresh()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "Failed to sync threads from server.") 
                }
            }
        }
    }

    fun createNewThread(title: String, onThreadCreated: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val newThread = createChatThreadUseCase(title).getOrThrow()
                onThreadCreated(newThread.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create thread.") }
            }
        }
    }

    fun togglePin(threadId: String, currentPinStatus: Boolean) {
        viewModelScope.launch {
            getChatThreadsUseCase.togglePin(threadId, !currentPinStatus)
        }
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            try {
                getChatThreadsUseCase.deleteThread(threadId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete thread from server.") }
            }
        }
    }
}

