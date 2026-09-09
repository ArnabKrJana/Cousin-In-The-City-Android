package com.oneforth.cousininthecity.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.MessageRole
import com.oneforth.cousininthecity.domain.usecase.GetChatHistoryUseCase
import com.oneforth.cousininthecity.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.regex.Pattern

// Represents the state of the Chat Screen
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val threadId: String? = null
)

// Represents one-off events sent via SharedFlow (Channel) to the UI
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    
    // Jarvis Actions
    data class JarvisAddCalendar(val title: String, val description: String, val date: String) : UiEvent()
    data class JarvisOpenMap(val locationQuery: String) : UiEvent()
    data class JarvisSaveNote(val content: String) : UiEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Channel for one-off events (like Snackbar or Jarvis intents)
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun loadThread(threadId: String) {
        _uiState.update { it.copy(threadId = threadId, isLoading = true) }
        viewModelScope.launch {
            try {
                val history = getChatHistoryUseCase(threadId).getOrNull() ?: emptyList()
                _uiState.update { it.copy(messages = history, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to load chat history. Swipe down to retry."))
            }
        }
    }

    fun toggleListening() {
        val currentlyListening = _uiState.value.isListening
        _uiState.update { it.copy(isListening = !currentlyListening) }
    }

    fun sendMessage(prompt: String) {
        val currentThreadId = _uiState.value.threadId ?: return
        
        // Optimistically add user message to UI
        val userMsg = ChatMessage(role = MessageRole.USER, content = prompt)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true) }

        viewModelScope.launch {
            try {
                val responseMsg = sendMessageUseCase(prompt, currentThreadId).getOrThrow()
                
                // --- JARVIS INTERCEPTOR LOGIC ---
                // We check if the AI appended a JSON intent block to its response
                val (cleanText, intentJson) = extractJsonBlock(responseMsg.content)
                
                val finalMsg = responseMsg.copy(content = cleanText.trim())
                _uiState.update { it.copy(messages = it.messages + finalMsg, isLoading = false) }
                
                // If a JSON intent was found, trigger the silent Jarvis automation via SharedFlow
                intentJson?.let { executeJarvisAutomation(it) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.send(UiEvent.ShowSnackbar("Network error: Could not reach Agent Orchestrator."))
            }
        }
    }

    private fun extractJsonBlock(text: String): Pair<String, String?> {
        val pattern = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL)
        val matcher = pattern.matcher(text)
        
        if (matcher.find()) {
            val json = matcher.group(1)
            val cleanText = text.replace(matcher.group(0) ?: "", "")
            return Pair(cleanText, json)
        }
        return Pair(text, null)
    }

    private fun executeJarvisAutomation(json: String) {
        viewModelScope.launch {
            try {
                // In a real scenario, use Gson/Kotlinx Serialization to parse this
                if (json.contains("\"intent\": \"CALENDAR\"")) {
                    _uiEvent.send(UiEvent.JarvisAddCalendar(
                        title = "Flight Booked", 
                        description = "Automated by CousinInTheCity",
                        date = "2026-10-12"
                    ))
                } else if (json.contains("\"intent\": \"MAP\"")) {
                    _uiEvent.send(UiEvent.JarvisOpenMap("Andheri West, Mumbai"))
                } else if (json.contains("\"intent\": \"KEEP\"")) {
                    _uiEvent.send(UiEvent.JarvisSaveNote("Here are your neighborhood options..."))
                }
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar("Jarvis automation failed to parse JSON."))
            }
        }
    }
}
