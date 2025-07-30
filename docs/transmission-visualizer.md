# Transmission Visualizer


An IntelliJ IDEA/Android Studio plugin for visualizing and navigating [Transmission library](https://trendyol.github.io/transmission/) signal flows in Kotlin Multiplatform projects.

## Overview

Transmission Visualizer enhances your development experience when working with the Transmission library by providing visual cues and navigation tools directly in your IDE. It helps you understand and debug the flow of signals, effects, and data throughout your application's architecture.

## Features

### 🎯 **Line Markers & Navigation**
- **Signal Detection**: Automatically detects `Transmission.Signal` and `Transmission.Effect` implementations
- **Visual Indicators**: Adds gutter icons next to transmission definitions
- **One-Click Navigation**: Click on line markers to visualize signal flow

### 🔍 **Flow Analysis**
- **Complete Flow Visualization**: Shows how signals move through your application
- **Context-Aware Grouping**: Organizes usage by context (Creation, Routing, Processing, Publication, Consumption)
- **Interactive Tree View**: Navigate through signal flows with an intuitive tree interface

### 🧭 **Smart Navigation**
- **Jump to Handlers**: Navigate directly from signals to their corresponding handlers
- **Source Navigation**: Click on any usage to jump directly to the source code
- **Real-time Analysis**: Flow analysis updates as you modify your code

### ⚡ **Performance Optimized**
- **K2 Compiler Support**: Full compatibility with both K1 and K2 Kotlin compiler frontends
- **Async Processing**: Background analysis doesn't block your IDE
- **Test File Filtering**: Excludes test files from analysis for cleaner results

## Usage

### 1. Enable the Tool Window
- Navigate to `View` → `Tool Windows` → `Transmission Visualizer`
- Or use the toolbar to dock the visualizer panel to your preferred location

### 2. Visualize Signal Flows
1. Open a Kotlin file containing Transmission signals or effects
2. Look for the plugin icon (🔌) in the gutter next to your transmission definitions
3. Click the icon to analyze and visualize the signal flow

### 3. Navigate Through Flows
- Use the tree view in the Transmission Visualizer tool window
- Click on any usage to jump directly to that location in your code
- Expand/collapse different contexts to focus on specific parts of the flow

## Example Usage

Given a simple Transmission setup:

```kotlin
// Your Signal Definition
sealed interface CounterSignal : Transmission.Signal {
    data object Increment : CounterSignal    // ← Gutter icon appears here
    data object Decrement : CounterSignal    // ← Gutter icon appears here
}

// Your Transformer
class CounterTransformer : Transformer() {
    override val handlers: Handlers = handlers {
        onSignal<CounterSignal.Increment> {    // ← Shows up in flow analysis
            // Handle increment
        }
        
        onSignal<CounterSignal.Decrement> {    // ← Shows up in flow analysis
            // Handle decrement  
        }
    }
}
```

When you click the gutter icon next to `CounterSignal.Increment`, the visualizer will show:

```
Signal: Increment
├── Creation
│   └── CounterSignal.kt:3 (Increment definition)
├── Processing  
│   └── CounterTransformer.kt:8 (onSignal<Increment> handler)
└── Consumption
    └── MainActivity.kt:45 (router.process(Increment) call)
```

## Supported Transmission Patterns

The visualizer supports all Transmission library patterns:

- ✅ **Signals**: `Transmission.Signal` implementations
- ✅ **Effects**: `Transmission.Effect` implementations
- ✅ **Signal Handlers**: `onSignal<T>` calls in transformers
- ✅ **Effect Handlers**: `onEffect<T>` calls in transformers
- ✅ **Router Processing**: `router.process()` calls
- ✅ **Transformer Communication**: Inter-transformer signal/effect flows

## Requirements

- **IDE**: IntelliJ IDEA 2024.3+ or Android Studio Hedgehog+
- **Kotlin Plugin**: Latest version recommended
- **Transmission Library**: Any version (the plugin detects patterns, not specific versions)

## Configuration

The plugin works out of the box with no configuration required. However, you can customize its behavior:

### Tool Window Position
- Right-click on the "Transmission Visualizer" tab
- Select your preferred docking position (Right, Left, Bottom)

### File Filtering
The plugin automatically excludes test files from analysis. Test files are identified by:
- File names containing "test"
- Files in `/test/`, `/androidTest/`, `/unitTest/` directories
- Files ending with `Test.kt` or `Tests.kt`


## Compatibility

| IDE Version | Plugin Version | Status |
|-------------|----------------|---------|
| IntelliJ IDEA 2024.3+ | 1.0.0+ | ✅ Supported |
| Android Studio Hedgehog+ | 1.0.0+ | ✅ Supported |
| IntelliJ IDEA 2024.2 | 1.0.0+ | ⚠️ Limited Support |

---
