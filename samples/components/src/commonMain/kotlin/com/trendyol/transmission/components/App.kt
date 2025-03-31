package com.trendyol.transmission.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val viewModel: ComponentViewModel = koinInject()
    ComponentScreen(viewModel)
}