# TransmissionRouter

The **TransmissionRouter** is the central hub that manages communication between all Transformers in your application. It receives Signals from the UI, distributes them to appropriate Transformers, collects their outputs, and provides streams for data consumption.

## Basic Setup

### Simple Router Creation

```kotlin
val router = TransmissionRouter {
    addTransformerSet(setOf(
        UserTransformer(),
        AuthTransformer(),
        DataTransformer()
    ))
}
```

### With Custom Configuration

```kotlin
val router = TransmissionRouter {
    addTransformerSet(transformers)
    addDispatcher(Dispatchers.IO) 
    setCapacity(Capacity.High)
}
```

## Constructor Parameters

```kotlin
class TransmissionRouter internal constructor(
    identity: Contract.Identity,
    transformerSetLoader: TransformerSetLoader? = null,
    autoInitialization: Boolean = true,
    capacity: Capacity = Capacity.Default,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
)
```

- **identity**: Unique identifier for the router
- **transformerSetLoader**: Lazy loading mechanism for transformers
- **autoInitialization**: Whether to initialize automatically (default: true)
- **capacity**: Buffer capacity for internal streams
- **dispatcher**: Coroutine dispatcher for router operations

## Builder Configuration

### Adding Transformers

```kotlin
// Direct transformer set
val router = TransmissionRouter {
    addTransformerSet(setOf(
        CounterTransformer(),
        LoggingTransformer()
    ))
}

// Using a loader for lazy initialization
val router = TransmissionRouter {
    addLoader(MyTransformerSetLoader())
}
```

### Setting Capacity

Capacity affects the buffer size of internal streams:

```kotlin
val router = TransmissionRouter {
    addTransformerSet(transformers)
    setCapacity(Capacity.High) // Higher capacity = better performance under load
}

// Available capacity options
enum class Capacity(val value: Int) {
    Low(64),
    Default(256), 
    High(1024),
    Unlimited(Channel.UNLIMITED)
}
```

### Custom Dispatcher

```kotlin
val router = TransmissionRouter {
    addTransformerSet(transformers)
    addDispatcher(Dispatchers.IO) // Use IO dispatcher for network/database operations
}
```

### Manual Initialization

```kotlin
val router = TransmissionRouter {
    overrideInitialization() // Disable auto-initialization
}

// Later, initialize manually
router.initialize(myTransformerSetLoader)
```

## Processing Transmissions

### Processing Signals

```kotlin
// Process signals from UI
router.process(UserLoginSignal(credentials))
router.process(RefreshDataSignal)
router.process(CounterSignal.Increment)
```

### Processing Effects

```kotlin
// Process effects (typically from other transformers)
router.process(LoggingEffect("Manual log entry"))
router.process(NotificationEffect("Custom notification"))
```

## Data Streams

### Observing All Data

```kotlin
// Collect all data from all transformers
lifecycleScope.launch {
    router.streamData().collect { data ->
        when (data) {
            is UserData -> updateUserUI(data)
            is CounterData -> updateCounterUI(data)
            is ErrorData -> showError(data)
        }
    }
}
```

### Filtering Specific Data Types

```kotlin
// Observe only user data
lifecycleScope.launch {
    router.streamData<UserData>()
        .collect { userData ->
            updateUserProfile(userData)
        }
}

// Observe multiple specific types
lifecycleScope.launch {
    router.streamData()
        .filter { it is UserData || it is ProfileData }
        .collect { data ->
            when (data) {
                is UserData -> handleUserData(data)
                is ProfileData -> handleProfileData(data) 
            }
        }
}
```

## Effect Streams

Access to the effect stream for monitoring:

```kotlin
lifecycleScope.launch {
    router.streamEffect<LoggingEffect>()
        .collect { effect ->
            println("Log: ${effect.message}")
        }
}
```

## Query Helper

The router provides a query helper for debugging and monitoring:

```kotlin
val queryHelper: QueryHandler = router.queryHelper

// Use for debugging transformer states
// (Specific usage depends on your debugging needs)
```

## Complete Examples

### Counter App from Sample

```kotlin
// From samples/counter
class CounterModule {
    fun provideTransmissionRouter(): TransmissionRouter {
        return TransmissionRouter {
            addTransformerSet(setOf(
                Worker("1"),
                Worker("2"),
                Worker("3")
            ))
        }
    }
}

// Usage in UI
class CounterViewModel(private val router: TransmissionRouter) {
    
    val counterData = router.streamData<CounterData>()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CounterData("Initial")
        )
    
    fun lookup() {
        router.process(CounterSignal.Lookup)
    }
}
```

### Components App from Sample

```kotlin
// From samples/components  
class ComponentsModule {
    fun provideTransmissionRouter(): TransmissionRouter {
        return TransmissionRouter {
            addTransformerSet(setOf(
                InputTransformer(defaultDispatcher),
                ColorPickerTransformer(defaultDispatcher),
                OutputTransformer(defaultDispatcher),
                MultiOutputTransformer()
            ))
            addDispatcher(defaultDispatcher)
        }
    }
}

// Usage with multiple data types
class ComponentViewModel(private val router: TransmissionRouter) {
    
    val uiState = combine(
        router.streamData<InputUiState>(),
        router.streamData<OutputCalculationResult>()
    ) { inputState, outputResult ->
        SampleScreenUiState(
            inputState = inputState,
            outputResult = outputResult
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SampleScreenUiState()
    )
    
    fun updateInput(value: String) {
        router.process(InputSignal.InputUpdate(value))
    }
    
    fun selectColor(color: Color) {
        router.process(ColorPickerSignal.ColorSelected(color))
    }
}
```

## Advanced Features

### Custom Transformer Set Loader

For complex applications, you might want to load transformers dynamically:

```kotlin
class CustomTransformerSetLoader : TransformerSetLoader {
    override suspend fun load(): Set<Transformer> {
        return buildSet {
            // Load based on configuration
            if (isFeatureEnabled("auth")) {
                add(AuthTransformer())
            }
            if (isFeatureEnabled("analytics")) {
                add(AnalyticsTransformer()) 
            }
            // Always include core transformers
            add(CoreTransformer())
        }
    }
}

val router = TransmissionRouter {
    addLoader(CustomTransformerSetLoader())
}
```

### Multiple Routers

For large applications, you might use multiple routers:

```kotlin
// Feature-specific routers
val authRouter = TransmissionRouter {
    addTransformerSet(authTransformers)
}

val dataRouter = TransmissionRouter {
    addTransformerSet(dataTransformers)
}

// Cross-router communication via effects
authRouter.streamEffect<UserLoggedInEffect>()
    .collect { effect ->
        dataRouter.process(LoadUserDataSignal(effect.userId))
    }
```

## Lifecycle Management

### Proper Cleanup

```kotlin
class MyRepository {
    private val router = TransmissionRouter { 
        addTransformerSet(transformers)
    }
    
    fun cleanup() {
        router.clear() // Cleans up all transformers and coroutines
    }
}

// In Android ViewModel
class MyViewModel : ViewModel() {
    private val router = TransmissionRouter { /* config */ }
    
    override fun onCleared() {
        router.clear()
        super.onCleared()
    }
}
```

### Integration with Dependency Injection

#### Hilt Example

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TransmissionModule {
    
    @Provides
    @Singleton
    fun provideTransformers(): Set<Transformer> = setOf(
        UserTransformer(),
        AuthTransformer(),
        DataTransformer()
    )
    
    @Provides
    @Singleton
    fun provideTransmissionRouter(
        transformers: Set<Transformer>
    ): TransmissionRouter = TransmissionRouter {
        addTransformerSet(transformers)
        setCapacity(Capacity.High)
    }
}
```

#### Koin Example

```kotlin
val transmissionModule = module {
    single { UserTransformer() }
    single { AuthTransformer() }
    single { DataTransformer() }
    
    single<Set<Transformer>> { 
        setOf(get<UserTransformer>(), get<AuthTransformer>(), get<DataTransformer>()) 
    }
    
    single { 
        TransmissionRouter {
            addTransformerSet(get<Set<Transformer>>())
            setCapacity(Capacity.High)
        }
    }
}
```

## Error Handling

The router includes built-in error handling:

```kotlin
// Transformer errors are isolated and don't crash the router
// Individual transformers can override onError() to handle their own errors

class SafeTransformer : Transformer() {
    override fun onError(throwable: Throwable) {
        // Handle transformer-specific errors
        publish(ErrorEffect("Transformer error: ${throwable.message}"))
    }
}
```

## Performance Considerations

### Capacity Settings

```kotlin
// For high-throughput applications
val router = TransmissionRouter {
    addTransformerSet(transformers)
    setCapacity(Capacity.High) // or Capacity.Unlimited for extreme cases
}

// For memory-constrained environments
val router = TransmissionRouter {
    addTransformerSet(transformers) 
    setCapacity(Capacity.Low)
}
```

### Dispatcher Selection

```kotlin
// For CPU-intensive work
val router = TransmissionRouter {
    addTransformerSet(transformers)
    addDispatcher(Dispatchers.Default)
}

// For IO-intensive work
val router = TransmissionRouter {
    addTransformerSet(transformers)
    addDispatcher(Dispatchers.IO)
}

// For UI-related work (use sparingly)
val router = TransmissionRouter {
    addTransformerSet(transformers)
    addDispatcher(Dispatchers.Main.immediate)
}
```

## Best Practices

1. **Single Router per Feature**: Use one router per logical feature or module
2. **Proper Cleanup**: Always call `router.clear()` when done
3. **Appropriate Capacity**: Choose capacity based on your app's throughput needs
4. **Error Handling**: Implement proper error handling in transformers
5. **Stream Management**: Use appropriate lifecycle scopes for data stream collection
6. **Testing**: The router is designed to be easily testable - see [Testing](testing.md) guide