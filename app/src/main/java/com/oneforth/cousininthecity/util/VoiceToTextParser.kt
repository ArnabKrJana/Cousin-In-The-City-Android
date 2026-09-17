package com.oneforth.cousininthecity.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VoiceToTextParser(private val context: Context) {

    private var handler: SpeechRecognizerHandler? = null

    private val _state = MutableStateFlow(VoiceToTextParserState())
    val state: StateFlow<VoiceToTextParserState> = _state.asStateFlow()

    fun startListening(languageCode: String = "en-US") {
        _state.update { VoiceToTextParserState(isSpeaking = true, spokenText = "") }

        if (handler == null) {
            handler = SpeechRecognizerHandler(context) { newState ->
                _state.value = newState
            }
        }

        handler?.startListening(languageCode)
    }

    fun stopListening() {
        _state.update { it.copy(isSpeaking = false) }
        handler?.stopListening()
    }

    fun reset() {
        _state.value = VoiceToTextParserState()
    }

    fun destroy() {
        handler?.destroy()
        handler = null
    }
}

private class SpeechRecognizerHandler(
    private val context: Context,
    private val onStateUpdate: (VoiceToTextParserState) -> Unit
) : RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var currentState = VoiceToTextParserState()

    private fun updateState(transform: (VoiceToTextParserState) -> VoiceToTextParserState) {
        currentState = transform(currentState)
        onStateUpdate(currentState)
    }

    private fun ensureRecognizerCreated(): Boolean {
        if (recognizer != null) return true
        return try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@SpeechRecognizerHandler)
                }
                true
            } else {
                updateState { it.copy(error = "Speech recognition is not available on this device") }
                false
            }
        } catch (e: Exception) {
            updateState { it.copy(error = e.localizedMessage ?: "Speech recognition not supported") }
            false
        }
    }

    fun startListening(languageCode: String) {
        updateState { VoiceToTextParserState(isSpeaking = true, spokenText = "") }

        if (!ensureRecognizerCreated()) {
            updateState { it.copy(isSpeaking = false) }
            return
        }

        try {
            recognizer?.cancel()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            updateState {
                it.copy(
                    error = e.localizedMessage ?: "Failed to start speech recognition",
                    isSpeaking = false
                )
            }
        }
    }

    fun stopListening() {
        updateState { it.copy(isSpeaking = false) }
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            recognizer = null
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        updateState { it.copy(error = null) }
    }

    override fun onBeginningOfSpeech() {
        updateState { it.copy(isSpeaking = true) }
    }

    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        updateState { it.copy(isSpeaking = false) }
    }

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            updateState { it.copy(isSpeaking = false) }
            return
        }
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Speech recognition error ($error)"
        }
        updateState { it.copy(error = message, isSpeaking = false) }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()
        if (!text.isNullOrBlank()) {
            updateState { it.copy(spokenText = text, isSpeaking = false) }
        } else {
            updateState { it.copy(isSpeaking = false) }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()
        if (!text.isNullOrBlank()) {
            updateState { it.copy(spokenText = text) }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

data class VoiceToTextParserState(
    val spokenText: String = "",
    val isSpeaking: Boolean = false,
    val error: String? = null
)
