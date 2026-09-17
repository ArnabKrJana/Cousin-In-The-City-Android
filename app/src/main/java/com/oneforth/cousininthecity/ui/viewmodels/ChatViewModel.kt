package com.oneforth.cousininthecity.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.gson.JsonSyntaxException
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.usecase.GetChatHistoryUseCase
import com.oneforth.cousininthecity.domain.usecase.CreateChatThreadUseCase
import com.oneforth.cousininthecity.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.regex.Pattern
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val threadId: String? = null,
    val isChatLoading: Boolean = false,
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
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val createChatThreadUseCase: CreateChatThreadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatHistory: Flow<PagingData<ChatMessage>> = _uiState
        .flatMapLatest { state ->
            val id = state.threadId
            if (id == null) {
                emptyFlow()
            } else {
                getChatHistoryUseCase(id).cachedIn(viewModelScope)
            }
        }

    fun loadThread(threadId: String) {
        _uiState.update { it.copy(threadId = threadId, isLoading = false) }
        
        viewModelScope.launch {
            try {
                getChatHistoryUseCase.refresh(threadId)
            } catch (e: Exception) {
                Log.w("ChatViewModel", "Failed to sync history for thread $threadId", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to sync chat history. Swipe down to retry."))
            }
        }
    }

    fun clearThread() {
        _uiState.update { it.copy(threadId = null, isLoading = false) }
    }

    fun sendMessage(prompt: String) {
        _uiState.update { it.copy(isChatLoading = true) }

        viewModelScope.launch {
            try {
                val currentThreadId = _uiState.value.threadId ?: run {
                    val generatedTitle = prompt.take(30).trim() + if (prompt.length > 30) "..." else ""
                    val newThread = createChatThreadUseCase(generatedTitle).getOrThrow()
                    _uiState.update { it.copy(threadId = newThread.id) }
                    newThread.id
                }

                val responseMsg = sendMessageUseCase(prompt, currentThreadId).getOrThrow()
                
                processStructuredIntent(responseMsg)
                extractAndExecuteFallbackIntent(responseMsg.content)
                
                _uiState.update { it.copy(isChatLoading = false) }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "sendMessage failed", e)
                _uiState.update { it.copy(isChatLoading = false) }
                val errorMessage = when (e) {
                    is HttpException -> {
                        val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                        "HTTP ${e.code()}: ${errorBody?.take(120) ?: e.message()}"
                    }
                    is ConnectException -> "Connection refused: Check server at 192.168.0.103:8080"
                    is SocketTimeoutException -> "Connection timed out to 192.168.0.103:8080"
                    is JsonSyntaxException -> "JSON format mismatch from server: ${e.message}"
                    else -> e.localizedMessage ?: "Error: ${e.javaClass.simpleName}"
                }
                _uiEvent.send(UiEvent.ShowSnackbar(errorMessage))
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
