# DataHolder

**DataHolder** is a state management component within Transformers that provides thread-safe, reactive state management with automatic data publishing capabilities.

## Overview

DataHolders allow Transformers to maintain internal state that can be:
- Updated safely from multiple coroutines
- Automatically published to the router's data stream
- Queried by other Transformers through contracts
- Tracked for debugging purposes

## Basic Usage

### Creating a DataHolder

```kotlin
class UserTransformer : Transformer() {
    
    // Create a data holder with initial state
    private val userHolder = dataHolder(
        initialValue = UserState(),
        contract = userStateContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<UpdateUserSignal> { signal ->
            // Update the state
            userHolder.update { currentState ->
                currentState.copy(name = signal.newName)
            }
            // Updated state is automatically sent to data stream
        }
    }
    
    companion object {
        val userStateContract = Contract.dataHolder<UserState>()
    }
}
```

### Data Classes for State

```kotlin
data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) : Transmission.Data

data class CounterState(
    val count: Int = 0,
    val step: Int = 1
) : Transmission.Data

data class InputUiState(
    val writtenText: String = "",
    val backgroundColor: Color = Color.White
) : Transmission.Data
```

## DataHolder Interface

```kotlin
interface TransmissionDataHolder<T : Transmission.Data?> {
    fun getValue(): T
    fun update(updater: (T) -> T)
    suspend fun updateAndGet(updater: (T) -> T): T
}
```

### Methods

- **`getValue()`**: Get the current state value
- **`update(updater)`**: Update state synchronously  
- **`updateAndGet(updater)`**: Update state and return the new value

## Creation Options

### Basic DataHolder

```kotlin
// Automatically publishes updates to data stream
val holder = dataHolder(
    initialValue = MyState(),
    contract = myStateContract
)
```

### DataHolder Without Auto-Publishing

```kotlin
// Manual control over when to publish
val holder = dataHolder(
    initialValue = MyState(),
    contract = myStateContract,
    publishUpdates = false
)
```

## Examples from Samples

### Counter Sample

```kotlin
// Simple data holding for counter state
data class CounterData(val id: String) : Transmission.Data

class Worker(val id: String) : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<CounterSignal.Lookup> {
            // Direct data sending without holder
            send(CounterData("Transformer $id updated data to ${compute(lookUpAndReturn, id)}"))
        }
    }
}
```

### Components Sample - Input State

```kotlin
class InputTransformer(
    private val defaultDispatcher: CoroutineDispatcher
) : Transformer(dispatcher = defaultDispatcher) {

    // DataHolder for input UI state
    private val holder = dataHolder(InputUiState(), holderContract)

    override val handlers: Handlers = handlers {
        onSignal<InputSignal.InputUpdate> { signal ->
            // Update the holder state
            holder.update { it.copy(writtenText = signal.value) }
            
            // State is automatically published as InputUiState data
        }
        
        onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            // Update background color
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }

    companion object {
        val holderContract = Contract.dataHolder<InputUiState>()
    }
}
```

## Thread Safety

DataHolders are thread-safe and use internal locking:

```kotlin
class ConcurrentTransformer : Transformer() {
    private val stateHolder = dataHolder(
        initialValue = SharedState(),
        contract = sharedStateContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<ConcurrentUpdateSignal> { signal ->
            // Safe to call from multiple coroutines
            stateHolder.update { state ->
                state.copy(counter = state.counter + 1)
            }
        }
    }
}
```

## Advanced Usage

### Conditional Updates

```kotlin
class ValidationTransformer : Transformer() {
    private val validationHolder = dataHolder(
        initialValue = ValidationState(),
        contract = validationContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<ValidateInputSignal> { signal ->
            val isValid = validateInput(signal.input)
            
            // Only update if validation state changes
            validationHolder.update { currentState ->
                if (currentState.isValid != isValid) {
                    currentState.copy(
                        isValid = isValid,
                        lastValidated = System.currentTimeMillis()
                    )
                } else {
                    currentState // No change
                }
            }
        }
    }
}
```

### Using updateAndGet

```kotlin
class StatefulTransformer : Transformer() {
    private val counterHolder = dataHolder(
        initialValue = CounterState(),
        contract = counterContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<IncrementSignal> { 
            // Get the new value after update
            val newState = counterHolder.updateAndGet { state ->
                state.copy(count = state.count + 1)
            }
            
            // Use the new value for additional logic
            if (newState.count % 10 == 0) {
                publish(MilestoneReachedEffect(newState.count))
            }
        }
    }
}
```

### Manual Publishing Control

```kotlin
class BatchTransformer : Transformer() {
    private val batchHolder = dataHolder(
        initialValue = BatchState(),
        contract = batchContract,
        publishUpdates = false // Manual publishing
    )
    
    override val handlers: Handlers = handlers {
        onSignal<AddToBatchSignal> { signal ->
            batchHolder.update { state ->
                state.copy(items = state.items + signal.item)
            }
            
            // Don't publish until batch is complete
        }
        
        onSignal<CompleteBatchSignal> {
            val finalState = batchHolder.getValue()
            
            // Manually publish the final state
            send(finalState)
            
            // Reset for next batch
            batchHolder.update { BatchState() }
        }
    }
}
```

## Integration with Computations

DataHolders work seamlessly with computations for inter-transformer communication:

```kotlin
class DataProviderTransformer : Transformer() {
    private val dataHolder = dataHolder(
        initialValue = ProviderState(),
        contract = providerContract
    )
    
    override val computations: Computations = computations {
        // Other transformers can query current state
        register(getCurrentDataContract) {
            dataHolder.getValue()
        }
    }
    
    override val handlers: Handlers = handlers {
        onSignal<UpdateDataSignal> { signal ->
            dataHolder.update { state ->
                state.copy(data = signal.newData)
            }
        }
    }
}

class ConsumerTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<ProcessDataSignal> {
            // Query the provider's current state
            val currentData = compute(getCurrentDataContract)
            
            // Process the data
            val result = processData(currentData)
            send(ProcessedDataResult(result))
        }
    }
}
```

## Error Handling

Handle errors in state updates gracefully:

```kotlin
class SafeTransformer : Transformer() {
    private val safeHolder = dataHolder(
        initialValue = SafeState(),
        contract = safeContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<RiskyUpdateSignal> { signal ->
            try {
                val validatedData = validateAndProcess(signal.data)
                
                safeHolder.update { state ->
                    state.copy(
                        data = validatedData,
                        error = null
                    )
                }
            } catch (e: Exception) {
                safeHolder.update { state ->
                    state.copy(
                        error = e.message,
                        lastErrorTime = System.currentTimeMillis()
                    )
                }
            }
        }
    }
}
```

## Complex State Management

### Nested State Updates

```kotlin
data class ComplexState(
    val user: UserInfo,
    val preferences: UserPreferences,
    val cache: Map<String, Any> = emptyMap()
) : Transmission.Data

class ComplexTransformer : Transformer() {
    private val complexHolder = dataHolder(
        initialValue = ComplexState(
            user = UserInfo(),
            preferences = UserPreferences()
        ),
        contract = complexContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<UpdateUserInfoSignal> { signal ->
            complexHolder.update { state ->
                state.copy(
                    user = state.user.copy(
                        name = signal.newName,
                        email = signal.newEmail
                    )
                )
            }
        }
        
        onSignal<UpdatePreferenceSignal> { signal ->
            complexHolder.update { state ->
                state.copy(
                    preferences = state.preferences.copy(
                        theme = signal.newTheme
                    )
                )
            }
        }
        
        onSignal<CacheDataSignal> { signal ->
            complexHolder.update { state ->
                state.copy(
                    cache = state.cache + (signal.key to signal.value)
                )
            }
        }
    }
}
```

## Best Practices

### 1. Use Immutable Data Classes

```kotlin
// Good - immutable data class
data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false
) : Transmission.Data

// Avoid - mutable properties
data class UserState(
    var user: User? = null,
    var isLoading: Boolean = false
) : Transmission.Data
```

### 2. Provide Default Values

```kotlin
// Good - sensible defaults
data class AppState(
    val isInitialized: Boolean = false,
    val currentUser: User? = null,
    val settings: Settings = Settings.default()
) : Transmission.Data
```

### 3. Keep State Focused

```kotlin
// Good - focused state
data class AuthState(
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null
) : Transmission.Data

// Good - separate concerns
data class UIState(
    val isLoading: Boolean = false,
    val error: String? = null
) : Transmission.Data

// Avoid - mixed concerns
data class MixedState(
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val networkStatus: String = "",
    val cacheData: Map<String, Any> = emptyMap()
) : Transmission.Data
```

### 4. Handle Null States Carefully

```kotlin
// Explicit nullable handling
data class OptionalDataState(
    val data: ImportantData? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
) : Transmission.Data

class DataTransformer : Transformer() {
    private val dataHolder = dataHolder(
        initialValue = OptionalDataState(),
        contract = dataContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<LoadDataSignal> {
            dataHolder.update { it.copy(isLoading = true, hasError = false) }
            
            try {
                val data = loadData()
                dataHolder.update { 
                    it.copy(
                        data = data, 
                        isLoading = false, 
                        hasError = false
                    ) 
                }
            } catch (e: Exception) {
                dataHolder.update { 
                    it.copy(
                        isLoading = false, 
                        hasError = true
                    ) 
                }
            }
        }
    }
}
```