package com.oneforth.cousininthecity.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.MessageRole
import com.oneforth.cousininthecity.domain.usecase.GetChatHistoryUseCase
import com.oneforth.cousininthecity.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.regex.Pattern

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val threadId: String? = null,
    val isChatLoading: Boolean = false
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class JarvisAddCalendar(val title: String, val date: String, val time: String) : UiEvent()
    data class JarvisOpenMap(val locationQuery: String) : UiEvent()
    data class JarvisSaveNote(val title: String, val note: String) : UiEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var historyJob: Job? = null

    fun loadThread(threadId: String) {
        _uiState.update { it.copy(threadId = threadId, isLoading = true) }
        
        // Cancel previous collection if switching threads
        historyJob?.cancel()
        
        historyJob = viewModelScope.launch {
            getChatHistoryUseCase(threadId).collect { history ->
                _uiState.update { it.copy(messages = history, isLoading = false) }
            }
        }
        
        // Silently sync from backend
        viewModelScope.launch {
            try {
                getChatHistoryUseCase.refresh(threadId)
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to sync chat history. Swipe down to retry."))
            }
        }
    }

    fun toggleListening() {
        val currentlyListening = _uiState.value.isListening
        _uiState.update { it.copy(isListening = !currentlyListening) }
    }

    fun sendMessage(prompt: String) {
        val currentThreadId = _uiState.value.threadId ?: return
        
        _uiState.update { it.copy(isChatLoading = true) }

        viewModelScope.launch {
            try {
                // This triggers Option A: local save, API call, and failure cleanup.
                val responseMsg = sendMessageUseCase(prompt, currentThreadId).getOrThrow()
                
                // Intents and Fallbacks are handled via the newly returned message
                processStructuredIntent(responseMsg)
                extractAndExecuteFallbackIntent(responseMsg.content)
                
                _uiState.update { it.copy(isChatLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isChatLoading = false) }
                _uiEvent.send(UiEvent.ShowSnackbar("Network error: Could not reach Agent Orchestrator. Message unsent."))
            }
        }
    }

    private fun processStructuredIntent(message: ChatMessage) {
        val intentType = message.intentType?.uppercase() ?: return
        val actionData = message.actionData ?: emptyMap()

        viewModelScope.launch {
            when (intentType) {
                "MAP" -> {
                    val location = actionData["location"] ?: actionData["query"] ?: ""
                    if (location.isNotBlank()) {
                        _uiEvent.send(UiEvent.JarvisOpenMap(location))
                    }
                }
                "CALENDAR" -> {
                    val title = actionData["title"] ?: "New Event"
                    val date = actionData["date"] ?: ""
                    val time = actionData["time"] ?: ""
                    _uiEvent.send(UiEvent.JarvisAddCalendar(title, date, time))
                }
                "KEEP" -> {
                    val title = actionData["title"] ?: ""
                    val note = actionData["note"] ?: actionData["content"] ?: ""
                    _uiEvent.send(UiEvent.JarvisSaveNote(title, note))
                }
            }
        }
    }

    private fun extractAndExecuteFallbackIntent(text: String) {
        // Fallback processing for XML Intents that escaped structured JSON parsing
        val xmlPattern = Pattern.compile("<INTENT\\s+(.*?)\\s*/>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val xmlMatcher = xmlPattern.matcher(text)
        
        while (xmlMatcher.find()) {
            val attributesStr = xmlMatcher.group(1) ?: ""
            
            fun extractAttr(name: String): String {
                val attrPattern = Pattern.compile("$name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
                val attrMatcher = attrPattern.matcher(attributesStr)
                return if (attrMatcher.find()) attrMatcher.group(1) ?: "" else ""
            }
            
            val type = extractAttr("type").uppercase()
            val title = extractAttr("title").ifEmpty { "Cousin Assistant Event" }
            val date = extractAttr("date")
            val time = extractAttr("time")
            val location = extractAttr("location").ifEmpty { extractAttr("query") }.ifEmpty { "Mumbai" }
            val note = extractAttr("content").ifEmpty { extractAttr("note") }.ifEmpty { "Saved note" }
            
            viewModelScope.launch {
                when (type) {
                    "CALENDAR" -> _uiEvent.send(UiEvent.JarvisAddCalendar(title, date, time))
                    "MAP", "MAPS" -> _uiEvent.send(UiEvent.JarvisOpenMap(location))
                    "KEEP", "NOTE" -> _uiEvent.send(UiEvent.JarvisSaveNote(title, note))
                    else -> if (type.isNotBlank()) _uiEvent.send(UiEvent.ShowSnackbar("Found unknown intent: $type"))
                }
            }
        }
    }
}

