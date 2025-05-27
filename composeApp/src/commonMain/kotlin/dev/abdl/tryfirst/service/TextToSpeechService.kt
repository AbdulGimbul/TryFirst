package dev.abdl.tryfirst.service


expect class TextToSpeechService {
    fun speak(
        text: String,
        languageCode: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (error: String) -> Unit
    )

    fun stop()
    fun isAvailable(): Boolean
}
