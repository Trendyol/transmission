# Transmission

🚀 **Experimental asynchronous communication for Business Logic**

[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg)](http://kotlinlang.org)
[![Build Status](https://github.com/trendyol/transmission/workflows/Build/badge.svg)](https://github.com/trendyol/transmission/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Transmission is an experimental asynchronous communication library for Kotlin Multiplatform projects, designed to create a structured communication network between different business logic components. It provides a clean, testable architecture that enables different parts of your application to communicate without direct references.

## Quick Start

```kotlin
// Define your data transformation
class UserTransformer : Transformer<UserData, UserResult> {
    override suspend fun transform(data: UserData): UserResult {
        return UserResult(processedData = data.process())
    }
}

// Set up the transmission router
val router = TransmissionRouter.builder()
    .add<UserTransformer>()
    .build()

// Use it in your application
val result = router.transform<UserData, UserResult>(userData)
```

## Documentation

- **[Getting Started](how_to_use.md)** - Learn the basics and start using Transmission
- **[Setup Guide](setup.md)** - Installation and configuration instructions
- **[Transformers](transformer.md)** - Core data transformation concepts
- **[Router System](router.md)** - Advanced routing and communication features
- **[Testing](testing.md)** - Testing strategies and utilities

## API Reference

📚 **[API Documentation](api/0.x/)** - Complete API reference with detailed documentation for all classes and methods.

## Core Concepts

- **[Transmissions](transmissions.md)**: Data flow objects that carry information through your application
- **[Transformer](transformer.md)**: Components that process transmissions and handle business logic
- **[TransmissionRouter](router.md)**: Manages the flow of transmissions between transformers

## Architecture Overview

Transmission follows a structured communication pattern where components communicate through well-defined channels without direct dependencies, making your code more testable and maintainable.

## Key Features

- **Decoupled Communication**: Components communicate without direct references
- **Structured Flow**: Clear information flow through Signal → Effect → Data channels
- **Testable Architecture**: Built with testing in mind for better business logic testing
- **Asynchronous By Design**: Built for asynchronous operations from the ground up
- **Kotlin Multiplatform**: Works across all Kotlin targets

## Community & Support

- 🐛 [Report Issues](https://github.com/trendyol/transmission/issues)
- 💡 [Feature Requests](https://github.com/trendyol/transmission/discussions)
- 📖 [Documentation](https://trendyol.github.io/transmission/)

---

*Made with ❤️ by the Trendyol team*