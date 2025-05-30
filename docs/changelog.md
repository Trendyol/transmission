# Changelog

All notable changes to the Transmission library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive documentation with practical examples
- Complete API documentation for all core concepts
- Step-by-step setup guide for Kotlin Multiplatform projects
- Real-world examples using counter and components samples

### Changed
- Updated setup documentation to reflect Maven Central publication from version 1.6.0+
- Improved testing documentation with correct TransmissionTest API
- Enhanced documentation structure and navigation

## [1.6.0] - 2025-05-16

### Added
- **Kotlin Multiplatform Support**: Complete migration to Kotlin Multiplatform
- iOS target support with native compilation
- Desktop (JVM) target support
- **Maven Central Publication**: Now published to Maven Central instead of JitPack
- New publication workflow with signing and verification
- Convention plugins for build configuration management

### Changed
- **Breaking**: Migrated from Android-only to Kotlin Multiplatform
- **Breaking**: Publication moved from JitPack to Maven Central
- **Breaking**: Dependency coordinates changed to `com.trendyol:transmission`
- Refactored counter sample to Compose Multiplatform
- Updated build logic with new convention plugins
- Improved project structure for multiplatform development

### Removed
- Legacy Android-only sample applications
- JitPack publication support for new versions

### Migration Guide
- Update dependencies from `com.github.Trendyol:transmission` to `com.trendyol:transmission`
- Remove JitPack repository if only using Transmission
- For KMP projects, configure targets as needed (android, iosX64, iosArm64, iosSimulatorArm64, jvmDesktop)

## [1.5.0] - 2025-03-24

### Added
- **One-shot Payload Sending**: New API for sending payloads without persistent subscriptions
- Enhanced transmission processing capabilities
- New experimental annotations for evolving APIs

### Changed
- Improved payload handling performance
- Updated version numbering system

## [1.4.2] - 2025-03-14

### Added
- **Enhanced Testing Infrastructure**: New `TransmissionTest` class replacing deprecated `TestSuite`
- New test methods for checkpoint validation
- Improved test assertion methods with better type safety
- Additional transformer testing capabilities

### Changed
- **Breaking**: Deprecated `TestSuite` in favor of `TransmissionTest`
- Updated testing API with fluent builder pattern
- Improved test method naming and organization
- Enhanced sample test implementations

### Fixed
- Test indentation and formatting issues
- OutputTransformer test compatibility
- Broadcast functionality in testing scenarios

## [1.4.0] - 2025-02-25

### Added
- **Transformer Lifecycle**: `onCleared()` callback for cleanup operations
- **Handler Extension API**: Ability to extend and override handler behavior
- **Stacked Lambda System**: Advanced handler composition capabilities
- **Value Class Optimization**: Converted `Capacity` to value class for better performance
- **Stream Extensions**: New extension functions for data and effect streaming

### Changed
- **Breaking**: Refactored handler extension API
- **Breaking**: Updated `UpdateHandlerScope` implementation
- Improved router internals for better performance
- Enhanced API naming conventions for handlers
- Updated README with new features

### Fixed
- Removed redundant coroutine creation
- Fixed lambda versioning issues
- Improved memory management in handler chains

## [1.3.0] - 2024-11-14

### Added
- **Checkpoint API (Experimental)**: Advanced flow control with checkpoints
- `@ExperimentalTransmissionApi` and `@InternalTransmissionApi` annotations
- CheckpointTracker for managing checkpoint state
- Checkpoint validation and frequency control
- Enhanced Contract system with checkpoint support
- Identity-based computation validation
- UUID-based identifier generation

### Changed
- **Breaking**: Contract types converted to classes with internal constructors
- **Breaking**: Updated dataHolder API to remove manual key assignment
- Improved stability and thread safety
- Updated Kotlin to 2.0.20
- Enhanced Counter sample with better examples

### Fixed
- Removed buffer-related issues in broadcast system
- Improved mutex locking in dataHolder operations
- Fixed tryEmit/trySend calls replaced with suspending alternatives

## [1.2.1] - Previous Release

### Added
- Core transmission framework
- Basic transformer functionality
- Router implementation
- Initial testing support

### Changed
- API stabilization improvements
- Performance optimizations

## [1.0.x Series] - Legacy Releases

Multiple releases focusing on:
- Core functionality development
- API refinements
- Bug fixes and stability improvements
- Initial testing framework

## [0.0.x Series] - Early Development

Early development releases with:
- Initial concept implementation
- Proof of concept features
- Experimental APIs

---

## Breaking Changes Summary

### 1.6.0
- **Publication**: Moved from JitPack to Maven Central
- **Dependencies**: Changed from `com.github.Trendyol:transmission` to `com.trendyol:transmission`
- **Platform**: Migrated to Kotlin Multiplatform (supports Android, iOS, Desktop)

### 1.4.2
- **Testing**: `TestSuite` deprecated in favor of `TransmissionTest`

### 1.4.0
- **Handlers**: Handler extension API redesigned
- **Capacity**: Converted to value class (source compatible)

### 1.3.0
- **Contracts**: Contract types changed from interfaces to classes
- **DataHolder**: Manual key assignment removed

## Experimental Features

Some features are marked as experimental and may evolve:

- **Checkpoint System** (`@ExperimentalTransmissionApi`): Advanced flow control
- **Handler Extensions**: Dynamic handler modification
- **Advanced Transformer Communication**: Complex inter-transformer patterns

## Migration Support

For detailed migration guides between major versions, see:
- [Setup Guide](setup.md) for dependency updates
- [Testing Guide](testing.md) for test migration
- [Transformer Guide](transformer.md) for API changes

## Contributing

We welcome contributions! See our [GitHub repository](https://github.com/Trendyol/transmission) for:
- Bug reports and feature requests
- Pull request guidelines
- Development setup instructions

---

*This changelog reflects the actual development history. For the latest releases, check the [GitHub releases page](https://github.com/Trendyol/transmission/releases).*