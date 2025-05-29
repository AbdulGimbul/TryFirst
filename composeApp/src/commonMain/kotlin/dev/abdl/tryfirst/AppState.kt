package dev.abdl.tryfirst

enum class AppState {
    IDLE, REQUESTING_PERMISSION, LISTENING, PROCESSING, SPEAKING, ERROR
}

enum class InputLanguage(val code: String, val displayName: String) {
    ENGLISH("en-US", "English"),
    INDONESIAN("id-ID", "Bahasa Indonesia")
}

data class ProcessedOutput(
    val refinedEnglish: String? = null,
    val translatedToEnglish: String? = null,
    val translatedToBahasa: String? = null
)