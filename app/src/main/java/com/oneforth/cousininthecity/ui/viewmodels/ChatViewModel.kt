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

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val threadId: String? = null,
    val isChatLoading: Boolean = false
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class JarvisAddCalendar(val title: String, val date: String) : UiEvent()
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
        
        val userMsg = ChatMessage(role = MessageRole.USER, content = prompt)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true, isChatLoading = true) }

        viewModelScope.launch {
            try {
                val responseMsg = sendMessageUseCase(prompt, currentThreadId).getOrThrow()
                
                // Process structured intent response
                processStructuredIntent(responseMsg)
                
                // Fallback: clean any stray legacy tags in content string
                val cleanText = extractAndExecuteFallbackIntent(responseMsg.content)
                val finalMsg = responseMsg.copy(content = cleanText.trim())
                
                _uiState.update { it.copy(messages = it.messages + finalMsg, isLoading = false, isChatLoading = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isChatLoading = false) }
                _uiEvent.send(UiEvent.ShowSnackbar("Network error: Could not reach Agent Orchestrator."))
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
                    _uiEvent.send(UiEvent.JarvisAddCalendar(title, date))
                }
                "KEEP" -> {
                    val title = actionData["title"] ?: ""
                    val note = actionData["note"] ?: actionData["content"] ?: ""
                    _uiEvent.send(UiEvent.JarvisSaveNote(title, note))
                }
            }
        }
    }

    private fun extractAndExecuteFallbackIntent(text: String): String {
        var cleanText = text
        
        val jsonPattern = Pattern.compile("```(?:json|JSON)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL)
        val jsonMatcher = jsonPattern.matcher(cleanText)
        while (jsonMatcher.find()) {
            val jsonContent = jsonMatcher.group(1) ?: ""
            if (jsonContent.contains("\"name\"") || jsonContent.contains("\"parameters\"")) {
                cleanText = cleanText.replace(jsonMatcher.group(0) ?: "", "")
            }
        }
        
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
            val location = extractAttr("location").ifEmpty { extractAttr("query") }.ifEmpty { "Mumbai" }
            val note = extractAttr("content").ifEmpty { extractAttr("note") }.ifEmpty { "Saved note" }
            
            viewModelScope.launch {
                when (type) {
                    "CALENDAR" -> _uiEvent.send(UiEvent.JarvisAddCalendar(title, date))
                    "MAP", "MAPS" -> _uiEvent.send(UiEvent.JarvisOpenMap(location))
                    "KEEP", "NOTE" -> _uiEvent.send(UiEvent.JarvisSaveNote(title, note))
                    else -> if (type.isNotBlank()) _uiEvent.send(UiEvent.ShowSnackbar("Found unknown intent: $type"))
                }
            }
        }
        
        return cleanText
    }
}
