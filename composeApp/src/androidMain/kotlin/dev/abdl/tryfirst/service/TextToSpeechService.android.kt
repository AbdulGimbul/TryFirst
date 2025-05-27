package dev.abdl.tryfirst.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

actual class TextToSpeechService(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var onDoneCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onStartCallback: (() -> Unit)? = null
    private var pendingText: String? = null
    private var pendingLanguageCode: String? = null


    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onStartCallback?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    onDoneCallback?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onErrorCallback?.invoke("TTS Error")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onErrorCallback?.invoke("TTS Error with code: $errorCode")
                }
            })
            pendingText?.let { text ->
                pendingLanguageCode?.let { langCode ->
                    speakInternal(text, langCode)
                }
            }
            pendingText = null
            pendingLanguageCode = null
        } else {
            isTtsInitialized = false
            onErrorCallback?.invoke("TTS Initialization Failed")
        }
    }

    actual fun isAvailable(): Boolean = true

    actual fun speak(
        text: String,
        languageCode: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        onStartCallback = onStart
        onDoneCallback = onDone
        onErrorCallback = onError

        if (isTtsInitialized) {
            speakInternal(text, languageCode)
        } else {
            pendingText = text
            pendingLanguageCode = languageCode
            if (tts == null) {
                onError("TTS service could not be created.")
            }
        }
    }

    private fun speakInternal(text: String, languageCode: String) {
        val locale = Locale.forLanguageTag(languageCode)
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            onErrorCallback?.invoke("Language $languageCode not supported by TTS.")
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    actual fun stop() {
        tts?.stop()
    }
}
