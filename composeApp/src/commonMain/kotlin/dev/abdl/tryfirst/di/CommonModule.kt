package dev.abdl.tryfirst.di

import dev.abdl.tryfirst.VoiceViewModel
import dev.abdl.tryfirst.service.GeminiService
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val commonModule = module {
    single { GeminiService() }

    factoryOf(::VoiceViewModel)
}