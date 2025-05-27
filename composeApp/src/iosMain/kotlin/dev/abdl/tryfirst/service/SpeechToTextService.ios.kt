package dev.abdl.tryfirst.service

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class SpeechToTextService {
    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()

    private var onResultCallback: ((String, Boolean) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onEndOfSpeechCallback: (() -> Unit)? = null // Less explicit on iOS, managed by task state

    actual fun isAvailable(): Boolean = SFSpeechRecognizer.supportedLocales().isNotEmpty()

    private fun requestPermission(callback: (Boolean) -> Unit) {
        when (SFSpeechRecognizer.authorizationStatus()) {
            SFSpeechRecognizerAuthorizationStatusAuthorized -> callback(true)
            SFSpeechRecognizerAuthorizationStatusNotDetermined -> {
                SFSpeechRecognizer.requestAuthorization { status ->
                    dispatch_async(dispatch_get_main_queue()) {
                        callback(status == SFSpeechRecognizerAuthorizationStatusAuthorized)
                    }
                }
            }
            SFSpeechRecognizerAuthorizationStatusDenied,
            SFSpeechRecognizerAuthorizationStatusRestricted -> callback(false)
            else -> callback(false) // Should not happen
        }
    }


    actual fun startListening(
        languageCode: String,
        onResult: (String, Boolean) -> Unit,
        onError: (String) -> Unit,
        onEndOfSpeech: () -> Unit
    ) {
        onResultCallback = onResult
        onErrorCallback = onError
        onEndOfSpeechCallback = onEndOfSpeech // Store it, though iOS STT flow is a bit different

        requestPermission { granted ->
            if (!granted) {
                onErrorCallback?.invoke("Speech recognition permission denied.")
                return@requestPermission
            }

            // Initialize recognizer for the specific language
            val locale = NSLocale(localeIdentifier = languageCode) // e.g., "en-US" or "id-ID"
            speechRecognizer = SFSpeechRecognizer(locale)

            if (speechRecognizer?.available != true) {
                onErrorCallback?.invoke("Speech recognizer for $languageCode is not available.")
                return@requestPermission
            }


            // Cancel any previous task
            recognitionTask?.cancel()
            recognitionTask = null

            val audioSession = AVAudioSession.sharedInstance()
            try {
                audioSession.setCategory(AVAudioSessionCategoryRecord, mode = platform.AVFAudio.AVAudioSessionModeMeasurement, options = platform.AVFAudio.AVAudioSessionCategoryOptionsDuckOthers)
                audioSession.setActive(true, null) // Pass null for options on newer APIs, or specific options if needed
            } catch (e: Exception) {
                onErrorCallback?.invoke("Audio session setup failed: ${e.message}")
                return@requestPermission
            }

            recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
            val inputNode: AVAudioInputNode = audioEngine.inputNode

            recognitionRequest?.let { req ->
                req.shouldReportPartialResults = true // Get partial results
                recognitionTask = speechRecognizer?.recognitionTaskWithRequest(req) { result, error ->
                    var isFinal = false
                    if (result != null) {
                        val bestTranscription = result.bestTranscription.formattedString
                        isFinal = result.isFinal
                        onResultCallback?.invoke(bestTranscription, isFinal)
                    }

                    if (error != null || isFinal) {
                        stopAudioEngineAndSession()
                        recognitionRequest = null
                        recognitionTask = null
                        if (isFinal && error == null) {
                            onEndOfSpeechCallback?.invoke() // Call when task is final and successful
                        } else if (error != null) {
                            onErrorCallback?.invoke("Recognition error: ${error.localizedDescription}")
                        }
                    }
                }
            }


            val recordingFormat = inputNode.outputFormatForBus(0u)
            try {
                inputNode.installTapOnBus(0u, 1024u, recordingFormat) { buffer, _ -> // Buffer size 1024
                    recognitionRequest?.appendAudioPCMBuffer(buffer!!)
                }
                audioEngine.prepare()
                audioEngine.startAndReturnError(null) // Pass null for error pointer
            } catch (e: Exception) {
                onErrorCallback?.invoke("Audio engine start failed: ${e.message}")
                stopAudioEngineAndSession()
            }
        }
    }

    private fun stopAudioEngineAndSession() {
        audioEngine.stop()
        audioEngine.inputNode.removeTapOnBus(0u)
        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setActive(false, null)
        } catch (e: Exception) {
            // Log error if needed: println("Error deactivating audio session: ${e.message}")
        }
    }


    actual fun stopListening() {
        if (audioEngine.running) {
            audioEngine.stop()
            recognitionRequest?.endAudio() // Important to signal end of audio
            // Don't nullify task here, let the result/error handler do it after processing final audio
        }
        // If you want to immediately stop and not wait for final processing:
        // recognitionTask?.cancel()
        // recognitionTask?.finish() // Use finish if you want it to process what it has and then complete
        // stopAudioEngineAndSession() // Ensure cleanup
    }

    // Call this when the service is no longer needed
    fun dispose() {
        stopListening()
        recognitionTask?.cancel()
        recognitionTask = null
        speechRecognizer = null // Release recognizer
    }
}
