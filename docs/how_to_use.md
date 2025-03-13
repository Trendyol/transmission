# How To Use

This guide provides a quick overview of how to use the Transmission library in your Android project.

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
    private val counterDataHolder = dataHolder<CounterData?>(null)
    
    override val handlers: Handlers = createHandlers {
        onSignal<IncrementCounterSignal> {
            count++
            publish(LoggingEffect("Counter incremented to $count"))
            counterDataHolder.update { CounterData(count) }
        }
        
        onEffect<RefreshUIEffect> {
            counterDataHolder.update { CounterData(count) }
        }
    }
}

class TextTransformer : Transformer() {
    private val textDataHolder = dataHolder<TextData?>(null)
    
    override val handlers: Handlers = createHandlers {
        onSignal<UpdateTextSignal> { signal ->
            textDataHolder.update { TextData(signal.text) }
            publish(LoggingEffect("Text updated to ${signal.text}"))
        }
    }
}

class LoggingTransformer : Transformer() {
    override val handlers: Handlers = createHandlers {
        onEffect<LoggingEffect> { effect ->
            Log.d("Transmission", effect.message)
        }
    }
}
```

## 3. Set Up TransmissionRouter

Set up a TransmissionRouter to coordinate communication between your Transformers:

```kotlin
val router = TransmissionRouterBuilder.build {
    addTransformerSet(setOf(
        CounterTransformer(),
        TextTransformer(),
        LoggingTransformer()
    ))
}
```

## 4. Connect Your UI

Connect your UI to the Transmission network:

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
    router.dataStream
        .filterIsInstance<CounterData>()
        .collect { data ->
            countTextView.text = "Count: ${data.count}"
        }
}

lifecycleScope.launch {
    router.dataStream
        .filterIsInstance<TextData>()
        .collect { data ->
            outputTextView.text = data.text
        }
}
```

## 5. Advanced Communication

For more complex scenarios, use Contracts for inter-Transformer communication:

```kotlin
// Define a contract
val counterValueContract = Contracts.computation<Int>()

// In CounterTransformer
override val computations: Computations = createComputations {
    register(counterValueContract) {
        count
    }
}

// In AnotherTransformer
fun doSomething() {
    val currentCount = compute(counterValueContract) ?: 0
    // Use the count
}
```

## Complete Example

For complete examples, check out the sample module in the repository. The samples demonstrate practical use cases for the Transmission library in a real Android application.