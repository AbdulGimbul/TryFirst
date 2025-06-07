package dev.abdl.tryfirst

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

    val isProcessingOrSpeaking = viewModel.appState == AppState.PROCESSING ||
            viewModel.appState == AppState.SPEAKING
    val isListening = viewModel.appState == AppState.LISTENING
    val isRequestingPermission = viewModel.appState == AppState.REQUESTING_PERMISSION
    val uiEnabled = !isProcessingOrSpeaking && !isListening && !isRequestingPermission

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Try First:\nYour English Speaking Journey!",
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
            )

            InputModeSelector(
                currentMode = viewModel.inputMode,
                onModeSelected = { viewModel.onInputModeChanged(it) },
                isEnabled = uiEnabled
            )

            LanguageSelector(
                selectedLanguage = viewModel.selectedLanguage,
                onLanguageSelected = { viewModel.onLanguageSelected(it) },
                isEnabled = viewModel.appState == AppState.IDLE || viewModel.appState == AppState.ERROR
            )

            if (viewModel.inputMode == InputMode.TEXT) {
                OutlinedTextField(
                    value = viewModel.manualInputText,
                    onValueChange = { viewModel.onManualInputTextChanged(it) },
                    label = { Text("Write here") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiEnabled,
                    singleLine = false,
                    maxLines = 5,
                    minLines = 3
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultViewWithCopy(
                    title = if (viewModel.inputMode == InputMode.SPEAK) "You said:" else "Input Text:",
                    text = viewModel.transcribedText
                )

                if (viewModel.selectedLanguage == InputLanguage.ENGLISH) {
                    ResultViewWithCopy(
                        title = "Refined:",
                        text = viewModel.refinedText
                    )
                    ResultViewWithCopy(
                        title = "Translated:",
                        text = viewModel.translatedToBahasaText
                    )
                } else {
                    ResultViewWithCopy(
                        title = "Translated:",
                        text = viewModel.translatedToEnglishText
                    )
                }
            }

            if (viewModel.errorMessage != null) {
                Text("${viewModel.errorMessage}", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButton(
                appState = viewModel.appState,
                inputMode = viewModel.inputMode,
                isManualInputReady = viewModel.manualInputText.isNotBlank(),
                onProcess = {
                    if (!viewModel.connectivity.isConnected) {
                        showToast(
                            message = "Ups! there's no internet connection",
                            backgroundColor = Color.Red
                        )
                    } else {
                        viewModel.handleProcessAction(permissionsController)
                    }
                },
                onStopListening = { viewModel.stopListeningAndProcess() }
            )

            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        .widthIn(min = 200.dp),
                    onClick = { uriHandler.openUri("https://github.com/abdulgimbul") },
                ) {
                    Text(stringResource(Res.string.open_github))
                }
            }
        }
    }
}

@Composable
fun ResultViewWithCopy(
    title: String,
    text: String?,
    modifier: Modifier = Modifier
) {
    if (text.isNullOrBlank() || text == "-") return

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            if (title != "You said:") {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(text))
                        showToast(message = "'${title.trimEnd(':')}' copied to clipboard")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $title"
                    )
                }
            }
        }
    }
}

@Composable
fun InputModeSelector(
    currentMode: InputMode,
    onModeSelected: (InputMode) -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                )
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputMode.entries.forEach { mode ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (mode == currentMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable(enabled = isEnabled) { onModeSelected(mode) }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.name.capitalize(),
                        color = if (mode == currentMode) Color.White else Color.Gray,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    appState: AppState,
    inputMode: InputMode,
    isManualInputReady: Boolean,
    onProcess: () -> Unit,
    onStopListening: () -> Unit
) {
    val (text: String, buttonEnabled: Boolean, color: Color) = when (inputMode) {
        InputMode.SPEAK -> when (appState) {
            AppState.IDLE -> Triple("Tap to Speak", true, MaterialTheme.colorScheme.primary)
            AppState.REQUESTING_PERMISSION -> Triple(
                "Requesting Permission...",
                false,
                MaterialTheme.colorScheme.primary
            )

            AppState.LISTENING -> Triple(
                "Listening... (Tap to Stop)",
                true,
                MaterialTheme.colorScheme.error
            )

            AppState.PROCESSING -> Triple("Processing...", false, MaterialTheme.colorScheme.primary)
            AppState.SPEAKING -> Triple("Speaking...", false, MaterialTheme.colorScheme.primary)
            AppState.ERROR -> Triple(
                "Error (Tap to Retry)",
                true,
                MaterialTheme.colorScheme.primary
            )
        }

        InputMode.TEXT -> when (appState) {
            AppState.IDLE, AppState.ERROR -> Triple(
                if (isManualInputReady) "Process Text" else "Enter text to process",
                isManualInputReady,
                MaterialTheme.colorScheme.primary
            )

            AppState.PROCESSING -> Triple("Processing...", false, MaterialTheme.colorScheme.primary)
            AppState.SPEAKING -> Triple("Speaking...", false, MaterialTheme.colorScheme.primary)
            AppState.REQUESTING_PERMISSION, AppState.LISTENING -> Triple(
                "Process Text",
                false,
                MaterialTheme.colorScheme.primary
            )
        }
    }

    Button(
        onClick = {
            if (inputMode == InputMode.SPEAK && appState == AppState.LISTENING) {
                onStopListening()
            } else if (buttonEnabled) {
                onProcess()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = buttonEnabled,
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, modifier = Modifier.padding(vertical = 8.dp))
    }
}

private fun String.capitalize(): String {
    return this.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
            Text("Practice with: ${selectedLanguage.displayName}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            InputLanguage.entries.forEach { language ->
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
