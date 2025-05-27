package dev.abdl.tryfirst.service

import dev.abdl.tryfirst.BuildKonfig
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel

class GeminiService {
    // Consider making modelName configurable or choosing based on specific needs
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash", // Or "gemini-pro" or other suitable models
        apiKey = BuildKonfig.GEMINI_API_KEY
    )

    suspend fun processNarration(inputText: String, originalLanguage: String): Result<String> {
        return try {
            val taskInstruction = if (originalLanguage.startsWith("en")) {
                "Refine the following English narration to make it more polished, clear, natural, and engaging."
            } else { // Assuming "id" for Bahasa Indonesia
                "Translate the following Bahasa Indonesia text accurately into fluent English."
            }

            val prompt = """
                $taskInstruction
                Return *only* the refined English text or the translated English text, and nothing else. Do not include any preamble, explanation, or markdown formatting.

                Input text: "$inputText"
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            Result.success(response.text?.trim() ?: "")
        } catch (e: Exception) {
            // Log the exception e.g. println("GeminiService Error: ${e.message}")
            Result.failure(e)
        }
    }
}
