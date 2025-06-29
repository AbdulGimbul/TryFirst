package dev.abdl.tryfirst

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plusmobileapps.konnectivity.Konnectivity
import dev.abdl.tryfirst.service.GeminiService
import dev.abdl.tryfirst.service.SpeechToTextService
import dev.abdl.tryfirst.service.TextToSpeechService
import dev.abdl.tryfirst.storage.AppSettings
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val speechToTextService: SpeechToTextService,
    private val textToSpeechService: TextToSpeechService,
    private val geminiService: GeminiService,
    private val appSettings: AppSettings
) : ViewModel() {

    var appState by mutableStateOf(AppState.IDLE)
        private set
    var transcribedText by mutableStateOf("")
        private set
    var refinedText by mutableStateOf<String?>(null)
        private set
    var translatedToEnglishText by mutableStateOf<String?>(null)
        private set
    var translatedToBahasaText by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var selectedLanguage by mutableStateOf(InputLanguage.ENGLISH)
    var connectivity by mutableStateOf(Konnectivity())
        private set

    var inputMode by mutableStateOf(InputMode.SPEAK)
        private set
    var manualInputText by mutableStateOf("")
        private set

    val showOnboarding: StateFlow<Boolean> = appSettings.isFirstRunFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun onOnboardingCompleted() {
        viewModelScope.launch {
            appSettings.setFirstRunCompleted()
        }
    }

    fun handleProcessAction(permissionsController: PermissionsController) {

        clearResultTexts()
        errorMessage = null

        when (inputMode) {
            InputMode.SPEAK -> {
                appState = AppState.REQUESTING_PERMISSION
                viewModelScope.launch {
                    try {
                        permissionsController.providePermission(Permission.RECORD_AUDIO)
                        startActualListening()
                    } catch (e: DeniedException) {
                        errorMessage =
                            "Microphone permission denied. Please grant it in app settings."
                        appState = AppState.ERROR
                    } catch (e: Exception) {
                        errorMessage = "Permission request failed: ${e.message}"
                        appState = AppState.ERROR
                    }
                }
            }

            InputMode.TEXT -> {
                if (manualInputText.isNotBlank()) {
                    transcribedText = manualInputText
                    processTranscribedText()
                } else {
                    errorMessage = "Please enter text to process."
                    appState = AppState.IDLE
                }
            }
        }
    }

    private fun clearResultTexts() {
        refinedText = null
        translatedToEnglishText = null
        translatedToBahasaText = null
    }

    private fun startActualListening() {
        if (!speechToTextService.isAvailable()) {
            errorMessage = "Speech recognition is not available on this device."
            appState = AppState.ERROR
            return
        }
        appState = AppState.LISTENING
        transcribedText = ""
        clearResultTexts()

        speechToTextService.startListening(
            languageCode = selectedLanguage.code,
            onResult = { text, isFinal ->
                transcribedText = text
            },
            onError = { error ->
                errorMessage = "STT Error: $error"
                appState = AppState.ERROR
            },
            onEndOfSpeech = {
                if (transcribedText.isNotBlank() && appState == AppState.LISTENING) {
                    processTranscribedText()
                } else if (appState == AppState.LISTENING) {
                    appState = AppState.IDLE
                }
            }
        )
    }

    fun stopListeningAndProcess() {
        if (appState == AppState.LISTENING) {
            speechToTextService.stopListening()
            if (transcribedText.isNotBlank() && appState != AppState.PROCESSING) {
                processTranscribedText()
            } else if (appState != AppState.PROCESSING) {
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
        viewModelScope.launch {
            val result = geminiService.processNarration(transcribedText, selectedLanguage.code)
            result.fold(
                onSuccess = { output ->
                    var textToSpeak: String? = null
                    if (selectedLanguage == InputLanguage.ENGLISH) {
                        refinedText = output.refinedEnglish
                        translatedToBahasaText = output.translatedToBahasa
                        textToSpeak = output.refinedEnglish
                    } else {
                        translatedToEnglishText = output.translatedToEnglish
                        textToSpeak =
                            output.translatedToEnglish
                    }

                    if (!textToSpeak.isNullOrBlank()) {
                        speakText(textToSpeak, "en-US")
                    } else {
                        appState = AppState.IDLE
                    }
                },
                onFailure = { error ->
                    errorMessage = "Gemini AI Error: ${error.message}"
                    appState = AppState.ERROR
                }
            )
        }
    }

    private fun speakText(text: String, languageCode: String) {
        if (text.isBlank() || !textToSpeechService.isAvailable()) {
            errorMessage =
                if (text.isBlank()) "Nothing to speak." else "Text-to-speech is not available."
            appState = if (appState != AppState.ERROR) AppState.IDLE else AppState.ERROR
            return
        }
        appState = AppState.SPEAKING
        textToSpeechService.speak(
            text = text,
            languageCode = languageCode,
            onStart = { },
            onDone = { appState = AppState.IDLE },
            onError = { error ->
                errorMessage = "TTS Error: $error"
                appState = AppState.ERROR
            }
        )
    }

    fun onLanguageSelected(language: InputLanguage) {
        selectedLanguage = language
        if (appState == AppState.LISTENING) {
            speechToTextService.stopListening()
            appState = AppState.IDLE
        }
    }

    fun onInputModeChanged(newMode: InputMode) {
        if (inputMode == InputMode.SPEAK && appState == AppState.LISTENING) {
            speechToTextService.stopListening()
        }
        inputMode = newMode
        appState = AppState.IDLE
        transcribedText = ""
        manualInputText = ""
        clearResultTexts()
        errorMessage = null
    }

    fun onManualInputTextChanged(newText: String) {
        manualInputText = newText
        if (appState == AppState.ERROR && newText.isNotBlank()) {
            errorMessage = null
        }
    }

    public override fun onCleared() {
        super.onCleared()
        speechToTextService.stopListening()
        textToSpeechService.stop()
    }
}
