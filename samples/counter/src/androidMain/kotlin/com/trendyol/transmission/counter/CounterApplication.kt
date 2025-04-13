package com.trendyol.transmission.counter

import android.app.Application
import org.koin.android.ext.koin.androidContext

class CounterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.trendyol.transmission.counter.di.initKoin {
            androidContext(this@CounterApplication)
        }
    }
}