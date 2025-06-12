# How To Use

This guide provides a quick overview of how to use the Transmission library in your Kotlin Multiplatform project.

## Basic Usage Flow

1. **Define your Transmissions** (Signals, Effects, Data)
2. **Create Transformers** to process these transmissions
3. **Set up a TransmissionRouter** to coordinate communication
4. **Connect your UI** to the Transmission network

## 1. Define Your Transmissions

First, define the different types of transmissions your application will use:

```kotlin
// Signal - Input from UI or external events
object IncrementCounterSignal : Transmission.Signal
data class UpdateTextSignal(val text: String) : Transmission.Signal

// Effect - Intermediate processing
data class LoggingEffect(val message: String) : Transmission.Effect
object RefreshUIEffect : Transmission.Effect

// Data - Output for consumption
data class CounterData(val count: Int) : Transmission.Data
data class TextData(val text: String) : Transmission.Data
```

## 2. Create Transformers

Create Transformers to process your transmissions:

```kotlin
class CounterTransformer : Transformer() {
    private var count = 0
    private val counterDataHolder = dataHolder(
        initialValue = CounterData(0),
        contract = counterDataContract
    )

    override val handlers: Handlers = handlers {
        onSignal<IncrementCounterSignal> {
            count++
            publish(LoggingEffect("Counter incremented to $count"))
            counterDataHolder.update { CounterData(count) }
        }

        onEffect<RefreshUIEffect> {
            counterDataHolder.update { CounterData(count) }
        }
    }

    companion object {
        val counterDataContract = Contract.dataHolder<CounterData>()
    }
}

class TextTransformer : Transformer() {
    private val textDataHolder = dataHolder(
        initialValue = TextData(""),
        contract = textDataContract
    )

    override val handlers: Handlers = handlers {
        onSignal<UpdateTextSignal> { signal ->
            textDataHolder.update { TextData(signal.text) }
            publish(LoggingEffect("Text updated to ${signal.text}"))
        }
    }

    companion object {
        val textDataContract = Contract.dataHolder<TextData>()
    }
}

class LoggingTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onEffect<LoggingEffect> { effect ->
            println("Log: ${effect.message}")
        }
    }
}
```

## 3. Set Up TransmissionRouter

Set up a TransmissionRouter to coordinate communication between your Transformers:

```kotlin
val router = TransmissionRouter {
    addTransformerSet(setOf(
        CounterTransformer(),
        TextTransformer(),
        LoggingTransformer()
    ))
}
```

## 4. Connect Your UI

### Option A: Direct Router Usage

Connect your UI directly to the Transmission network:

```kotlin
// Handle UI events
incrementButton.setOnClickListener {
    router.process(IncrementCounterSignal)
}

textInput.addTextChangedListener {
    router.process(UpdateTextSignal(it.toString()))
}

// Observe data changes
lifecycleScope.launch {
    router.streamData<CounterData>()
        .collect { data ->
            countTextView.text = "Count: ${data.count}"
        }
}

lifecycleScope.launch {
    router.streamData<TextData>()
        .collect { data ->
            outputTextView.text = data.text
        }
}
```

### Option B: RouterViewModel (Multiplatform)

For multiplatform applications, use RouterViewModel for cleaner architecture across all platforms:

```kotlin
class MainViewModel : RouterViewModel(
    setOf(
        CounterTransformer(),
        TextTransformer(),
        LoggingTransformer()
    )
) {
    // StateFlow properties for UI
    val counterState = streamDataAsState<CounterData>(CounterData(0))
    val textState = streamDataAsState<TextData>(TextData(""))
    
    // Handle effects
    override fun onEffect(effect: Transmission.Effect) {
        when (effect) {
            is LoggingEffect -> Log.d("MainViewModel", effect.message)
        }
    }
    
    // Public methods for UI interaction
    fun incrementCounter() {
        processSignal(IncrementCounterSignal)
    }
    
    fun updateText(text: String) {
        processSignal(UpdateTextSignal(text))
    }
    
    fun refreshUI() {
        processEffect(RefreshUIEffect)
    }
}

// In your Compose UI
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val counterState by viewModel.counterState.collectAsState()
    val textState by viewModel.textState.collectAsState()
    
    Column {
        Text("Count: ${counterState.count}")
        Button(onClick = { viewModel.incrementCounter() }) {
            Text("Increment")
        }
        
        TextField(
            value = textState.text,
            onValueChange = { viewModel.updateText(it) }
        )
        
        Button(onClick = { viewModel.refreshUI() }) {
            Text("Refresh")
        }
    }
}
```

## 5. Advanced Communication

For more complex scenarios, use Contracts for inter-Transformer communication:

```kotlin
// Define a contract
val counterValueContract = Contract.computation<Int>()

// In CounterTransformer
override val computations: Computations = computations {
    register(counterValueContract) {
        count
    }
}

// In AnotherTransformer
override val handlers: Handlers = handlers {
    onSignal<SomeSignal> {
        val currentCount = compute(counterValueContract)
        // Use the count
        send(SomeData(currentCount))
    }
}
```

## Complete Example

For complete examples, check out the sample modules in the repository:

- **Counter Sample**: Simple increment/decrement counter demonstrating basic concepts
- **Components Sample**: Complex example with multiple transformers and inter-transformer communication

The samples demonstrate practical use cases for the Transmission library in real Kotlin Multiplatform applications.
