package com.trendyol.transmission.counter.di

import com.trendyol.transmission.counter.CounterViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { CounterViewModel() }
}

fun initKoin(appDeclaration: KoinAppDeclaration) = startKoin {
    modules(viewModelModule)
    appDeclaration()
}

fun initKoin() = initKoin { }