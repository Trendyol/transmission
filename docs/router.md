# TransmissionRouter

The **TransmissionRouter** is the central hub that manages communication between all Transformers in your application. It receives Signals from the UI, distributes them to appropriate Transformers, collects their outputs, and provides streams for data consumption.

For Android development, consider using **RouterViewModel** which provides a simplified, lifecycle-aware wrapper around TransmissionRouter.

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

# RouterViewModel

**RouterViewModel** is a convenience wrapper around TransmissionRouter that extends `androidx.lifecycle.ViewModel`. It's fully Kotlin Multiplatform compatible and provides automatic setup, lifecycle management, and simplified stream handling across all platforms.

## Key Features

- **Automatic Setup**: Router is configured and initialized automatically
- **Lifecycle Management**: Router cleanup is handled in `onCleared()`
- **Stream Collection**: Data and effect streams are automatically collected in `viewModelScope`
- **Hook Methods**: Override `onData`, `onEffect`, `onProcessSignal`, and `onProcessEffect` for custom logic
- **Query Support**: Built-in access to `QueryHandler` for querying transformer states
- **StateFlow Helpers**: Built-in methods for converting streams to StateFlow
- **Error Handling**: Built-in error handling with `onError` callback

## Basic Usage

### Simple Implementation

```kotlin
class UserViewModel : RouterViewModel(
    setOf(
        UserTransformer(),
        AuthTransformer(),
        ProfileTransformer()
    )
) {
    // Create StateFlow from data streams
    val userState = streamDataAsState<UserData>(UserData.Empty)
    val authState = streamDataAsState<AuthData>(AuthData.LoggedOut)
    
    // Handle data updates
    override fun onData(data: Transmission.Data) {
        when (data) {
            is UserData -> logUserUpdate(data)
            is AuthData -> handleAuthChange(data)
        }
    }
    
    // Handle effects
    override fun onEffect(effect: Transmission.Effect) {
        when (effect) {
            is NavigationEffect -> navigateTo(effect.destination)
            is ErrorEffect -> showError(effect.message)
        }
    }
    
    // User actions
    fun login(credentials: Credentials) {
        processSignal(AuthSignal.Login(credentials))
    }
    
    fun updateProfile(profile: Profile) {
        processSignal(UserSignal.UpdateProfile(profile))
    }
}
```

### Advanced Configuration

```kotlin
class FeatureViewModel : RouterViewModel(
    loader = FeatureTransformerSetLoader(),
    config = RouterViewModelConfig(
        capacity = Capacity.High,
        dispatcher = Dispatchers.IO,
        identity = Contract.identity("feature-router")
    )
) {
    // Type-safe stream access
    val errorStream = streamEffect<ErrorEffect>()
    val loadingData = streamData<LoadingData>()
    
    // Custom error handling
    override fun onError(throwable: Throwable) {
        logError("Router error", throwable)
        processEffect(ErrorEffect("System error occurred"))
    }
    
    // Post-processing hooks
    override fun onProcessSignal(signal: Transmission.Signal) {
        analytics.trackSignal(signal::class.simpleName)
    }
    
    // Query transformer states
    suspend fun getCurrentUserData(): UserData? {
        return queryHandler.getData(UserTransformer.dataContract)
    }
}
```

## Configuration Options

### RouterViewModelConfig

```kotlin
data class RouterViewModelConfig(
    val capacity: Capacity = Capacity.Default,           // Buffer capacity
    val dispatcher: CoroutineDispatcher = Dispatchers.Default, // Coroutine dispatcher
    val identity: Contract.Identity = Contract.identity()       // Router identity
)
```

### Usage with Configuration

```kotlin
// High-performance configuration
val config = RouterViewModelConfig(
    capacity = Capacity.High,
    dispatcher = Dispatchers.IO
)

class MyViewModel : RouterViewModel(transformers, config) {
    // Implementation
}
```

## Stream Helpers

### StateFlow Creation

```kotlin
class ShoppingViewModel : RouterViewModel(shoppingTransformers) {
    // Automatic StateFlow conversion
    val cartState = streamDataAsState<CartData>(CartData.Empty)
    val productsState = streamDataAsState<ProductListData>(ProductListData.Loading)
    val checkoutState = streamDataAsState<CheckoutData>(CheckoutData.Idle)
    
    // Custom sharing behavior
    val userPreferences = streamDataAsState<UserPreferencesData>(
        initialValue = UserPreferencesData.Default,
        started = SharingStarted.Eagerly
    )
}
```

### Type-Safe Stream Access

```kotlin
class ChatViewModel : RouterViewModel(chatTransformers) {
    // Access specific data types
    val messageFlow = streamData<MessageData>()
    val userStatusFlow = streamData<UserStatusData>()
    
    // Access specific effect types  
    val notificationEffects = streamEffect<NotificationEffect>()
    val navigationEffects = streamEffect<NavigationEffect>()
    
    init {
        // Custom stream handling
        viewModelScope.launch {
            navigationEffects.collect { effect ->
                handleNavigation(effect)
            }
        }
    }
}
```

## Dependency Injection Integration

### Hilt Example

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val transformerSetLoader: MainTransformerSetLoader
) : RouterViewModel(
    loader = transformerSetLoader,
    config = RouterViewModelConfig(capacity = Capacity.High)
) {
    val mainState = streamDataAsState<MainScreenData>(MainScreenData.Loading)
    
    fun refreshData() {
        processSignal(RefreshSignal)
    }
}
```

### Koin Example

```kotlin
val viewModelModule = module {
    viewModel { 
        UserViewModel(
            transformerSet = get<Set<Transformer>>(),
            config = RouterViewModelConfig(
                dispatcher = get(named("ioDispatcher"))
            )
        )
    }
}

class UserViewModel(
    transformerSet: Set<Transformer>,
    config: RouterViewModelConfig
) : RouterViewModel(transformerSet, config) {
    // Implementation
}
```

## Error Handling

```kotlin
class RobustViewModel : RouterViewModel(transformers) {
    override fun onError(throwable: Throwable) {
        when (throwable) {
            is NetworkException -> handleNetworkError(throwable)
            is ValidationException -> handleValidationError(throwable)
            else -> {
                logError("Unexpected router error", throwable)
                processEffect(ErrorEffect("Something went wrong"))
            }
        }
    }
    
    override fun onProcessSignal(signal: Transmission.Signal) {
        // Log all processed signals for debugging
        if (BuildConfig.DEBUG) {
            Log.d("RouterViewModel", "Processed signal: $signal")
        }
    }
}
```

## Multiplatform Support

RouterViewModel is built using `androidx.lifecycle.ViewModel` which is fully multiplatform:

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
```

This means you can use RouterViewModel across all supported platforms:
- **Android**: Native AndroidX ViewModel support
- **iOS**: Full lifecycle management via androidx.lifecycle-viewmodel
- **Desktop**: JVM-based lifecycle management  
- **Web**: Kotlin/JS lifecycle management

### Platform-Specific Considerations

While RouterViewModel works on all platforms, you may want different configurations per platform:

```kotlin
// Platform-specific configurations
expect fun defaultRouterConfig(): RouterViewModelConfig

// androidMain
actual fun defaultRouterConfig() = RouterViewModelConfig(
    capacity = Capacity.High,
    dispatcher = Dispatchers.Main.immediate
)

// iosMain  
actual fun defaultRouterConfig() = RouterViewModelConfig(
    capacity = Capacity.Default,
    dispatcher = Dispatchers.Main
)

// Usage
class MyViewModel : RouterViewModel(
    transformers = myTransformers,
    config = defaultRouterConfig()
)

## Comparison: Direct Router vs RouterViewModel

### Direct TransmissionRouter

```kotlin
class ManualViewModel : ViewModel() {
    private val router = TransmissionRouter {
        addTransformerSet(transformers)
    }
    
    val dataState = router.streamData<MyData>()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), MyData.Empty)
    
    init {
        viewModelScope.launch {
            router.streamEffect<MyEffect>().collect { effect ->
                handleEffect(effect)
            }
        }
    }
    
    fun processUserAction(action: MySignal) {
        router.process(action)
    }
    
    override fun onCleared() {
        router.clear()
        super.onCleared()
    }
}
```

### RouterViewModel

```kotlin
class SimpleViewModel : RouterViewModel(transformers) {
    val dataState = streamDataAsState<MyData>(MyData.Empty)
    
    override fun onEffect(effect: Transmission.Effect) {
        if (effect is MyEffect) {
            handleEffect(effect)
        }
    }
    
    fun processUserAction(action: MySignal) {
        processSignal(action)
    }
    
    // Cleanup is automatic!
}
```

## Best Practices for RouterViewModel

### 1. Use StateFlow for UI State

```kotlin
class GoodViewModel : RouterViewModel(transformers) {
    // ✅ Good: Direct StateFlow creation
    val uiState = streamDataAsState<UiState>(UiState.Loading)
    
    // ❌ Avoid: Manual StateFlow conversion in ViewModels
    // val uiState = streamData<UiState>()
    //     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), UiState.Loading)
}
```

### 2. Handle Effects Properly

```kotlin
class EffectHandlingViewModel : RouterViewModel(transformers) {
    override fun onEffect(effect: Transmission.Effect) {
        when (effect) {
            is NavigationEffect -> {
                // Handle navigation - don't process signals here
                navigationManager.navigateTo(effect.destination)
            }
            is ValidationEffect -> {
                // Handle validation feedback
                showValidationError(effect.message)
            }
        }
    }
}
```

### 3. Use Configuration for Performance

```kotlin
// ✅ Good: Configure based on use case
class HighThroughputViewModel : RouterViewModel(
    transformers = heavyTransformers,
    config = RouterViewModelConfig(
        capacity = Capacity.High,
        dispatcher = Dispatchers.Default
    )
)

class IOIntensiveViewModel : RouterViewModel(
    transformers = networkTransformers,
    config = RouterViewModelConfig(
        dispatcher = Dispatchers.IO
    )
)
```

## Best Practices

1. **Single Router per Feature**: Use one router per logical feature or module
2. **Proper Cleanup**: Always call `router.clear()` when done
3. **Appropriate Capacity**: Choose capacity based on your app's throughput needs
4. **Error Handling**: Implement proper error handling in transformers
5. **Stream Management**: Use appropriate lifecycle scopes for data stream collection
6. **Testing**: The router is designed to be easily testable - see [Testing](testing.md) guide