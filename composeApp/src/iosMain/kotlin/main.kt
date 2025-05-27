import androidx.compose.ui.window.ComposeUIViewController
import dev.abdl.tryfirst.App
import dev.abdl.tryfirst.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}
