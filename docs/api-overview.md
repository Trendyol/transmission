# Transmission Library API Overview

Transmission is a Kotlin Multiplatform library that provides a powerful router-based architecture for managing data flow and transformations in your applications.

## Core Modules

### transmission
The core library containing the main routing and transformation infrastructure:
- **Router**: Central routing mechanism for managing data flow
- **Transformer**: Interface for data transformation and processing
- **Effects**: Side effects management system
- **Data Holders**: State management utilities

### transmission-test
Testing utilities and framework for Transmission-based applications:
- **TransmissionTest**: Main testing framework class
- **Test Extensions**: Utility functions for testing transformers
- **Mock Utilities**: Helper classes for creating test doubles

### transmission-viewmodel
Integration with ViewModel pattern for UI applications:
- **RouterViewModel**: ViewModel implementation with Transmission router integration
- **State Management**: Reactive state handling for UI components

## Key Concepts

### Router Architecture
The router acts as the central hub for your application's data flow, managing transformers and their interactions.

### Transformers
Transformers are the core processing units that handle specific data transformations and business logic.

### Effects
Effects provide a clean way to handle side effects while maintaining the unidirectional data flow.

## Getting Started

1. Add the core transmission dependency
2. Define your transformers
3. Configure your router
4. Set up your data flow

For detailed usage examples and API documentation, see the individual module documentation. 