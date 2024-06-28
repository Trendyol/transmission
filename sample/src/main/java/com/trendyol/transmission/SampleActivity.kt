package com.trendyol.transmission

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trendyol.transmission.features.colorpicker.ColorPickerSignal
import com.trendyol.transmission.features.input.InputSignal
import com.trendyol.transmission.ui.theme.TransmissionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: SampleViewModel by viewModels()
        enableEdgeToEdge()
        setContent {
            TransmissionTheme {
                SampleScreen(viewModel)
            }
        }
    }
}

@Composable
fun SampleScreen(viewModel: SampleViewModel) {
    val inputUiState by viewModel.inputUiState.collectAsStateWithLifecycle()
    val outputUiState by viewModel.outputUiState.collectAsStateWithLifecycle()
    val colorPickerUiState by viewModel.colorPickerUiState.collectAsStateWithLifecycle()
    val multiOutputUiState by viewModel.multiOutputUiState.collectAsStateWithLifecycle()
    val transmissionList by viewModel.transmissionList.collectAsStateWithLifecycle()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InputComponent(
                backgroundColor = inputUiState.backgroundColor,
                onSignal = viewModel::processSignal
            )
            OutputComponent(
                writtenText = outputUiState.outputText,
                backgroundColor = outputUiState.backgroundColor
            )
            ColorPickerComponent(
                backgroundColor = colorPickerUiState.backgroundColor,
                onSignal = viewModel::processSignal,
                selectedColorIndex = colorPickerUiState.selectedColorIndex
            )
            MultiOutputComponent(
                writtenText = multiOutputUiState.writtenUppercaseText,
                selectedColor = multiOutputUiState.selectedColor,
                backgroundColor = multiOutputUiState.backgroundColor
            )
            TransmissionComponent(transmissionList = transmissionList)
        }
    }
}

@Composable
fun TransmissionComponent(
    modifier: Modifier = Modifier,
    transmissionList: List<String>
) {
    val scrollState = rememberLazyListState()
    LaunchedEffect(transmissionList.size) {
        if (transmissionList.lastIndex > 0) {
            scrollState.scrollToItem(transmissionList.lastIndex)
        }
    }
    LazyColumn(modifier = modifier.padding(8.dp), state = scrollState) {
        items(transmissionList) {
            Text(it, fontSize = 10.sp)
        }

    }
}

@Composable
fun InputComponent(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onSignal: (Transmission.Signal) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(color = backgroundColor)
            .border(1.dp, color = Color.Gray)
            .padding(12.dp)
    ) {
        Text(text = "Input Component")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                modifier = Modifier.weight(2.5f),
                shape = RoundedCornerShape(4.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                value = text,
                onValueChange = { text = it },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear text",
                            modifier = Modifier.clickable { text = "" }
                        )
                    }
                }
            )
            Button(
                onClick = {
                    keyboardController?.hide()
                    onSignal.invoke(InputSignal.InputUpdate(text))
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .weight(1f)
            ) {
                Text("Update")
            }
        }
    }
}

@Composable
fun OutputComponent(
    modifier: Modifier = Modifier,
    writtenText: String = "",
    backgroundColor: Color
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(color = backgroundColor)
            .border(1.dp, color = Color.Gray)
            .padding(12.dp)
    ) {
        Text("Output Component")
        val annotatedString = AnnotatedString.Builder().apply {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Written text is: ")
            }
            withStyle(SpanStyle(textDecoration = TextDecoration.None, fontSize = 16.sp)) {
                append(writtenText)
            }
        }.toAnnotatedString()
        Text(annotatedString)
    }
}

@Composable
fun ColorPickerComponent(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    selectedColorIndex: Int,
    onSignal: (Transmission.Signal) -> Unit
) {
    val items =
        listOf(
            Color.White,
            Color.Green,
            Color.DarkGray,
            Color.Yellow,
            Color.Magenta,
            Color.Black,
            Color.Blue
        )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(color = backgroundColor)
            .border(1.dp, color = Color.Gray)
            .padding(12.dp)
    ) {
        Text("Color Picker Component")
        LazyRow(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { index, item ->
                ColorItem(item, selectedColorIndex == index) {
                    onSignal(ColorPickerSignal.SelectColor(index, item))
                }
            }
        }
    }
}


@Composable
fun ColorItem(color: Color, isSelected: Boolean, onClickColor: (Color) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color, CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color.Black else Color.Gray,
                    shape = CircleShape
                )
                .clickable { onClickColor(color) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun MultiOutputComponent(
    modifier: Modifier = Modifier,
    writtenText: String,
    selectedColor: Color,
    backgroundColor: Color
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(color = backgroundColor)
            .border(1.dp, color = Color.Gray)
            .padding(12.dp)
    ) {
        Text("Multi Output Component")
        val annotatedString = AnnotatedString.Builder().apply {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Written Uppercase text is: ")
            }
            withStyle(SpanStyle(textDecoration = TextDecoration.None, fontSize = 16.sp)) {
                append(writtenText)
            }
        }.toAnnotatedString()
        Text(annotatedString)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Selected background is")
            Spacer(modifier = Modifier.width(8.dp))
            ColorItem(color = selectedColor, false, {})
        }
    }
}
