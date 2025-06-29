package dev.abdl.tryfirst.di

import dev.abdl.tryfirst.VoiceViewModel
import dev.abdl.tryfirst.service.GeminiService
import dev.abdl.tryfirst.storage.AppSettings
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val commonModule = module {
    single { GeminiService() }
    single { AppSettings(get()) }
    factoryOf(::VoiceViewModel)
}