package com.trendyol.transmission.components

import android.app.Application
import org.koin.android.ext.koin.androidContext

class ComponentsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ComponentsApplication)
        }
    }
}