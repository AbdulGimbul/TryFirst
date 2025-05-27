package dev.abdl.tryfirst.service

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class SpeechToTextService {
    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: platform.Speech.SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: platform.Speech.SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()

    private var onResultCallback: ((String, Boolean) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onEndOfSpeechCallback: (() -> Unit)? = null

    actual fun isAvailable(): Boolean = SFSpeechRecognizer.supportedLocales().isNotEmpty()

    actual fun startListening(
        languageCode: String,
        onResult: (String, Boolean) -> Unit,
        onError: (String) -> Unit,
        onEndOfSpeech: () -> Unit
    ) {
        // Permission is assumed to be granted by the ViewModel using Moko Permissions
        this.onResultCallback = onResult
        this.onErrorCallback = onError
        this.onEndOfSpeechCallback = onEndOfSpeech

        // Initialize recognizer for the specific language
        val locale = platform.Foundation.NSLocale(localeIdentifier = languageCode)
        speechRecognizer = SFSpeechRecognizer(locale)

        if (speechRecognizer?.available != true) {
            onErrorCallback?.invoke("Speech recognizer for $languageCode is not available.")
            return
        }
        recognitionTask?.cancel()
        recognitionTask = null

        val audioSession = platform.AVFAudio.AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(
                platform.AVFAudio.AVAudioSessionCategoryRecord,
                mode = platform.AVFAudio.AVAudioSessionModeMeasurement,
                options = platform.AVFAudio.AVAudioSessionCategoryOptionsDuckOthers
            )
            audioSession.setActive(true, null)
        } catch (e: Exception) {
            onErrorCallback?.invoke("Audio session setup failed: ${e.message}")
            return
        }

        recognitionRequest = platform.Speech.SFSpeechAudioBufferRecognitionRequest()
        val inputNode: AVAudioInputNode = audioEngine.inputNode

        recognitionRequest?.let { req ->
            req.shouldReportPartialResults = true
            recognitionTask = speechRecognizer?.recognitionTaskWithRequest(req) { result, error ->
                var isFinalResult = false
                if (result != null) {
                    val bestTranscription = result.bestTranscription.formattedString
                    isFinalResult = result.isFinal
                    onResultCallback?.invoke(bestTranscription, isFinalResult)
                }

                if (error != null || isFinalResult) {
                    stopAudioEngineAndSession()
                    recognitionRequest = null
                    if (isFinalResult && error == null) {
                        onEndOfSpeechCallback?.invoke()
                    } else if (error != null) {
                        onErrorCallback?.invoke("Recognition error: ${error.localizedDescription}")
                    }
                }
            }
        }
        val recordingFormat = inputNode.outputFormatForBus(0u)
        try {
            inputNode.installTapOnBus(0u, 1024u, recordingFormat) { buffer, _ ->
                recognitionRequest?.appendAudioPCMBuffer(buffer!!)
            }
            audioEngine.prepare()
            audioEngine.startAndReturnError(null)
        } catch (e: Exception) {
            onErrorCallback?.invoke("Audio engine start failed: ${e.message}")
            stopAudioEngineAndSession()
        }
    }

    actual fun stopListening() {
        if (audioEngine.running) {
            audioEngine.stop()
            recognitionRequest?.endAudio()
        }
    }
}
