package com.trendyol.transmission.counter

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val scrollState = rememberLazyListState()
    val itemList = viewModel.transmissionList.collectAsStateWithLifecycle()
    val areAllDistinct = viewModel.areAllDistinct.collectAsStateWithLifecycle(true)
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { viewModel.processSignal(CounterSignal.Lookup) },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text("Send")
            }
            Text("All Distinct: ${areAllDistinct.value} | Total Size: ${itemList.value.size}")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), state = scrollState
        ) {
            items(itemList.value) {
                Text(it, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}