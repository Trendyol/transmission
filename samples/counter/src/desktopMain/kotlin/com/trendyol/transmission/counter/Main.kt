package com.trendyol.transmission.counter

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Counter",
    ) {
        initKoin()
        App()
    }
}