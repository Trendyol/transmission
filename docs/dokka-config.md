# Dokka Documentation Configuration

This document describes the Dokka configuration for the Transmission library.

## Configuration Overview

The Transmission project uses Dokka for generating comprehensive API documentation across multiple modules:

- **transmission**: Core library documentation
- **transmission-test**: Testing utilities documentation  
- **transmission-viewmodel**: ViewModel integration documentation

## Generated Documentation Structure

```
build/dokka/htmlMultiModule/
├── index.html                    # Main documentation entry point
├── transmission/                 # Core library docs
├── transmission-test/            # Testing utilities docs
├── transmission-viewmodel/       # ViewModel integration docs
└── navigation.html              # Navigation between modules
```

## Building Documentation

### Generate All Documentation
```bash
./gradlew generateDocs
```

### Generate Individual Module Documentation
```bash
./gradlew :transmission:dokkaHtml
./gradlew :transmission-test:dokkaHtml
./gradlew :transmission-viewmodel:dokkaHtml
```

### Generate Multi-Module Documentation
```bash
./gradlew dokkaHtmlMultiModule
```

## Features Configured

- **Source Links**: Direct links to GitHub source code
- **External Links**: Links to Kotlin and Coroutines documentation
- **Module Descriptions**: Clear descriptions for each module
- **Cross-References**: Inter-module API references
- **Search Functionality**: Built-in search across all modules
- **Responsive Design**: Mobile-friendly documentation

## Customization

The Dokka configuration can be customized in:
- `gradle/build-logic/convention/src/main/kotlin/com/trendyol/transmission/DokkaConvention.kt`
- Individual module `build.gradle.kts` files
- Root `build.gradle.kts` for multi-module settings

## Best Practices

1. **Write comprehensive KDoc comments** for all public APIs
2. **Include code examples** in documentation
3. **Use @param and @return tags** for parameters and return values
4. **Add @since tags** for version information
5. **Include @see references** for related functionality 