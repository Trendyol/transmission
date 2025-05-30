# Handlers

**Handlers** define how Transformers respond to incoming Signals and Effects. They contain the core business logic of your application and determine what happens when specific transmissions are received.

## Overview

Handlers are defined within Transformers using the `handlers` DSL:

```kotlin
class MyTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<MySignal> { signal ->
            // Handle signal
        }
        
        onEffect<MyEffect> { effect ->
            // Handle effect
        }
    }
}
```

## Signal Handlers

Signal handlers respond to incoming Signals from the UI or external sources.

### Basic Signal Handler

```kotlin
override val handlers: Handlers = handlers {
    onSignal<UserLoginSignal> { signal ->
        // Access signal properties
        val credentials = signal.credentials
        
        // Perform business logic
        val result = authenticateUser(credentials)
        
        // Send data or publish effects
        if (result.isSuccess) {
            send(UserData(result.user))
            publish(NavigationEffect.GoToHome)
        } else {
            send(ErrorData("Login failed"))
        }
    }
}
```

### Multiple Signal Handlers

```kotlin
override val handlers: Handlers = handlers {
    onSignal<IncrementSignal> {
        count++
        send(CounterData(count))
    }
    
    onSignal<DecrementSignal> {
        count--  
        send(CounterData(count))
    }
    
    onSignal<ResetSignal> {
        count = 0
        send(CounterData(count))
    }
}
```

## Effect Handlers

Effect handlers respond to Effects, which can come from other Transformers or the same Transformer.

### Basic Effect Handler

```kotlin
override val handlers: Handlers = handlers {
    onEffect<RefreshDataEffect> { effect ->
        // Handle refresh request
        val freshData = fetchDataFromServer()
        send(FreshDataLoaded(freshData))
    }
    
    onEffect<LoggingEffect> { effect ->
        // Handle logging
        logger.log(effect.level, effect.message)
    }
}
```

### Chaining Effects

```kotlin
override val handlers: Handlers = handlers {
    onSignal<ProcessDataSignal> { signal ->
        // Start processing chain
        publish(ValidateDataEffect(signal.data))
    }
    
    onEffect<ValidateDataEffect> { effect ->
        val isValid = validateData(effect.data)
        if (isValid) {
            publish(SaveDataEffect(effect.data))
        } else {
            send(ValidationErrorData("Invalid data"))
        }
    }
    
    onEffect<SaveDataEffect> { effect ->
        saveData(effect.data)
        send(DataSavedSuccessfully(effect.data))
    }
}
```

## Communication Scope

Within handlers, you have access to `CommunicationScope` which provides several operations:

### Sending Data

Data is sent to the router's data stream for UI consumption:

```kotlin
onSignal<LoadUserSignal> { signal ->
    val user = loadUser(signal.userId)
    send(UserData(user)) // Available to UI via router.dataStream
}
```

### Publishing Effects

Effects are published to other Transformers:

```kotlin
onSignal<UserActionSignal> { signal ->
    // Publish to any transformer listening for this effect
    publish(LoggingEffect("User performed: ${signal.action}"))
    
    // Publish to specific transformer
    publish(
        effect = NotificationEffect("Action completed"),
        identity = notificationTransformerIdentity
    )
}
```

### Inter-Transformer Communication

#### Computing Values

```kotlin
onSignal<CalculateSignal> { signal ->
    // Get value from another transformer
    val currentData = compute(dataContract)
    
    // Compute with arguments
    val result = compute(calculationContract, signal.input)
    
    send(CalculationResult(result))
}
```

#### Executing Operations

```kotlin
onSignal<UserActionSignal> { signal ->
    // Fire-and-forget operation
    execute(logActionContract)
    
    // Execute with arguments
    execute(auditContract, AuditEntry(signal.action, System.currentTimeMillis()))
    
    send(ActionCompletedData())
}
```

## Complete Examples

### Counter Sample Handler

```kotlin
class Worker(val id: String) : Transformer() {
    
    override val handlers: Handlers = handlers {
        onSignal<CounterSignal.Lookup> {
            // Compute value from another transformer and send data
            send(CounterData("Transformer $id updated data to ${compute(lookUpAndReturn, id)}"))
        }
    }
}
```

### Input Transformer from Components Sample

```kotlin
class InputTransformer(
    private val defaultDispatcher: CoroutineDispatcher
) : Transformer(dispatcher = defaultDispatcher) {

    private val holder = dataHolder(InputUiState(), holderContract)

    @OptIn(ExperimentalTransmissionApi::class)
    override val handlers: Handlers = handlers {
        onSignal<InputSignal.InputUpdate> { signal ->
            // Update local state
            holder.update { it.copy(writtenText = signal.value) }
            
            // Checkpoint-based communication (experimental)
            val color = pauseOn(colorCheckpoint)
            
            // Send effect to specific transformer
            send(
                effect = ColorPickerEffect.SelectedColorUpdate(color),
                identity = multiOutputTransformerIdentity
            )
            
            // Publish effect to all listeners
            publish(effect = InputEffect.InputUpdate(signal.value))
        }
        
        onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            // Validate checkpoint
            validate(colorCheckpoint, effect.color)
            
            // Update state based on effect
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }
}
```

### Complex Business Logic Handler

```kotlin
class OrderTransformer : Transformer() {
    private val orderHolder = dataHolder(OrderState(), orderContract)
    
    override val handlers: Handlers = handlers {
        onSignal<CreateOrderSignal> { signal ->
            // Start order creation process
            orderHolder.update { it.copy(isProcessing = true) }
            
            // Validate order
            publish(ValidateOrderEffect(signal.orderDetails))
        }
        
        onEffect<ValidateOrderEffect> { effect ->
            try {
                val validationResult = validateOrder(effect.orderDetails)
                
                if (validationResult.isValid) {
                    publish(ProcessPaymentEffect(effect.orderDetails))
                } else {
                    orderHolder.update { 
                        it.copy(
                            isProcessing = false,
                            error = validationResult.errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                orderHolder.update { 
                    it.copy(
                        isProcessing = false,
                        error = "Validation failed: ${e.message}"
                    )
                }
            }
        }
        
        onEffect<ProcessPaymentEffect> { effect ->
            try {
                val paymentResult = processPayment(effect.orderDetails.payment)
                
                if (paymentResult.isSuccess) {
                    publish(CreateOrderRecordEffect(effect.orderDetails, paymentResult.transactionId))
                } else {
                    orderHolder.update { 
                        it.copy(
                            isProcessing = false,
                            error = "Payment failed: ${paymentResult.error}"
                        )
                    }
                }
            } catch (e: Exception) {
                orderHolder.update { 
                    it.copy(
                        isProcessing = false,
                        error = "Payment processing error: ${e.message}"
                    )
                }
            }
        }
        
        onEffect<CreateOrderRecordEffect> { effect ->
            try {
                val order = createOrderRecord(effect.orderDetails, effect.transactionId)
                
                orderHolder.update { 
                    it.copy(
                        isProcessing = false,
                        completedOrder = order,
                        error = null
                    )
                }
                
                // Notify other systems
                publish(OrderCreatedEffect(order))
                execute(sendConfirmationEmailContract, order.customerEmail)
                
            } catch (e: Exception) {
                orderHolder.update { 
                    it.copy(
                        isProcessing = false,
                        error = "Order creation failed: ${e.message}"
                    )
                }
            }
        }
        
        onEffect<OrderCreatedEffect> { effect ->
            // Update analytics
            execute(trackOrderContract, AnalyticsEvent.OrderCreated(effect.order.id))
        }
    }
}
```

## Error Handling in Handlers

### Try-Catch in Handlers

```kotlin
override val handlers: Handlers = handlers {
    onSignal<RiskyOperationSignal> { signal ->
        try {
            val result = performRiskyOperation(signal.data)
            send(OperationSuccessData(result))
        } catch (e: NetworkException) {
            send(NetworkErrorData("Network error: ${e.message}"))
        } catch (e: ValidationException) {
            send(ValidationErrorData("Invalid input: ${e.message}"))
        } catch (e: Exception) {
            send(GenericErrorData("Unexpected error: ${e.message}"))
        }
    }
}
```

### Using Result Types

```kotlin
override val handlers: Handlers = handlers {
    onSignal<LoadDataSignal> { signal ->
        when (val result = loadDataSafely(signal.id)) {
            is Result.Success -> {
                send(DataLoadedSuccessfully(result.data))
            }
            is Result.Failure -> {
                send(DataLoadingFailed(result.error))
                publish(LoggingEffect("Data loading failed: ${result.error}"))
            }
        }
    }
}
```

## Advanced Handler Patterns

### State Machine Pattern

```kotlin
class StateMachineTransformer : Transformer() {
    private val stateHolder = dataHolder(MachineState.Idle, stateContract)
    
    override val handlers: Handlers = handlers {
        onSignal<StartProcessSignal> { signal ->
            val currentState = stateHolder.getValue()
            
            when (currentState) {
                is MachineState.Idle -> {
                    stateHolder.update { MachineState.Processing(signal.data) }
                    publish(BeginProcessingEffect(signal.data))
                }
                is MachineState.Processing -> {
                    send(ErrorData("Process already running"))
                }
                is MachineState.Completed -> {
                    stateHolder.update { MachineState.Processing(signal.data) }
                    publish(BeginProcessingEffect(signal.data))
                }
            }
        }
        
        onEffect<ProcessCompletedEffect> { effect ->
            stateHolder.update { MachineState.Completed(effect.result) }
        }
    }
}

sealed class MachineState : Transmission.Data {
    object Idle : MachineState()
    data class Processing(val data: Any) : MachineState()
    data class Completed(val result: Any) : MachineState()
}
```

### Command Pattern

```kotlin
class CommandTransformer : Transformer() {
    
    override val handlers: Handlers = handlers {
        onSignal<ExecuteCommandSignal> { signal ->
            when (val command = signal.command) {
                is Command.Save -> handleSaveCommand(command)
                is Command.Load -> handleLoadCommand(command)
                is Command.Delete -> handleDeleteCommand(command)
            }
        }
    }
    
    private suspend fun CommunicationScope.handleSaveCommand(command: Command.Save) {
        // Save logic
        saveData(command.data)
        send(CommandExecutedData("Save completed"))
    }
    
    private suspend fun CommunicationScope.handleLoadCommand(command: Command.Load) {
        // Load logic
        val data = loadData(command.id)
        send(DataLoadedData(data))
    }
    
    private suspend fun CommunicationScope.handleDeleteCommand(command: Command.Delete) {
        // Delete logic
        deleteData(command.id)
        send(CommandExecutedData("Delete completed"))
    }
}

sealed class Command {
    data class Save(val data: Any) : Command()
    data class Load(val id: String) : Command()
    data class Delete(val id: String) : Command()
}
```

### Observer Pattern

```kotlin
class ObserverTransformer : Transformer() {
    private val observersHolder = dataHolder(
        initialValue = ObserverState(),
        contract = observerContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<RegisterObserverSignal> { signal ->
            observersHolder.update { state ->
                state.copy(observers = state.observers + signal.observer)
            }
        }
        
        onSignal<NotifyObserversSignal> { signal ->
            val observers = observersHolder.getValue().observers
            
            observers.forEach { observer ->
                publish(NotifyObserverEffect(observer, signal.event))
            }
        }
        
        onEffect<NotifyObserverEffect> { effect ->
            // Notify specific observer
            notifyObserver(effect.observer, effect.event)
        }
    }
}
```

## Testing Handlers

Handlers are easily testable using the transmission-test module:

```kotlin
@Test
fun `should handle login signal correctly`() = transmissionTest {
    val transformer = AuthTransformer()
    
    // Send signal
    transformer.test {
        send(UserLoginSignal(validCredentials))
        
        // Verify data output
        expectData<UserData> { userData ->
            assertEquals("John Doe", userData.user.name)
        }
        
        // Verify effect output  
        expectEffect<NavigationEffect.GoToHome>()
    }
}
```

## Best Practices

### 1. Keep Handlers Focused

```kotlin
// Good - single responsibility
onSignal<ValidateInputSignal> { signal ->
    val isValid = validateInput(signal.input)
    send(ValidationResult(isValid))
}

// Avoid - multiple responsibilities
onSignal<ProcessEverythingSignal> { signal ->
    validateInput(signal.input)
    saveToDatabase(signal.data)
    sendEmail(signal.email)
    updateUI(signal.uiData)
    logAction(signal.action)
}
```

### 2. Use Descriptive Signal/Effect Names

```kotlin
// Good - clear intent
onSignal<UserRequestsPasswordResetSignal> { /* ... */ }
onEffect<PasswordResetEmailSentEffect> { /* ... */ }

// Avoid - vague names
onSignal<UserSignal> { /* ... */ }
onEffect<SomeEffect> { /* ... */ }
```

### 3. Handle Errors Gracefully

```kotlin
onSignal<LoadDataSignal> { signal ->
    try {
        val data = loadData(signal.id)
        send(DataLoadedSuccessfully(data))
    } catch (e: Exception) {
        send(ErrorData("Failed to load data: ${e.message}"))
        execute(logErrorContract, e)
    }
}
```

### 4. Use Immutable Operations

```kotlin
// Good - immutable updates
onSignal<UpdateUserSignal> { signal ->
    userHolder.update { currentUser ->
        currentUser.copy(name = signal.newName)
    }
}

// Avoid - mutable operations
onSignal<UpdateUserSignal> { signal ->
    val user = userHolder.getValue()
    user.name = signal.newName // Mutation!
    userHolder.update { user }
}
```

### 5. Chain Related Operations

```kotlin
// Good - clear flow
onSignal<CreateAccountSignal> { signal ->
    publish(ValidateAccountDataEffect(signal.accountData))
}

onEffect<ValidateAccountDataEffect> { effect ->
    if (isValid(effect.accountData)) {
        publish(CreateAccountRecordEffect(effect.accountData))
    } else {
        send(ValidationErrorData("Invalid account data"))
    }
}

onEffect<CreateAccountRecordEffect> { effect ->
    val account = createAccount(effect.accountData)
    send(AccountCreatedData(account))
    publish(SendWelcomeEmailEffect(account.email))
}
```