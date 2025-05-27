package dev.abdl.tryfirst.service

expect class SpeechToTextService {
    fun startListening(
        languageCode: String, // e.g., "en-US" or "id-ID"
        onResult: (text: String, isFinal: Boolean) -> Unit,
        onError: (error: String) -> Unit,
        onEndOfSpeech: () -> Unit
    )
    fun stopListening()
    fun isAvailable(): Boolean
    // Optional: Request permissions if needed, though this is often UI-driven
    // suspend fun requestPermission(): Boolean
}