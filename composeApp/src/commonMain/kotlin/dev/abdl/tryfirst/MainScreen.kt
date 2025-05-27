package dev.abdl.tryfirst

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.abdl.tryfirst.service.GeminiService
import dev.abdl.tryfirst.service.SpeechToTextService
import dev.abdl.tryfirst.service.TextToSpeechService
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun MainScreen(viewModel: VoiceViewModel = koinViewModel<VoiceViewModel>()) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Voice Narration App", style = MaterialTheme.typography.headlineSmall)

            LanguageSelector(
                selectedLanguage = viewModel.selectedLanguage,
                onLanguageSelected = { viewModel.onLanguageSelected(it) },
                isEnabled = viewModel.appState == AppState.IDLE || viewModel.appState == AppState.ERROR
            )

            if (viewModel.transcribedText.isNotBlank()) {
                Text("You said:", style = MaterialTheme.typography.titleMedium)
                Text(viewModel.transcribedText, style = MaterialTheme.typography.bodyLarge)
            }

            if (viewModel.processedText.isNotBlank()) {
                Text("Processed:", style = MaterialTheme.typography.titleMedium)
                Text(viewModel.processedText, style = MaterialTheme.typography.bodyLarge)
            }

            if (viewModel.errorMessage != null) {
                Text("Error: ${viewModel.errorMessage}", color = MaterialTheme.colorScheme.error)
            }
        }

        RecordButton(
            appState = viewModel.appState,
            onStartRecording = { viewModel.startListening() },
            onStopRecording = { viewModel.stopListeningAndProcess() }
        )
    }
}

// LanguageSelector and RecordButton Composables remain the same as in the previous version
@Composable
fun LanguageSelector(
    selectedLanguage: InputLanguage,
    onLanguageSelected: (InputLanguage) -> Unit,
    isEnabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = isEnabled) {
            Text("Language: ${selectedLanguage.displayName}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            InputLanguage.values().forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RecordButton(
    appState: AppState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Button(
        onClick = {
            if (appState == AppState.LISTENING) {
                onStopRecording()
            } else {
                onStartRecording()
            }
        },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        enabled = appState == AppState.IDLE || appState == AppState.LISTENING || appState == AppState.ERROR,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (appState == AppState.LISTENING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            when (appState) {
                AppState.IDLE -> "Tap to Speak"
                AppState.LISTENING -> "Listening... (Tap to Stop)"
                AppState.PROCESSING -> "Processing..."
                AppState.SPEAKING -> "Speaking..."
                AppState.ERROR -> "Error (Tap to Retry)"
            }
        )
    }
}
