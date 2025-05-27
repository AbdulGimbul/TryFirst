package dev.abdl.tryfirst.service

expect class SpeechToTextService {
    fun startListening(
        languageCode: String,
        onResult: (text: String, isFinal: Boolean) -> Unit,
        onError: (error: String) -> Unit,
        onEndOfSpeech: () -> Unit
    )

    fun stopListening()
    fun isAvailable(): Boolean
}