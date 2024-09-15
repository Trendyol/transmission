package com.trendyol.transmission.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val scrollState = rememberLazyListState()
    val itemList = viewModel.transmissionList.collectAsStateWithLifecycle()
    val areAllDistinct by viewModel.areAllDistinct.collectAsStateWithLifecycle()
    val itemListSize by remember { derivedStateOf { itemList.value.size } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { viewModel.processSignal(CounterSignal.Lookup) },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text("Send")
            }
            Text("Distinct: $areAllDistinct | Total Size: $itemListSize")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), state = scrollState
        ) {
            items(itemList.value) {
                Text(
                    it.first, fontSize = 10.sp, modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (it.second) Color.Red.copy(alpha = 0.2f) else Color.Transparent
                        )
                )
            }
        }
    }
}