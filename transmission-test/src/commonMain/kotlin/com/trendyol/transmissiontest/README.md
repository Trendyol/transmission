# Transmission Test Framework

This package provides a streamlined testing framework for the Transmission library. The new `TransmissionTest` class offers an improved API for testing transformers, with a cleaner syntax and more intuitive methods.

## Key improvements over the previous API

1. **More intuitive naming conventions**:
   - `withData` instead of `registerData`
   - `withComputation` instead of `registerComputation`
   - `withInitialProcessing` instead of `processBeforeTesting`
   - `testEffect` and `testSignal` methods for clear separation of test types

2. **Simplified test initialization**:
   - Direct method for creating tests with `Transformer.test()`
   - Optional dispatcher parameter during initialization
   - No need to use separate attachment and initialization methods

3. **Better test result handling**:
   - `TestResult` class that provides direct access to data and effect streams
   - Convenient helper methods for obtaining typed results

4. **More consistent API**:
   - All configuration methods return the test instance for fluent chaining
   - Clear distinction between setup and execution phases
   - No internal/public API confusion

5. **Improved error handling**:
   - Proper exception handling with specific error messages
   - Clear checks for validity of test configurations

## Example usage

Here's a simple example of how to use the new API:

```kotlin
// Initialize a transformer and test it
myTransformer.test()
    // Mock necessary data
    .withData(MyContracts.dataHolder) {
        MyData("test value")
    }
    // Mock computations
    .withComputation(MyContracts.computation) {
        42
    }
    // Test with a signal
    .testSignal(MySignal) {
        // Verify the results
        val lastData = dataStream.filterIsInstance<MyData>().lastOrNull()
        assertEquals("expected value", lastData?.value)
        
        // Verify effects
        val effects = effectStream.filterIsInstance<MyEffect>()
        assertTrue(effects.isNotEmpty())
    }
```

## Migration guide

If you're using the old API, here's how to migrate to the new one:

| Old API | New API |
|---------|---------|
| `transformer.attachToRouter()` | `transformer.test()` |
| `.initialize(transformer)` | Not needed, included in `test()` |
| `.registerData(contract, data)` | `.withData(contract, data)` |
| `.registerComputation(contract, data)` | `.withComputation(contract, data)` |
| `.processBeforeTesting(transmissions)` | `.withInitialProcessing(transmissions)` |
| `.test(effect, scope)` | `.testEffect(effect, assertions)` |
| `.test(signal, scope)` | `.testSignal(signal, assertions)` |

The new API also provides more direct access to test results without requiring the `TransformerTestScope` indirection. 