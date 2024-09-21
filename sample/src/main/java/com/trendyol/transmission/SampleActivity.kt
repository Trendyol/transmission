package com.trendyol.transmission

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.trendyol.transmission.components.ComponentScreen
import com.trendyol.transmission.components.ComponentViewModel
import com.trendyol.transmission.counter.CounterViewModel
import com.trendyol.transmission.ui.theme.TransmissionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val componentVM: ComponentViewModel by viewModels()
        val counterVM: CounterViewModel by viewModels()
        enableEdgeToEdge()
        setContent {
            TransmissionTheme {
                ComponentScreen(componentVM)
//                CounterScreen(counterVM)
            }
        }
    }
}

