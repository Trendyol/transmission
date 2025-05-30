# Testing

The Transmission library includes a comprehensive testing framework that makes it easy to test your Transformers, business logic, and communication flows in isolation.

## Testing Dependencies

Add the testing module to your project:

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("com.trendyol:transmission-test:<version>")
}
```

## Imports

For testing, you'll typically need these imports:

```kotlin
import com.trendyol.transmission.test.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
```

## Basic Testing

### Testing a Simple Transformer

```kotlin
@Test
fun `should increment counter when receiving increment signal`() {
    val transformer = CounterTransformer()
    
    transformer.test()
        .testSignal(CounterSignal.Increment) {
            // Verify data output
            val data = lastData<CounterData>()
            assertEquals(1, data?.count)
        }
}
```

### Testing Signal Handlers

```kotlin
@Test
fun `should handle user login correctly`() {
    val authTransformer = AuthTransformer()
    
    authTransformer.test()
        .testSignal(UserLoginSignal(validCredentials)) {
            // Verify successful login data
            val userData = lastData<UserData>()
            assertEquals("John Doe", userData?.user?.name)
            assertTrue(userData?.isAuthenticated == true)
            
            // Verify navigation effect
            val navEffect = lastEffect<NavigationEffect.GoToHome>()
            assertNotNull(navEffect)
            
            // Verify no error data
            assertFalse(hasData<ErrorData>())
        }
}
```

### Testing Effect Handlers

```kotlin
@Test
fun `should process refresh effect correctly`() {
    val dataTransformer = DataTransformer()
    
    dataTransformer.test()
        .testEffect(RefreshDataEffect) {
            // Verify data is refreshed
            val data = lastData<FreshData>()
            assertNotNull(data?.content)
            assertTrue(data?.timestamp?.let { it > 0 } == true)
        }
}
```

## Testing with Mocking

### Mocking Data

```kotlin
@Test
fun `should handle mocked computation data`() {
    val transformer = UserTransformer()
    
    transformer.test()
        .withData(UserTransformer.getCurrentUserContract, mockUserData)
        .testSignal(UpdateUserSignal("New Name", "new@email.com")) {
            val state = lastData<UserState>()
            assertEquals("New Name", state?.user?.name)
            assertEquals("new@email.com", state?.user?.email)
        }
}
```

### Mocking Computations

```kotlin
@Test
fun `should use mocked computation results`() {
    val calculatorTransformer = CalculatorTransformer()
    
    calculatorTransformer.test()
        .withComputation(CalculatorTransformer.getCurrentValueContract, 42)
        .withComputation(CalculatorTransformer.addNumbersContract, 15)
        .testSignal(CalculateSignal) {
            val result = lastData<CalculationResult>()
            assertEquals(42, result?.currentValue)
            assertEquals(15, result?.additionResult)
        }
}
```

### Testing with Checkpoints

```kotlin
@Test
fun `should handle checkpoint-based flow`() {
    val transformer = InputTransformer()
    
    transformer.test()
        .withCheckpoint(InputTransformer.colorCheckpoint, Color.Gray)
        .testSignal(InputSignal.InputUpdate("test")) {
            assertEquals(InputEffect.InputUpdate("test"), lastEffect())
            assertEquals(InputUiState("test"), lastData())
        }
}
```

### Testing with Initial Processing

```kotlin
@Test
fun `should handle initial processing setup`() {
    val transformer = DataTransformer()
    val initialTransmissions = listOf(
        DataSignal.Initialize,
        DataSignal.LoadDefaults
    )
    
    transformer.test()
        .withInitialProcessing(initialTransmissions)
        .testSignal(DataSignal.Process) {
            assertTrue(hasData<InitializedData>())
            assertTrue(hasData<DefaultsData>())
            assertTrue(hasData<ProcessedData>())
        }
}
```

## Testing Complex Flows

### Testing Effect Chains

```kotlin
@Test
fun `should process validation chain correctly`() {
    val validationTransformer = ValidationChainTransformer()
    
    validationTransformer.test()
        .testSignal(ValidateDataSignal(validTestData)) {
            // Should progress through validation stages
            assertTrue(hasEffect<ValidateFormatEffect>())
            assertTrue(hasEffect<ValidateBusinessRulesEffect>())
            assertTrue(hasEffect<ValidateSecurityEffect>())
            
            // Should end with successful validation
            assertTrue(hasData<ValidationPassed>())
            assertFalse(hasData<ValidationFailed>())
        }
}

@Test
fun `should fail validation chain on invalid data`() {
    val validationTransformer = ValidationChainTransformer()
    
    validationTransformer.test()
        .testSignal(ValidateDataSignal(invalidTestData)) {
            // Should fail at format validation
            assertTrue(hasEffect<ValidateFormatEffect>())
            assertFalse(hasEffect<ValidateBusinessRulesEffect>())
            
            // Should send failure data
            val failure = lastData<ValidationFailed>()
            assertEquals("Invalid format", failure?.reason)
        }
}
```

### Testing State Machines

```kotlin
@Test
fun `should transition order states correctly`() {
    val orderStateMachine = OrderStateMachineTransformer()
    
    orderStateMachine.test()
        .testSignal(ProcessOrderSignal("order-123")) {
            // Verify state transitions through multiple data emissions
            val states = allData<OrderState>()
            assertTrue(states.any { it is OrderState.Processing })
            
            // Verify final state
            val finalState = lastData<OrderState>()
            assertTrue(finalState is OrderState.Completed)
            
            // Verify effects
            assertTrue(hasEffect<StartOrderProcessingEffect>())
            assertTrue(hasEffect<OrderCompletedEffect>())
        }
        .testEffect(OrderProcessingCompleteEffect("order-123")) {
            val completedState = lastData<OrderState>()
            assertTrue(completedState is OrderState.Completed)
        }
}
```

## Testing Error Handling

### Testing Exception Handling

```kotlin
@Test
fun `should handle errors gracefully`() {
    val riskyTransformer = RiskyTransformer()
    
    riskyTransformer.test()
        .testSignal(RiskyOperationSignal(invalidData)) {
            // Should produce error data instead of crashing
            val error = lastData<ErrorData>()
            assertTrue(error?.message?.contains("Invalid data") == true)
            
            // Should not produce success data
            assertFalse(hasData<SuccessData>())
        }
}
```

### Testing Fallback Mechanisms

```kotlin
@Test
fun `should fallback when external service fails`() {
    val serviceTransformer = ExternalServiceTransformer()
    
    serviceTransformer.test()
        .withComputation(ExternalServiceTransformer.serviceContract, null) // Mock failure
        .testSignal(CallExternalServiceSignal("test-request")) {
            // Should use fallback
            val fallbackData = lastData<FallbackData>()
            assertEquals("fallback-response", fallbackData?.content)
            
            assertTrue(hasEffect<ServiceFailureLoggedEffect>())
        }
}
```

## Testing with Real Examples

### Counter Sample Test

```kotlin
@Test
fun `worker should process lookup signal correctly`() {
    val worker = Worker("test-worker")
    
    worker.test()
        .testSignal(CounterSignal.Lookup) {
            val data = lastData<CounterData>()
            assertTrue(data?.id?.contains("test-worker") == true)
        }
}
```

### Components Sample Test

```kotlin
@Test
fun `input transformer should handle input updates`() {
    val inputTransformer = InputTransformer(Dispatchers.Unconfined)
    
    inputTransformer.test()
        .testSignal(InputSignal.InputUpdate("Hello World")) {
            // Verify state update
            val state = lastData<InputUiState>()
            assertEquals("Hello World", state?.writtenText)
            
            // Verify effect publication
            val effect = lastEffect<InputEffect.InputUpdate>()
            assertEquals("Hello World", effect?.value)
        }
}

@Test
fun `color picker should handle background color update`() {
    val colorPickerTransformer = ColorPickerTransformer()
    
    colorPickerTransformer.test()
        .testEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
            val state = lastData<ColorPickerUiState>()
            assertEquals(Color.Gray, state?.backgroundColor)
        }
}
```

## Advanced Testing with Assertions

### Using All Data/Effects

```kotlin
@Test
fun `should track all data emissions`() {
    val transformer = MultiDataTransformer()
    
    transformer.test()
        .testSignal(EmitMultipleDataSignal) {
            val allUserData = allData<UserData>()
            assertEquals(3, allUserData.size)
            
            val allEffects = allEffects<NotificationEffect>()
            assertTrue(allEffects.isNotEmpty())
        }
}
```

### Using Find Data with Predicates

```kotlin
@Test
fun `should find specific data with predicate`() {
    val transformer = SearchTransformer()
    
    transformer.test()
        .testSignal(SearchSignal("query")) {
            val specificResult = findData<SearchResult> { 
                it.query == "query" && it.resultCount > 0 
            }
            assertNotNull(specificResult)
            
            val nthResult = nthData<SearchResult>(1) // Second result
            assertNotNull(nthResult)
        }
}
```

## Advanced Testing Patterns

### Testing Inter-Transformer Communication

```kotlin
@Test
fun `should communicate between transformers correctly`() {
    val providerTransformer = DataProviderTransformer()
    val consumerTransformer = DataConsumerTransformer()
    
    // Setup provider with mock data
    providerTransformer.test()
        .withData(DataProviderTransformer.dataContract, mockProviderData)
        .testSignal(UpdateDataSignal("Test Data")) {
            assertTrue(hasData<DataUpdated>())
        }
    
    // Test consumer querying provider
    consumerTransformer.test()
        .withComputation(DataProviderTransformer.dataContract, mockProviderData)
        .testSignal(RequestDataSignal) {
            val data = lastData<RequestedData>()
            assertEquals("Test Data", data?.content)
        }
}
```

### Testing Time-Dependent Operations

```kotlin
@Test
fun `should handle delayed operations correctly`() {
    val delayedTransformer = DelayedTransformer()
    
    delayedTransformer.test()
        .testSignal(StartDelayedOperationSignal) {
            // Verify immediate response
            assertTrue(hasData<OperationStartedData>())
            
            // Verify delayed result appears
            val delayedResult = lastData<DelayedOperationCompletedData>()
            assertNotNull(delayedResult)
        }
}
```

### Testing Multiple Transformer Interactions

```kotlin
@Test
fun `should handle multi-transformer workflow`() {
    val orderTransformer = OrderTransformer()
    val paymentTransformer = PaymentTransformer()
    val inventoryTransformer = InventoryTransformer()
    
    // Mock external dependencies
    orderTransformer.test()
        .withComputation(PaymentTransformer.processPaymentContract, paymentResult)
        .withComputation(InventoryTransformer.reserveItemsContract, inventoryResult)
        .testSignal(CreateOrderSignal(orderDetails)) {
            assertTrue(hasEffect<ValidateInventoryEffect>())
            assertTrue(hasEffect<ProcessPaymentEffect>())
            
            val order = lastData<OrderCreatedData>()
            assertEquals(OrderStatus.CREATED, order?.status)
        }
}
```

## Testing Utilities

### Custom Test Helpers

```kotlin
fun TransmissionTest.sendValidLoginSignal(): TransmissionTest {
    return this.testSignal(UserLoginSignal(Credentials("user", "password"))) {
        // Validation logic can be here or separate
    }
}

fun TransmissionTest.expectSuccessfulLogin() {
    this.testSignal(UserLoginSignal(validCredentials)) {
        val userData = lastData<UserData>()
        assertTrue(userData?.isAuthenticated == true)
        assertTrue(hasEffect<NavigationEffect.GoToHome>())
    }
}

@Test
fun `should login successfully with valid credentials`() {
    val authTransformer = AuthTransformer()
    
    authTransformer.test()
        .expectSuccessfulLogin()
}
```

### Parameterized Tests

```kotlin
@ParameterizedTest
@ValueSource(strings = ["", " ", "invalid-format", "too-long-input"])
fun `should reject invalid inputs`(invalidInput: String) {
    val validationTransformer = ValidationTransformer()
    
    validationTransformer.test()
        .testSignal(ValidateInputSignal(invalidInput)) {
            val result = lastData<ValidationResult>()
            assertFalse(result?.isValid == true)
            assertNotNull(result?.errorMessage)
        }
}
```

### Mock Transformers for Testing

```kotlin
class MockExternalServiceTransformer(
    private val shouldFail: Boolean = false
) : Transformer() {
    
    override val computations: Computations = computations {
        register(ExternalServiceTransformer.getDataContract) {
            if (shouldFail) {
                throw RuntimeException("Service unavailable")
            } else {
                "Mock data"
            }
        }
    }
}

@Test
fun `should handle external service failure`() {
    val businessTransformer = BusinessTransformer()
    
    businessTransformer.test()
        .withComputation(ExternalServiceTransformer.getDataContract, null)
        .testSignal(ProcessDataSignal) {
            val error = lastData<ErrorData>()
            assertTrue(error?.message?.contains("Service unavailable") == true)
        }
}
```

## Best Practices

### 1. Test Business Logic, Not Implementation

```kotlin
// Good - tests behavior
@Test
fun `should calculate order total correctly`() {
    val orderTransformer = OrderTransformer()
    
    orderTransformer.test()
        .testSignal(CalculateOrderSignal(orderWithItems)) {
            val total = lastData<OrderTotal>()
            assertEquals(BigDecimal("150.00"), total?.amount)
        }
}

// Avoid - tests implementation details
@Test
fun `should call specific internal method`() {
    // Testing internal method calls is fragile
}
```

### 2. Use Descriptive Test Names

```kotlin
// Good
@Test
fun `should reject order when inventory is insufficient`()

@Test
fun `should send welcome email after successful registration`()

// Avoid
@Test
fun `test1()`()

@Test
fun `testOrderProcessing()`()
```

### 3. Test Edge Cases

```kotlin
@Test
fun `should handle empty input gracefully`() {
    val processor = DataProcessorTransformer()
    
    processor.test()
        .testSignal(ProcessDataSignal(emptyList())) {
            val result = lastData<ProcessingResult>()
            assertTrue(result?.isEmpty == true)
        }
}

@Test
fun `should handle null values correctly`() {
    val transformer = NullSafeTransformer()
    
    transformer.test()
        .testSignal(ProcessNullableSignal(null)) {
            val result = lastData<SafeProcessingResult>()
            assertEquals("default", result?.value)
        }
}
```

### 4. Keep Tests Isolated

```kotlin
// Good - each test is independent
@Test
fun `should process valid order`() {
    val transformer = OrderTransformer()
    // Test implementation
}

@Test
fun `should reject invalid order`() {
    val transformer = OrderTransformer() // Fresh instance
    // Test implementation
}

// Avoid - shared state between tests
class OrderTransformerTest {
    private val sharedTransformer = OrderTransformer() // Bad: shared state
}
```

### 5. Verify Both Positive and Negative Cases

```kotlin
@Test
fun `should succeed with valid data`() {
    val transformer = DataTransformer()
    
    transformer.test()
        .testSignal(ValidDataSignal(validData)) {
            assertTrue(hasData<SuccessData>())
            assertFalse(hasData<ErrorData>())
        }
}

@Test
fun `should fail with invalid data`() {
    val transformer = DataTransformer()
    
    transformer.test()
        .testSignal(InvalidDataSignal(invalidData)) {
            assertTrue(hasData<ErrorData>())
            assertFalse(hasData<SuccessData>())
        }
}

@Test
fun `should handle edge cases`() {
    val transformer = DataTransformer()
    
    transformer.test()
        .testSignal(EdgeCaseSignal(edgeCaseData)) {
            val result = lastData<ProcessingResult>()
            // Verify boundary condition handling
        }
}
```

## Summary

The Transmission testing framework provides a comprehensive API for testing your transformers:

- **transformer.test()** - Creates a TransmissionTest instance
- **testSignal(signal) { assertions }** - Tests signal handling
- **testEffect(effect) { assertions }** - Tests effect handling
- **withData/withComputation/withCheckpoint** - Mocking and setup
- **lastData<T>(), allData<T>(), hasData<T>()** - Data assertions
- **lastEffect<T>(), allEffects<T>(), hasEffect<T>()** - Effect assertions
- **findData<T>(predicate), nthData(index)** - Advanced data queries

Use these tools to create comprehensive, maintainable tests that verify your business logic while remaining resilient to implementation changes.