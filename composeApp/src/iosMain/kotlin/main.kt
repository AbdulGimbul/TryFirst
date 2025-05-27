import androidx.compose.ui.window.ComposeUIViewController
import dev.abdl.tryfirst.App
import dev.abdl.tryfirst.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {

//     val speechToTextService = IosSpeechToTextService()
//     val textToSpeechService = IosTextToSpeechService()
//     val geminiService = GeminiService(apiKey = GEMINI_API_KEY) // Securely provide API key
//     val viewModel = rememberVoiceViewModel(speechToTextService, textToSpeechService, geminiService)
    App()

}
