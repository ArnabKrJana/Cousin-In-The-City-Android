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
    val threadId: String? = null,
    val isChatLoading: Boolean = false
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
        
        // Optimistically add user message to UI and set chat loading state for shimmer effect
        val userMsg = ChatMessage(role = MessageRole.USER, content = prompt)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true, isChatLoading = true) }

        viewModelScope.launch {
            try {
                val responseMsg = sendMessageUseCase(prompt, currentThreadId).getOrThrow()
                
                // --- JARVIS INTERCEPTOR LOGIC ---
                // Clean the text and execute any XML intents found
                val cleanText = extractAndExecuteIntent(responseMsg.content)
                
                val finalMsg = responseMsg.copy(content = cleanText.trim())
                _uiState.update { it.copy(messages = it.messages + finalMsg, isLoading = false, isChatLoading = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isChatLoading = false) }
                _uiEvent.send(UiEvent.ShowSnackbar("Network error: Could not reach Agent Orchestrator."))
            }
        }
    }

    private fun extractAndExecuteIntent(text: String): String {
        var cleanText = text
        
        // 1. Aggressively strip any stray JSON tool-calling blocks
        val jsonPattern = Pattern.compile("```(?:json|JSON)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL)
        val jsonMatcher = jsonPattern.matcher(cleanText)
        while (jsonMatcher.find()) {
            val jsonContent = jsonMatcher.group(1) ?: ""
            if (jsonContent.contains("\"name\"") || jsonContent.contains("\"parameters\"")) {
                cleanText = cleanText.replace(jsonMatcher.group(0) ?: "", "")
            }
        }
        
        // 2. Find and execute XML INTENT tags
        val xmlPattern = Pattern.compile("<INTENT\\s+(.*?)\\s*/>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val xmlMatcher = xmlPattern.matcher(cleanText)
        
        while (xmlMatcher.find()) {
            val attributesStr = xmlMatcher.group(1) ?: ""
            val fullTag = xmlMatcher.group(0) ?: ""
            
            cleanText = cleanText.replace(fullTag, "")
            
            fun extractAttr(name: String): String {
                val attrPattern = Pattern.compile("$name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
                val attrMatcher = attrPattern.matcher(attributesStr)
                return if (attrMatcher.find()) attrMatcher.group(1) ?: "" else ""
            }
            
            val type = extractAttr("type").uppercase()
            val title = extractAttr("title").ifEmpty { "Cousin Assistant Event" }
            val date = extractAttr("date")
            val description = extractAttr("description").ifEmpty { "Automated by Cousin" }
            val location = extractAttr("location").ifEmpty { extractAttr("query") }.ifEmpty { "Mumbai" }
            val content = extractAttr("content").ifEmpty { extractAttr("note") }.ifEmpty { "Saved note" }
            
            viewModelScope.launch {
                when (type) {
                    "CALENDAR" -> _uiEvent.send(UiEvent.JarvisAddCalendar(title, description, date))
                    "MAP", "MAPS" -> _uiEvent.send(UiEvent.JarvisOpenMap(location))
                    "KEEP", "NOTE" -> _uiEvent.send(UiEvent.JarvisSaveNote(content))
                    else -> if (type.isNotBlank()) _uiEvent.send(UiEvent.ShowSnackbar("Found unknown intent: $type"))
                }
            }
        }
        
        return cleanText
    }
}
