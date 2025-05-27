package dev.abdl.tryfirst

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abdl.tryfirst.service.GeminiService
import dev.abdl.tryfirst.service.SpeechToTextService
import dev.abdl.tryfirst.service.TextToSpeechService
import kotlinx.coroutines.launch

enum class AppState {
    IDLE, LISTENING, PROCESSING, SPEAKING, ERROR
}

enum class InputLanguage(val code: String, val displayName: String) {
    ENGLISH("en-US", "English"),
    INDONESIAN("id-ID", "Bahasa Indonesia")
}

class VoiceViewModel(
    private val speechToTextService: SpeechToTextService,
    private val textToSpeechService: TextToSpeechService,
    private val geminiService: GeminiService
) : ViewModel() {

    var appState by mutableStateOf(AppState.IDLE)
        private set

    var transcribedText by mutableStateOf("")
        private set

    var processedText by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedLanguage by mutableStateOf(InputLanguage.ENGLISH)


    fun startListening() {
        if (!speechToTextService.isAvailable()) {
            errorMessage = "Speech recognition is not available on this device."
            appState = AppState.ERROR
            return
        }
        appState = AppState.LISTENING
        transcribedText = ""
        processedText = ""
        errorMessage = null

        speechToTextService.startListening(
            languageCode = selectedLanguage.code,
            onResult = { text, isFinal ->
                transcribedText = text
                if (isFinal) {
                    // Some STT services might call onResult multiple times with isFinal=true
                    // or might have a separate onEndOfSpeech. Adjust logic as needed.
                    // For simplicity, we process when isFinal is true.
                    // If onEndOfSpeech is more reliable for your chosen STT, use that.
                    // stopListening() // Stop explicitly if not stopped by onEndOfSpeech
                    // processTranscribedText()
                }
            },
            onError = { error ->
                errorMessage = "STT Error: $error"
                appState = AppState.ERROR
                // Consider if stopListening should be called here
            },
            onEndOfSpeech = {
                // This is often a better place to trigger processing
                if (transcribedText.isNotBlank() && appState == AppState.LISTENING) {
                    // Ensure we only process if we were actively listening and got some text
                    processTranscribedText()
                } else if (appState == AppState.LISTENING) {
                    // No speech detected or transcribed text is empty
                    appState = AppState.IDLE
                }
            }
        )
    }

    fun stopListeningAndProcess() {
        if (appState == AppState.LISTENING) {
            speechToTextService.stopListening() // This should ideally trigger onEndOfSpeech or a final result
            // If stopListening itself doesn't trigger a final callback with text,
            // you might need to process with the current `transcribedText` if it's not blank.
            // This depends heavily on the STT implementation.
            // For now, we assume onEndOfSpeech or a final onResult handles processing.
            // If transcribedText is populated but onEndOfSpeech wasn't called, call process.
            if (transcribedText.isNotBlank()) {
                processTranscribedText()
            } else {
                appState = AppState.IDLE
            }
        }
    }


    private fun processTranscribedText() {
        if (transcribedText.isBlank()) {
            appState = AppState.IDLE
            return
        }
        appState = AppState.PROCESSING
        viewModelScope.launch { // Use viewModelScope from moko-mvvm or lifecycle-viewmodel-ktx
            val result = geminiService.processNarration(transcribedText, selectedLanguage.code)
            result.fold(
                onSuccess = { refinedOrTranslatedText ->
                    processedText = refinedOrTranslatedText
                    speakProcessedText()
                },
                onFailure = { error ->
                    errorMessage = "Gemini AI Error: ${error.message}"
                    appState = AppState.ERROR
                }
            )
        }
    }

    private fun speakProcessedText() {
        if (processedText.isBlank() || !textToSpeechService.isAvailable()) {
            errorMessage = if (processedText.isBlank()) "Nothing to speak." else "Text-to-speech is not available."
            appState = if (appState != AppState.ERROR) AppState.IDLE else AppState.ERROR
            return
        }
        appState = AppState.SPEAKING
        // Output is always English after Gemini processing
        textToSpeechService.speak(
            text = processedText,
            languageCode = "en-US", // Output is always English
            onStart = { /* Potentially update UI */ },
            onDone = { appState = AppState.IDLE },
            onError = { error ->
                errorMessage = "TTS Error: $error"
                appState = AppState.ERROR
            }
        )
    }

    fun onLanguageSelected(language: InputLanguage) {
        selectedLanguage = language
        if (appState == AppState.LISTENING) { // If listening, stop and restart with new lang
            speechToTextService.stopListening()
            startListening()
        }
    }

    // Call this when the ViewModel is cleared to release resources
    public override fun onCleared() { // For moko-mvvm ViewModel
        super.onCleared()
        speechToTextService.stopListening() // Ensure STT is stopped
        textToSpeechService.stop()      // Ensure TTS is stopped
    }
}
