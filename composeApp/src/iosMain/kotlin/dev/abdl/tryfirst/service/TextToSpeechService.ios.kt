package dev.abdl.tryfirst.service

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class TextToSpeechService : NSObject(), AVSpeechSynthesizerDelegateProtocol {
    private val synthesizer = AVSpeechSynthesizer()
    private var onDoneCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onStartCallback: (() -> Unit)? = null

    init {
        synthesizer.delegate = this
    }

    actual fun isAvailable(): Boolean = true // iOS TTS is generally available

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

        try {
            val utterance = AVSpeechUtterance(string = text)
            utterance.voice =
                AVSpeechSynthesisVoice.voiceWithLanguage(languageCode) // e.g., "en-US"
            // You can adjust rate and pitch if needed:
            // utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9f
            // utterance.pitchMultiplier = 1.0f

            if (synthesizer.speaking) {
                synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            }
            synthesizer.speakUtterance(utterance)
        } catch (e: Exception) {
            onError("TTS speak failed: ${e.message}")
        }
    }

    actual fun stop() {
        if (synthesizer.speaking) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
    }

    // AVSpeechSynthesizerDelegate methods
    override fun speechSynthesizer(
        synthesizer: AVSpeechSynthesizer,
        didStartSpeechUtterance: AVSpeechUtterance
    ) {
        onStartCallback?.invoke()
    }

    override fun speechSynthesizer(
        synthesizer: AVSpeechSynthesizer,
        didFinishSpeechUtterance: AVSpeechUtterance
    ) {
        onDoneCallback?.invoke()
    }

    override fun speechSynthesizer(
        synthesizer: AVSpeechSynthesizer,
        didCancelSpeechUtterance: AVSpeechUtterance
    ) {
        // Treat cancel as done or handle differently if needed
        onDoneCallback?.invoke()
    }

    override fun speechSynthesizer(
        synthesizer: AVSpeechSynthesizer,
        didPauseSpeechUtterance: AVSpeechUtterance
    ) {
    }

    override fun speechSynthesizer(
        synthesizer: AVSpeechSynthesizer,
        didContinueSpeechUtterance: AVSpeechUtterance
    ) {
    }

    override fun speechSynthesizer(
        synthesizer: AVSpeechSynthesizer,
        willSpeakRangeOfSpeechString: platform.Foundation.NSRange,
        utterance: AVSpeechUtterance
    ) {
    }

    // Note: The basic `onError` in the `speak` function handles synchronous errors.
    // For asynchronous errors during speech, AVSpeechSynthesizerDelegate doesn't have a direct `didError` method.
    // Errors are typically caught during utterance creation or if the voice is unavailable.
}
