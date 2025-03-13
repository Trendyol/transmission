# Transmission

Transmission is an experimental library designed to create a communication network between different business logic blocks for Android projects. It establishes a structured way for these components to communicate asynchronously.

## Core Concepts

Transmission is built around three primary concepts:

- **Transmission**: The unit of information being transferred through the system.
- **Transformer**: Processes transmissions and potentially produces new ones.
- **Router**: Manages the flow of transmissions between transformers.

## Key Features

- **Decoupled Communication**: Enable different parts of your Android application to communicate without direct references.
- **Structured Flow**: Clear flow of information through Signal → Effect → Data channels.
- **Testable Architecture**: Designed with testing in mind, making your business logic more testable.
- **Asynchronous By Design**: Built for asynchronous operations from the ground up.

## Project Structure

The project consists of three main modules:

- **transmission**: The core library implementation
- **transmission-test**: Testing utilities for working with the Transmission library
- **sample**: Sample projects demonstrating real-world usage of the library

