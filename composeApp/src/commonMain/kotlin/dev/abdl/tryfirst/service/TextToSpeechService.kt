package dev.abdl.tryfirst.service


expect class TextToSpeechService {
    fun speak(
        text: String,
        languageCode: String, // e.g., "en-US" for the output
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (error: String) -> Unit
    )
    fun stop()
    fun isAvailable(): Boolean
    // fun setLanguage(languageCode: String): Boolean
}
