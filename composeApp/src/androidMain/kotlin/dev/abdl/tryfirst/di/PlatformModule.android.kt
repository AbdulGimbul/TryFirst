package dev.abdl.tryfirst.di

import dev.abdl.tryfirst.service.SpeechToTextService
import dev.abdl.tryfirst.service.TextToSpeechService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import storage.createDataStore

actual val platformModule: Module
    get() = module {
        single<SpeechToTextService> { SpeechToTextService(androidContext()) }
        single<TextToSpeechService> { TextToSpeechService(androidContext()) }
        single { createDataStore(context = androidContext()) }
    }