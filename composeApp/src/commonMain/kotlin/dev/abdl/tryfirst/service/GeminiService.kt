package dev.abdl.tryfirst.service

import dev.abdl.tryfirst.BuildKonfig
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel

data class ProcessedOutput(
    val refinedEnglish: String? = null,
    val translatedToEnglish: String? = null,
    val translatedToBahasa: String? = null
)

class GeminiService {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildKonfig.GEMINI_API_KEY
    )

    private suspend fun callGemini(prompt: String): Result<String> {
        return try {
            val response = generativeModel.generateContent(prompt)
            Result.success(response.text?.trim() ?: "")
        } catch (e: Exception) {
            println("GeminiService internal call Error: ${e::class.simpleName} - ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun processNarration(
        inputText: String,
        originalLanguage: String
    ): Result<ProcessedOutput> {
        if (originalLanguage.startsWith("en")) {
            val refinePrompt = """
                Refine the following English narration to make it more polished, clear, natural, and engaging.
                Return *only* the refined English text, and nothing else. Do not include any preamble, explanation, or markdown formatting.

                Input text: "$inputText"
            """.trimIndent()

            val refinementResult = callGemini(refinePrompt)
            val refinedEnglish = refinementResult.getOrNull()

            if (refinementResult.isFailure || refinedEnglish.isNullOrBlank()) {
                val textToTranslateToBahasa = refinedEnglish ?: inputText
                if (textToTranslateToBahasa.isBlank()) {
                    return Result.failure(Exception("Refinement resulted in blank text, cannot proceed."))
                }


                val translateToBahasaPrompt = """
                    Translate the following English text accurately into fluent Bahasa Indonesia.
                    Return *only* the translated text, and nothing else. Do not include any preamble, explanation, or markdown formatting.

                    English text: "$textToTranslateToBahasa"
                """.trimIndent()
                val translationToBahasaResult = callGemini(translateToBahasaPrompt)
                val translatedToBahasa = translationToBahasaResult.getOrNull()

                if (translationToBahasaResult.isFailure && refinementResult.isFailure) {
                    return Result.failure(
                        refinementResult.exceptionOrNull()
                            ?: translationToBahasaResult.exceptionOrNull()
                            ?: Exception("Both refinement and translation failed")
                    )
                }

                return Result.success(
                    ProcessedOutput(
                        refinedEnglish = refinedEnglish,
                        translatedToBahasa = translatedToBahasa
                    )
                )
            }
            val translateToBahasaPrompt = """
                Translate the following English text accurately into fluent Bahasa Indonesia.
                Return *only* the translated text, and nothing else. Do not include any preamble, explanation, or markdown formatting.

                English text: "$refinedEnglish" 
            """.trimIndent()
            val translationToBahasaResult = callGemini(translateToBahasaPrompt)
            val translatedToBahasa = translationToBahasaResult.getOrNull()

            return Result.success(
                ProcessedOutput(
                    refinedEnglish = refinedEnglish,
                    translatedToBahasa = translatedToBahasa
                )
            )

        } else {
            val translateToEnglishPrompt = """
                Translate the following Bahasa Indonesia text accurately into fluent English.
                Return *only* the translated English text, and nothing else. Do not include any preamble, explanation, or markdown formatting.

                Input text: "$inputText"
            """.trimIndent()

            val translationToEnglishResult = callGemini(translateToEnglishPrompt)
            return if (translationToEnglishResult.isSuccess) {
                Result.success(ProcessedOutput(translatedToEnglish = translationToEnglishResult.getOrNull()))
            } else {
                Result.failure(
                    translationToEnglishResult.exceptionOrNull()
                        ?: Exception("Translation to English failed")
                )
            }
        }
    }
}
