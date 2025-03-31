package com.trendyol.transmission.components

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "components",
    ) {
        initKoin()
        App()
    }
}
