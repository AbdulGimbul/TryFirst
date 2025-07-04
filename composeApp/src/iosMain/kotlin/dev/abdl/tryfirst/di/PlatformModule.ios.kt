package dev.abdl.tryfirst.di

import dev.abdl.tryfirst.service.SpeechToTextService
import dev.abdl.tryfirst.service.TextToSpeechService
import dev.abdl.tryfirst.storage.createDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<SpeechToTextService> { SpeechToTextService() }
        single<TextToSpeechService> { TextToSpeechService() }
        single { createDataStore() }
    }