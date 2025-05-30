# Contracts

**Contracts** define interfaces for inter-transformer communication, providing type-safe ways for Transformers to interact with each other through computations, executions, data holders, and checkpoints.

## Overview

Contracts enable Transformers to:
- Query data from other Transformers (Computations)
- Execute operations in other Transformers (Executions)
- Access shared data holders (Data Holders)
- Coordinate complex flows (Checkpoints)

## Types of Contracts

```kotlin
sealed interface Contract {
    class Identity
    class DataHolder<T : Transmission.Data?>
    class Computation<T : Any?>
    class ComputationWithArgs<A : Any, T : Any?>
    class Execution
    class ExecutionWithArgs<A : Any>
    class Checkpoint
}
```

## Identity Contracts

Identity contracts provide unique identifiers for Transformers:

```kotlin
class MyTransformer : Transformer(
    identity = Contract.identity()
) {
    // Transformer with unique identity
}

// Or use a shared identity
companion object {
    val transformerIdentity = Contract.identity()
}

class MyTransformer : Transformer(identity = transformerIdentity)
```

## Data Holder Contracts

Data holder contracts enable shared state management:

```kotlin
class StateTransformer : Transformer() {
    
    private val stateHolder = dataHolder(
        initialValue = AppState(),
        contract = appStateContract
    )
    
    companion object {
        val appStateContract = Contract.dataHolder<AppState>()
    }
}

// Other transformers can access this state through computations
class ConsumerTransformer : Transformer() {
    override val computations: Computations = computations {
        register(getCurrentStateContract) {
            // Access the state holder from StateTransformer
            // This requires additional implementation
        }
    }
}
```

## Computation Contracts

Computations allow Transformers to query data from other Transformers:

### Simple Computations

```kotlin
class DataProviderTransformer : Transformer() {
    private var currentData = "Initial Data"
    
    override val computations: Computations = computations {
        register(getCurrentDataContract) {
            currentData
        }
        
        register(getProcessedDataContract) {
            processData(currentData)
        }
    }
    
    companion object {
        val getCurrentDataContract = Contract.computation<String>()
        val getProcessedDataContract = Contract.computation<ProcessedData>()
    }
}

class ConsumerTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<RequestDataSignal> {
            // Query data from DataProviderTransformer
            val data = compute(DataProviderTransformer.getCurrentDataContract)
            val processedData = compute(DataProviderTransformer.getProcessedDataContract)
            
            send(CombinedData(data, processedData))
        }
    }
}
```

### Computations with Arguments

```kotlin
class CalculatorTransformer : Transformer() {
    
    override val computations: Computations = computations {
        register(addNumbersContract) { numbers: List<Int> ->
            numbers.sum()
        }
        
        register(multiplyContract) { operands: MultiplyOperands ->
            operands.a * operands.b
        }
        
        register(formatNumberContract) { number: Double ->
            "%.2f".format(number)
        }
    }
    
    companion object {
        val addNumbersContract = Contract.computationWithArgs<List<Int>, Int>()
        val multiplyContract = Contract.computationWithArgs<MultiplyOperands, Int>()
        val formatNumberContract = Contract.computationWithArgs<Double, String>()
    }
}

data class MultiplyOperands(val a: Int, val b: Int)

class MathTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<CalculateSignal> { signal ->
            // Use computations with arguments
            val sum = compute(CalculatorTransformer.addNumbersContract, listOf(1, 2, 3, 4, 5))
            val product = compute(CalculatorTransformer.multiplyContract, MultiplyOperands(6, 7))
            val formatted = compute(CalculatorTransformer.formatNumberContract, 123.456)
            
            send(CalculationResult("Sum: $sum, Product: $product, Formatted: $formatted"))
        }
    }
}
```

### Cached Computations

```kotlin
class ExpensiveCalculationTransformer : Transformer() {
    
    override val computations: Computations = computations {
        // Result is cached - expensive operation runs only once
        register(expensiveOperationContract) {
            performExpensiveCalculation()
        }
    }
    
    companion object {
        val expensiveOperationContract = Contract.computation<BigDecimal>(useCache = true)
    }
    
    private fun performExpensiveCalculation(): BigDecimal {
        // Simulate expensive operation
        Thread.sleep(5000)
        return BigDecimal("123.456789")
    }
}
```

## Execution Contracts

Executions are fire-and-forget operations for side effects:

```kotlin
class LoggingTransformer : Transformer() {
    
    override val executions: Executions = executions {
        register(logInfoContract) {
            writeToLogFile("INFO", "General information logged")
        }
        
        register(logWithMessageContract) { message: String ->
            writeToLogFile("INFO", message)
        }
        
        register(logWithLevelContract) { logEntry: LogEntry ->
            writeToLogFile(logEntry.level, logEntry.message)
        }
    }
    
    companion object {
        val logInfoContract = Contract.execution()
        val logWithMessageContract = Contract.executionWithArgs<String>()
        val logWithLevelContract = Contract.executionWithArgs<LogEntry>()
    }
}

data class LogEntry(val level: String, val message: String)

class BusinessLogicTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<UserActionSignal> { signal ->
            // Fire-and-forget logging
            execute(LoggingTransformer.logInfoContract)
            execute(LoggingTransformer.logWithMessageContract, "User performed: ${signal.action}")
            execute(LoggingTransformer.logWithLevelContract, LogEntry("INFO", "Action completed"))
            
            // Continue with main business logic
            send(ActionCompletedData(signal.action))
        }
    }
}
```

## Checkpoint Contracts (Experimental)

Checkpoints enable complex flow control and debugging:

```kotlin
@OptIn(ExperimentalTransmissionApi::class)
class FlowControlTransformer : Transformer() {
    
    override val handlers: Handlers = handlers {
        onSignal<StartProcessSignal> { signal ->
            // Pause execution until validation is complete
            val validatedData = pauseOn(validationCheckpoint)
            
            // Continue with validated data
            send(ProcessCompletedData(validatedData))
        }
        
        onEffect<ValidationCompleteEffect> { effect ->
            // Resume paused execution with validated data
            validate(validationCheckpoint, effect.validatedData)
        }
    }
    
    companion object {
        val validationCheckpoint = Contract.checkpointWithArgs<ValidatedData>()
    }
}
```

## Examples from Samples

### Counter Sample

```kotlin
// From samples/counter
val lookUpAndReturn = Contract.computationWithArgs<String, String>()

class Worker(val id: String) : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<CounterSignal.Lookup> {
            // Use computation contract to get data from another transformer
            send(CounterData("Transformer $id updated data to ${compute(lookUpAndReturn, id)}"))
        }
    }
}
```

### Components Sample

```kotlin
// From samples/components/input/InputTransformer
@OptIn(ExperimentalTransmissionApi::class)
class InputTransformer : Transformer() {
    
    private val holder = dataHolder(InputUiState(), holderContract)

    override val computations: Computations = computations {
        register(writtenInputContract) {
            delay(1.seconds)
            WrittenInput(holder.getValue().writtenText)
        }
        register(writtenInputWithArgs) {
            WrittenInput(it)
        }
    }
    
    companion object {
        val writtenInputWithArgs = Contract.computationWithArgs<String, WrittenInput>()
        val writtenInputContract = Contract.computation<WrittenInput>()
        val holderContract = Contract.dataHolder<InputUiState>()
        val colorCheckpoint = Contract.checkpointWithArgs<Color>()
    }
}
```

## Advanced Usage Patterns

### Factory Pattern for Contracts

```kotlin
object ContractFactory {
    fun <T : Any> createCachedComputation(): Contract.Computation<T> {
        return Contract.computation(useCache = true)
    }
    
    fun <A : Any, T : Any> createParameterizedComputation(): Contract.ComputationWithArgs<A, T> {
        return Contract.computationWithArgs(useCache = false)
    }
}

class MyTransformer : Transformer() {
    companion object {
        val cachedDataContract = ContractFactory.createCachedComputation<ExpensiveData>()
        val parameterizedContract = ContractFactory.createParameterizedComputation<String, ProcessedData>()
    }
}
```

### Contract Groups

```kotlin
object UserContracts {
    val getCurrentUser = Contract.computation<User?>()
    val validateUser = Contract.computationWithArgs<User, Boolean>()
    val saveUser = Contract.executionWithArgs<User>()
    val deleteUser = Contract.executionWithArgs<String>()
    val userDataHolder = Contract.dataHolder<UserState>()
}

object AuthContracts {
    val isAuthenticated = Contract.computation<Boolean>()
    val authenticate = Contract.computationWithArgs<Credentials, AuthResult>()
    val logout = Contract.execution()
    val authStateHolder = Contract.dataHolder<AuthState>()
}

class UserTransformer : Transformer() {
    private val userHolder = dataHolder(UserState(), UserContracts.userDataHolder)
    
    override val computations: Computations = computations {
        register(UserContracts.getCurrentUser) {
            userHolder.getValue().currentUser
        }
        
        register(UserContracts.validateUser) { user: User ->
            validateUserData(user)
        }
    }
    
    override val executions: Executions = executions {
        register(UserContracts.saveUser) { user: User ->
            saveUserToDatabase(user)
        }
        
        register(UserContracts.deleteUser) { userId: String ->
            deleteUserFromDatabase(userId)
        }
    }
}
```

### Conditional Contract Registration

```kotlin
class ConfigurableTransformer(private val config: TransformerConfig) : Transformer() {
    
    override val computations: Computations = computations {
        // Always available
        register(basicDataContract) {
            getBasicData()
        }
        
        // Conditionally available based on configuration
        if (config.enableAdvancedFeatures) {
            register(advancedDataContract) {
                getAdvancedData()
            }
        }
        
        if (config.enableCaching) {
            register(cachedDataContract) {
                getCachedData()
            }
        }
    }
    
    companion object {
        val basicDataContract = Contract.computation<BasicData>()
        val advancedDataContract = Contract.computation<AdvancedData>()
        val cachedDataContract = Contract.computation<CachedData>(useCache = true)
    }
}
```

## Error Handling with Contracts

### Safe Computation Calls

```kotlin
class SafeConsumerTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<RequestDataSignal> {
            try {
                val data = compute(DataProviderTransformer.getCurrentDataContract)
                send(DataRetrievedSuccessfully(data))
            } catch (e: Exception) {
                send(ErrorData("Failed to compute data: ${e.message}"))
            }
        }
    }
}
```

### Nullable Return Types

```kotlin
class OptionalDataTransformer : Transformer() {
    override val computations: Computations = computations {
        register(optionalDataContract) {
            // May return null
            getOptionalData()
        }
    }
    
    companion object {
        val optionalDataContract = Contract.computation<OptionalData?>()
    }
}

class ConsumerTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<CheckDataSignal> {
            val data = compute(OptionalDataTransformer.optionalDataContract)
            
            if (data != null) {
                send(DataAvailable(data))
            } else {
                send(NoDataAvailable)
            }
        }
    }
}
```

## Testing with Contracts

Contracts make testing easier by enabling mocking:

```kotlin
@Test
fun `should handle data computation correctly`() = transmissionTest {
    val providerTransformer = DataProviderTransformer()
    val consumerTransformer = ConsumerTransformer()
    
    // Test the computation
    consumerTransformer.test {
        send(RequestDataSignal)
        
        expectData<CombinedData> { data ->
            assertNotNull(data.rawData)
            assertNotNull(data.processedData)
        }
    }
}
```

## Best Practices

### 1. Organize Contracts by Domain

```kotlin
// Good - domain-specific contract objects
object UserContracts {
    val getCurrentUser = Contract.computation<User?>()
    val validateCredentials = Contract.computationWithArgs<Credentials, Boolean>()
}

object OrderContracts {
    val getCurrentOrder = Contract.computation<Order?>()
    val calculateTotal = Contract.computationWithArgs<List<OrderItem>, BigDecimal>()
}

// Avoid - mixed contracts
object AllContracts {
    val userContract = Contract.computation<User?>()
    val orderContract = Contract.computation<Order?>()
    val paymentContract = Contract.computation<Payment?>()
}
```

### 2. Use Descriptive Contract Names

```kotlin
// Good - clear purpose
val getCurrentUserDataContract = Contract.computation<UserData>()
val validateUserInputContract = Contract.computationWithArgs<UserInput, ValidationResult>()
val saveUserToDatabase = Contract.executionWithArgs<User>()

// Avoid - vague names
val userContract = Contract.computation<UserData>()
val checkStuff = Contract.computationWithArgs<Any, Boolean>()
val doThing = Contract.execution()
```

### 3. Group Related Contracts

```kotlin
class AuthTransformer : Transformer() {
    companion object {
        // Authentication contracts
        val isLoggedIn = Contract.computation<Boolean>()
        val getCurrentUser = Contract.computation<User?>()
        val authenticate = Contract.computationWithArgs<Credentials, AuthResult>()
        
        // Session management contracts
        val refreshToken = Contract.execution()
        val invalidateSession = Contract.execution()
        
        // State contracts
        val authStateHolder = Contract.dataHolder<AuthState>()
    }
}
```

### 4. Use Type-Safe Arguments

```kotlin
// Good - specific types
data class SearchParams(val query: String, val filters: List<Filter>)
val searchContract = Contract.computationWithArgs<SearchParams, SearchResult>()

// Avoid - generic types
val searchContract = Contract.computationWithArgs<Map<String, Any>, Any>()
```

### 5. Consider Caching for Expensive Operations

```kotlin
// For expensive computations
val expensiveDataContract = Contract.computation<ExpensiveData>(useCache = true)

// For frequently accessed data
val frequentDataContract = Contract.computation<FrequentData>(useCache = true)

// For simple, fast operations (default)
val simpleDataContract = Contract.computation<SimpleData>()
```