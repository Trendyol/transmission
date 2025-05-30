# Transformer Communication

Transformers in the Transmission library can communicate with each other through several mechanisms: Effects, Computations, Executions, and Checkpoints. This enables complex business logic flows while maintaining loose coupling between components.

## Communication Methods

### 1. Effects (Asynchronous Communication)

Effects are the primary way for Transformers to communicate asynchronously.

#### Publishing Effects

```kotlin
class SourceTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<TriggerActionSignal> { signal ->
            // Publish effect to all listening transformers
            publish(DataProcessingEffect(signal.data))
        }
    }
}

class TargetTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onEffect<DataProcessingEffect> { effect ->
            // Handle the effect
            val processedData = processData(effect.data)
            send(ProcessedData(processedData))
        }
    }
}
```

#### Targeted Effects

```kotlin
class SpecificTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<SendToSpecificSignal> { signal ->
            // Send effect to a specific transformer
            publish(
                effect = SpecificEffect(signal.data),
                identity = targetTransformerIdentity
            )
        }
    }
}
```

### 2. Computations (Synchronous Queries)

Computations allow one Transformer to query data from another synchronously.

#### Simple Computations

```kotlin
class DataProviderTransformer : Transformer() {
    private var currentData = "Initial State"
    
    override val computations: Computations = computations {
        register(getCurrentDataContract) {
            currentData
        }
        
        register(getDataStatusContract) {
            DataStatus(
                value = currentData,
                lastUpdated = System.currentTimeMillis(),
                isValid = currentData.isNotEmpty()
            )
        }
    }
    
    override val handlers: Handlers = handlers {
        onSignal<UpdateDataSignal> { signal ->
            currentData = signal.newData
            send(DataUpdated(currentData))
        }
    }
    
    companion object {
        val getCurrentDataContract = Contract.computation<String>()
        val getDataStatusContract = Contract.computation<DataStatus>()
    }
}

class DataConsumerTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<ProcessDataSignal> {
            // Query data from another transformer
            val currentData = compute(DataProviderTransformer.getCurrentDataContract)
            val status = compute(DataProviderTransformer.getDataStatusContract)
            
            if (status.isValid) {
                val result = processData(currentData)
                send(ProcessingResult(result))
            } else {
                send(ProcessingError("Invalid data state"))
            }
        }
    }
}
```

#### Computations with Arguments

```kotlin
class CalculationTransformer : Transformer() {
    override val computations: Computations = computations {
        register(calculateSumContract) { numbers: List<Int> ->
            numbers.sum()
        }
        
        register(formatCurrencyContract) { amount: Double ->
            "%.2f USD".format(amount)
        }
        
        register(validateDataContract) { data: InputData ->
            ValidationResult(
                isValid = data.isNotEmpty() && data.isNumeric(),
                errors = validateInput(data)
            )
        }
    }
    
    companion object {
        val calculateSumContract = Contract.computationWithArgs<List<Int>, Int>()
        val formatCurrencyContract = Contract.computationWithArgs<Double, String>()
        val validateDataContract = Contract.computationWithArgs<InputData, ValidationResult>()
    }
}

class BusinessLogicTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<ProcessOrderSignal> { signal ->
            // Validate input data
            val validation = compute(CalculationTransformer.validateDataContract, signal.orderData)
            
            if (!validation.isValid) {
                send(OrderValidationFailed(validation.errors))
                return@onSignal
            }
            
            // Calculate totals
            val itemPrices = signal.orderData.items.map { it.price }
            val total = compute(CalculationTransformer.calculateSumContract, itemPrices)
            
            // Format for display
            val formattedTotal = compute(CalculationTransformer.formatCurrencyContract, total.toDouble())
            
            send(OrderProcessed(signal.orderData.id, formattedTotal))
        }
    }
}
```

### 3. Executions (Fire-and-Forget Operations)

Executions are used for side effects that don't return values.

```kotlin
class AuditTransformer : Transformer() {
    override val executions: Executions = executions {
        register(logUserActionContract) { action: UserAction ->
            writeAuditLog(action.userId, action.action, action.timestamp)
        }
        
        register(sendNotificationContract) { notification: Notification ->
            sendPushNotification(notification.userId, notification.message)
        }
        
        register(updateAnalyticsContract) { event: AnalyticsEvent ->
            analyticsService.track(event.name, event.properties)
        }
    }
    
    companion object {
        val logUserActionContract = Contract.executionWithArgs<UserAction>()
        val sendNotificationContract = Contract.executionWithArgs<Notification>()
        val updateAnalyticsContract = Contract.executionWithArgs<AnalyticsEvent>()
    }
}

class UserInteractionTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<UserClickedButtonSignal> { signal ->
            // Fire-and-forget operations
            execute(AuditTransformer.logUserActionContract, UserAction(
                userId = signal.userId,
                action = "button_click:${signal.buttonId}",
                timestamp = System.currentTimeMillis()
            ))
            
            execute(AuditTransformer.updateAnalyticsContract, AnalyticsEvent(
                name = "button_click",
                properties = mapOf("button_id" to signal.buttonId)
            ))
            
            // Continue with main business logic
            send(ButtonClickProcessed(signal.buttonId))
        }
    }
}
```

## Complex Communication Patterns

### Chain of Responsibility

```kotlin
class ValidationChainTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<ValidateDataSignal> { signal ->
            // Start validation chain
            publish(ValidateFormatEffect(signal.data))
        }
        
        onEffect<ValidateFormatEffect> { effect ->
            if (isValidFormat(effect.data)) {
                publish(ValidateBusinessRulesEffect(effect.data))
            } else {
                send(ValidationFailed("Invalid format"))
            }
        }
        
        onEffect<ValidateBusinessRulesEffect> { effect ->
            if (passesBusinessRules(effect.data)) {
                publish(ValidateSecurityEffect(effect.data))
            } else {
                send(ValidationFailed("Business rules violation"))
            }
        }
        
        onEffect<ValidateSecurityEffect> { effect ->
            if (isSecure(effect.data)) {
                send(ValidationPassed(effect.data))
            } else {
                send(ValidationFailed("Security check failed"))
            }
        }
    }
}
```

### Observer Pattern

```kotlin
class EventBroadcasterTransformer : Transformer() {
    private val subscribersHolder = dataHolder(
        initialValue = SubscriberState(),
        contract = subscriberContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<SubscribeToEventsSignal> { signal ->
            subscribersHolder.update { state ->
                state.copy(subscribers = state.subscribers + signal.subscriberId)
            }
        }
        
        onSignal<BroadcastEventSignal> { signal ->
            val subscribers = subscribersHolder.getValue().subscribers
            
            subscribers.forEach { subscriberId ->
                publish(EventBroadcastEffect(subscriberId, signal.event))
            }
        }
    }
    
    companion object {
        val subscriberContract = Contract.dataHolder<SubscriberState>()
    }
}

class EventListenerTransformer(private val listenerId: String) : Transformer() {
    override val handlers: Handlers = handlers {
        onEffect<EventBroadcastEffect> { effect ->
            if (effect.subscriberId == listenerId) {
                // Handle the broadcasted event
                handleEvent(effect.event)
            }
        }
    }
    
    private suspend fun CommunicationScope.handleEvent(event: Event) {
        when (event.type) {
            "user_action" -> send(UserActionReceived(event.data))
            "system_update" -> send(SystemUpdateReceived(event.data))
        }
    }
}
```

### State Machine Communication

```kotlin
class OrderStateMachineTransformer : Transformer() {
    private val orderStateHolder = dataHolder(
        initialValue = OrderState.Pending,
        contract = orderStateContract
    )
    
    override val computations: Computations = computations {
        register(getCurrentOrderStateContract) {
            orderStateHolder.getValue()
        }
        
        register(canTransitionToContract) { targetState: OrderState ->
            val currentState = orderStateHolder.getValue()
            isValidTransition(currentState, targetState)
        }
    }
    
    override val handlers: Handlers = handlers {
        onSignal<ProcessOrderSignal> { signal ->
            val currentState = orderStateHolder.getValue()
            
            when (currentState) {
                is OrderState.Pending -> {
                    orderStateHolder.update { OrderState.Processing }
                    publish(StartOrderProcessingEffect(signal.orderId))
                }
                is OrderState.Processing -> {
                    send(OrderAlreadyProcessing(signal.orderId))
                }
                is OrderState.Completed -> {
                    send(OrderAlreadyCompleted(signal.orderId))
                }
            }
        }
        
        onEffect<OrderProcessingCompleteEffect> { effect ->
            orderStateHolder.update { OrderState.Completed }
            publish(OrderCompletedEffect(effect.orderId))
        }
    }
    
    companion object {
        val orderStateContract = Contract.dataHolder<OrderState>()
        val getCurrentOrderStateContract = Contract.computation<OrderState>()
        val canTransitionToContract = Contract.computationWithArgs<OrderState, Boolean>()
    }
}

sealed class OrderState : Transmission.Data {
    object Pending : OrderState()
    object Processing : OrderState()
    object Completed : OrderState()
}
```

## Examples from Samples

### Components Sample Communication

```kotlin
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

    override val handlers: Handlers = handlers {
        onSignal<InputSignal.InputUpdate> { signal ->
            holder.update { it.copy(writtenText = signal.value) }
            
            // Checkpoint-based communication
            val color = pauseOn(colorCheckpoint)
            
            // Send effect to specific transformer
            send(
                effect = ColorPickerEffect.SelectedColorUpdate(color),
                identity = multiOutputTransformerIdentity
            )
            
            // Broadcast effect to all listeners
            publish(effect = InputEffect.InputUpdate(signal.value))
        }
        
        onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            validate(colorCheckpoint, effect.color)
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }
}

class ColorPickerTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<ColorPickerSignal.ColorSelected> { signal ->
            // Respond to color selection
            publish(ColorPickerEffect.BackgroundColorUpdate(signal.color))
        }
        
        onEffect<ColorPickerEffect.SelectedColorUpdate> { effect ->
            // Handle incoming color update from InputTransformer
            processSelectedColor(effect.color)
        }
    }
}
```

## Advanced Communication Patterns

### Request-Response Pattern

```kotlin
class ServiceTransformer : Transformer() {
    private val pendingRequests = mutableMapOf<String, String>()
    
    override val handlers: Handlers = handlers {
        onEffect<ServiceRequestEffect> { effect ->
            val requestId = generateRequestId()
            pendingRequests[requestId] = effect.requesterId
            
            // Process request
            val result = processServiceRequest(effect.request)
            
            // Send response back to requester
            publish(ServiceResponseEffect(
                requestId = requestId,
                response = result,
                targetTransformerId = effect.requesterId
            ))
        }
    }
}

class ClientTransformer(private val transformerId: String) : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<RequestServiceSignal> { signal ->
            // Send request to service
            publish(ServiceRequestEffect(
                request = signal.request,
                requesterId = transformerId
            ))
        }
        
        onEffect<ServiceResponseEffect> { effect ->
            if (effect.targetTransformerId == transformerId) {
                // Handle response
                send(ServiceResponseReceived(effect.response))
            }
        }
    }
}
```

### Pipeline Pattern

```kotlin
class PipelineStage1Transformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<StartPipelineSignal> { signal ->
            val stage1Result = processStage1(signal.input)
            publish(Stage1CompleteEffect(stage1Result))
        }
    }
}

class PipelineStage2Transformer : Transformer() {
    override val handlers: Handlers = handlers {
        onEffect<Stage1CompleteEffect> { effect ->
            val stage2Result = processStage2(effect.data)
            publish(Stage2CompleteEffect(stage2Result))
        }
    }
}

class PipelineStage3Transformer : Transformer() {
    override val handlers: Handlers = handlers {
        onEffect<Stage2CompleteEffect> { effect ->
            val finalResult = processStage3(effect.data)
            send(PipelineCompleted(finalResult))
        }
    }
}
```

### Mediator Pattern

```kotlin
class MediatorTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onEffect<ComponentAEvent> { effect ->
            // Mediate between Component A and Component B
            val transformedData = transformDataForB(effect.data)
            publish(ComponentBCommand(transformedData))
        }
        
        onEffect<ComponentBEvent> { effect ->
            // Mediate between Component B and Component A
            val transformedData = transformDataForA(effect.data)
            publish(ComponentACommand(transformedData))
        }
        
        onEffect<ComponentACommand> { effect ->
            // Forward command to Component A
            publish(ProcessComponentAEffect(effect.data))
        }
        
        onEffect<ComponentBCommand> { effect ->
            // Forward command to Component B  
            publish(ProcessComponentBEffect(effect.data))
        }
    }
}
```

## Error Handling in Communication

### Safe Computation Calls

```kotlin
class SafeCommunicationTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<SafeQuerySignal> {
            try {
                val result = compute(DataProviderTransformer.dataContract)
                send(QuerySuccessful(result))
            } catch (e: Exception) {
                send(QueryFailed("Failed to compute data: ${e.message}"))
                execute(LoggingTransformer.logErrorContract, e.message)
            }
        }
    }
}
```

### Circuit Breaker Pattern

```kotlin
class CircuitBreakerTransformer : Transformer() {
    private val circuitState = dataHolder(
        initialValue = CircuitState.Closed,
        contract = circuitStateContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<CallExternalServiceSignal> { signal ->
            val currentState = circuitState.getValue()
            
            when (currentState) {
                is CircuitState.Closed -> {
                    try {
                        val result = callExternalService(signal.request)
                        send(ServiceCallSuccessful(result))
                    } catch (e: Exception) {
                        circuitState.update { CircuitState.Open(System.currentTimeMillis()) }
                        send(ServiceCallFailed("Service unavailable"))
                    }
                }
                is CircuitState.Open -> {
                    if (System.currentTimeMillis() - currentState.openedAt > CIRCUIT_TIMEOUT) {
                        circuitState.update { CircuitState.HalfOpen }
                        // Retry the call
                        onSignal(signal)
                    } else {
                        send(ServiceCallFailed("Circuit breaker is open"))
                    }
                }
                is CircuitState.HalfOpen -> {
                    try {
                        val result = callExternalService(signal.request)
                        circuitState.update { CircuitState.Closed }
                        send(ServiceCallSuccessful(result))
                    } catch (e: Exception) {
                        circuitState.update { CircuitState.Open(System.currentTimeMillis()) }
                        send(ServiceCallFailed("Service still unavailable"))
                    }
                }
            }
        }
    }
}
```

## Best Practices

### 1. Use Appropriate Communication Methods

```kotlin
// Use Effects for asynchronous notifications
publish(UserLoggedInEffect(user.id))

// Use Computations for data queries
val userData = compute(UserTransformer.getCurrentUserContract)

// Use Executions for fire-and-forget operations
execute(AuditTransformer.logActionContract, action)
```

### 2. Design Clear Interfaces

```kotlin
// Good - clear, specific contracts
object OrderContracts {
    val getCurrentOrder = Contract.computation<Order?>()
    val calculateOrderTotal = Contract.computationWithArgs<OrderItems, BigDecimal>()
    val validateOrder = Contract.computationWithArgs<Order, ValidationResult>()
    val saveOrder = Contract.executionWithArgs<Order>()
}

// Avoid - vague, generic contracts
val dataContract = Contract.computation<Any>()
val processContract = Contract.computationWithArgs<Any, Any>()
```

### 3. Handle Communication Failures

```kotlin
override val handlers: Handlers = handlers {
    onSignal<QueryDataSignal> {
        try {
            val data = compute(DataProviderTransformer.dataContract)
            send(DataQuerySuccessful(data))
        } catch (e: Exception) {
            send(DataQueryFailed("Communication failed: ${e.message}"))
            execute(LoggingTransformer.logErrorContract, "Query failed: ${e.message}")
        }
    }
}
```

### 4. Maintain Loose Coupling

```kotlin
// Good - communicates through contracts
val userData = compute(UserContracts.getCurrentUser)

// Avoid - direct references
val userData = userTransformer.getCurrentUser() // Direct coupling
```

### 5. Document Communication Flows

```kotlin
/**
 * Order Processing Flow:
 * 1. OrderTransformer receives CreateOrderSignal
 * 2. Publishes ValidateOrderEffect 
 * 3. ValidationTransformer validates and publishes ProcessPaymentEffect
 * 4. PaymentTransformer processes payment and publishes SaveOrderEffect
 * 5. OrderTransformer saves order and sends OrderCreatedData
 */
class OrderTransformer : Transformer() {
    // Implementation...
}
```