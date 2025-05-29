package dev.abdl.tryfirst

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tryfirst.composeapp.generated.resources.Res
import tryfirst.composeapp.generated.resources.open_github

@Composable
fun MainScreen(viewModel: VoiceViewModel = koinViewModel<VoiceViewModel>()) {
    val permissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember { permissionsControllerFactory.createPermissionsController() }
    BindEffect(permissionsController)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Try First:\nYour English Speaking Journey!",
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
            )

            LanguageSelector(
                selectedLanguage = viewModel.selectedLanguage,
                onLanguageSelected = { viewModel.onLanguageSelected(it) },
                isEnabled = viewModel.appState == AppState.IDLE || viewModel.appState == AppState.ERROR
            )

            if (viewModel.transcribedText.isNotBlank()) {
                Text("You said:", style = MaterialTheme.typography.titleMedium)
                Text(viewModel.transcribedText, style = MaterialTheme.typography.bodyLarge)
            }

            if (viewModel.selectedLanguage == InputLanguage.ENGLISH) {
                viewModel.refinedText?.let {
                    Text("Refined (English):", style = MaterialTheme.typography.titleMedium)
                    Text(it.ifBlank { "-" }, style = MaterialTheme.typography.bodyLarge)
                }
                viewModel.translatedToBahasaText?.let {
                    Text(
                        "Translated (to Bahasa Indonesia):",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(it.ifBlank { "-" }, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                viewModel.translatedToEnglishText?.let {
                    Text("Translated (to English):", style = MaterialTheme.typography.titleMedium)
                    Text(it.ifBlank { "-" }, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (viewModel.errorMessage != null) {
                Text("${viewModel.errorMessage}", color = MaterialTheme.colorScheme.error)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecordButton(
                appState = viewModel.appState,
                onStartCycle = {
                    if (!viewModel.connectivity.isConnected) {
                        showToast(
                            message = "Ups! there's no internet connection",
                            backgroundColor = Color.Red
                        )
                    } else {
                        viewModel.startRecognitionCycle(permissionsController)
                    }
                },
                onStopRecording = { viewModel.stopListeningAndProcess() }
            )

            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        .widthIn(min = 200.dp),
                    onClick = { uriHandler.openUri("https://github.com/abdulgimbul") },
                ) {
                    Text(stringResource(Res.string.open_github))
                }
                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        .widthIn(min = 200.dp),
                    onClick = { uriHandler.openUri("https://abdl97.tusk.page") },
                ) {
                    Text("Give me feedback!")
                }
            }
        }
    }
}

@Composable
fun LanguageSelector(
    selectedLanguage: InputLanguage,
    onLanguageSelected: (InputLanguage) -> Unit,
    isEnabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = isEnabled) {
            Text("Speak with: ${selectedLanguage.displayName}")
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
    onStartCycle: () -> Unit,
    onStopRecording: () -> Unit
) {
    Button(
        onClick = {
            if (appState == AppState.LISTENING) {
                onStopRecording()
            } else {
                onStartCycle()
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
                AppState.REQUESTING_PERMISSION -> "Requesting Permission..."
                AppState.LISTENING -> "Listening... (Tap to Stop)"
                AppState.PROCESSING -> "Processing..."
                AppState.SPEAKING -> "Speaking..."
                AppState.ERROR -> "Error (Tap to Retry)"
            }
        )
    }
}
