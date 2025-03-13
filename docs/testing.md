# Testing

Testing is an essential part of using the Transmission library. This document covers strategies and tools for testing your Transmission components.

## Testing Utilities

The Transmission library comes with a companion testing module `transmission-test` that provides utilities for testing your Transmission components.

To include it in your project:

```kotlin
testImplementation("com.github.Trendyol:transmission:transmission-test:<latest-version>")
```

## Testing Components

### Testing Transformers

Transformers can be tested in isolation:

```kotlin
class MyTransformerTest {
    private lateinit var transformer: MyTransformer
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        transformer = MyTransformer(dispatcher = testDispatcher)
    }
    
    @Test
    fun `when handling MySignal, should publish MyEffect`() = runTest {
        // Given
        val effectList = mutableListOf<Transmission.Effect>()
        
        // Create a custom scope to collect effects
        val communicationScope = object : CommunicationScope {
            override suspend fun publish(effect: Transmission.Effect) {
                effectList.add(effect)
            }
            // Implement other methods...
        }
        
        // When - trigger the signal handler manually
        transformer.handlerRegistry.signalHandlerRegistry[MySignal::class]?.invoke(
            communicationScope,
            MySignal
        )
        
        // Then
        assertTrue(effectList.any { it is MyEffect })
    }
}
```

### Testing TransmissionRouter

You can test the TransmissionRouter with a set of test Transformers:

```kotlin
class MyRouterTest {
    private lateinit var router: TransmissionRouter
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private val transformer1 = TestTransformer1(testDispatcher)
    private val transformer2 = TestTransformer2(testDispatcher)
    
    @Before
    fun setup() {
        router = TransmissionRouterBuilder.build {
            addTransformerSet(setOf(transformer1, transformer2))
            addDispatcher(testDispatcher)
        }
    }
    
    @Test
    fun `when processing MySignal, both transformers should receive it`() {
        // When
        router.process(MySignal)
        
        // Then
        assertTrue(transformer1.signalList.contains(MySignal))
        assertTrue(transformer2.signalList.contains(MySignal))
    }
}
```

## Testing Contracts

You can test contracts using the `transmission-test` module:

```kotlin
class ContractTest {
    @Test
    fun `test data contract`() = runTest {
        // Given
        val contract = Contracts.dataHolder<MyData>()
        val data = MyData("test")
        val transformer = DataTransformer(contract) { data }
        
        // When
        val result = transformer.requestHelper.getData(contract)
        
        // Then
        assertEquals(data, result)
    }
    
    @Test
    fun `test computation contract`() = runTest {
        // Given
        val contract = Contracts.computation<String>()
        val transformer = ComputationTransformer(contract) { "result" }
        
        // When
        val result = transformer.requestHelper.compute(contract)
        
        // Then
        assertEquals("result", result)
    }
}
```

## Test Doubles

Creating test doubles for your Transmission components can make testing easier:

### Fake Transformers

```kotlin
class FakeTransformer(dispatcher: CoroutineDispatcher) : Transformer(dispatcher) {
    val signalList = mutableListOf<Transmission.Signal>()
    val effectList = mutableListOf<Transmission.Effect>()
    
    override val handlers: Handlers = createHandlers {
        onSignal<MySignal> { signal ->
            signalList.add(signal)
            publish(MyEffect)
        }
        
        onEffect<MyEffect> { effect ->
            effectList.add(effect)
        }
    }
}
```

### Mock Router

```kotlin
class MockRouter : TransmissionRouter(identity = Contracts.identity()) {
    val processedSignals = mutableListOf<Transmission.Signal>()
    val processedEffects = mutableListOf<Transmission.Effect>()
    
    override fun process(signal: Transmission.Signal) {
        processedSignals.add(signal)
    }
    
    override fun process(effect: Transmission.Effect) {
        processedEffects.add(effect)
    }
}
```

## Testing with Turbine

The [Turbine](https://github.com/cashapp/turbine) library is great for testing Flow-based APIs like the ones in Transmission:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class TransmissionRouterTest {
    @Test
    fun `when transformer publishes data, it should be available in dataStream`() = runTest {
        // Given
        val transformer = MyTransformer(UnconfinedTestDispatcher())
        val router = TransmissionRouterBuilder.build {
            addTransformerSet(setOf(transformer))
            addDispatcher(UnconfinedTestDispatcher())
        }
        
        // When
        router.dataStream.test {
            router.process(MySignal)
            
            // Then
            val item = awaitItem()
            assertTrue(item is MyData)
            assertEquals("expected value", (item as MyData).value)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## Integration Testing

For integration testing, you can create a test version of your Transmission setup:

```kotlin
class MyFeatureIntegrationTest {
    private lateinit var router: TransmissionRouter
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        router = TransmissionRouterBuilder.build {
            addTransformerSet(setOf(
                TransformerA(testDispatcher),
                TransformerB(testDispatcher),
                TransformerC(testDispatcher)
            ))
            addDispatcher(testDispatcher)
        }
    }
    
    @Test
    fun `complete feature flow test`() = runTest {
        // Set up data collection
        val dataCollector = mutableListOf<Transmission.Data>()
        val dataCollectionJob = launch {
            router.dataStream.collect { dataCollector.add(it) }
        }
        
        // Trigger initial signal
        router.process(StartFlowSignal)
        
        // Verify expected data was produced
        assertTrue(dataCollector.any { it is FlowCompletedData })
        
        dataCollectionJob.cancel()
    }
}
```

Remember that testing asynchronous code requires careful consideration of timing and order of operations. The Transmission library is designed with testability in mind, making it easier to write reliable tests for your communication logic.