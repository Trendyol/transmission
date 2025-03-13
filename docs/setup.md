# Setting Up

This guide provides instructions for adding the Transmission library to your Android project.

## Gradle Setup

### Step 1: Add JitPack Repository

Add the JitPack repository to your root build.gradle file:

```groovy
// In your root build.gradle file
allprojects {
    repositories {
        // Other repositories
        maven { url 'https://jitpack.io' }
    }
}
```

Or in your settings.gradle.kts file if using the new Gradle setup:

```kotlin
// In your settings.gradle.kts file
dependencyResolutionManagement {
    repositories {
        // Other repositories
        maven("https://jitpack.io")
    }
}
```

### Step 2: Add the Dependency

Add the dependency to your app's build.gradle file:

```groovy
// In your app's build.gradle file
dependencies {
    implementation 'com.github.Trendyol:transmission:<latest-version>'
    
    // If you need testing utilities
    testImplementation 'com.github.Trendyol:transmission:transmission-test:<latest-version>'
}
```

Or in Kotlin DSL:

```kotlin
// In your app's build.gradle.kts file
dependencies {
    implementation("com.github.Trendyol:transmission:<latest-version>")
    
    // If you need testing utilities
    testImplementation("com.github.Trendyol:transmission:transmission-test:<latest-version>")
}
```

Replace `<latest-version>` with the latest version from [JitPack](https://jitpack.io/#Trendyol/transmission).

## Project Structure

For a well-organized project using Transmission, consider the following structure:

```
app/
├── src/main/java/com/yourpackage/
│   ├── ui/           # UI Components
│   ├── data/         # Data models
│   ├── transmission/ # Transmission components
│   │   ├── signals/  # Signal definitions
│   │   ├── effects/  # Effect definitions
│   │   ├── data/     # Data definitions
│   │   ├── transformers/ # Transformer implementations
│   │   └── contracts/ # Contract definitions
│   └── di/           # Dependency injection
├── ...
```

## Dependency Injection Setup

If you're using a dependency injection framework like Hilt or Koin, you can create a module for your Transmission components:

### Hilt Example

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TransmissionModule {
    
    @Provides
    @Singleton
    fun provideTransformers(): Set<Transformer> = setOf(
        MyTransformer1(),
        MyTransformer2(),
        MyTransformer3()
    )
    
    @Provides
    @Singleton
    fun provideTransmissionRouter(
        transformers: Set<Transformer>
    ): TransmissionRouter = TransmissionRouterBuilder.build {
        addTransformerSet(transformers)
    }
}
```

### Koin Example

```kotlin
val transmissionModule = module {
    // Define transformers
    single { MyTransformer1() }
    single { MyTransformer2() }
    single { MyTransformer3() }
    
    // Create a set of all transformers
    single<Set<Transformer>> { 
        setOf(get<MyTransformer1>(), get<MyTransformer2>(), get<MyTransformer3>()) 
    }
    
    // Define router
    single { 
        TransmissionRouterBuilder.build {
            addTransformerSet(get<Set<Transformer>>())
        }
    }
}
```

## Proguard / R8 Configuration

If you're using ProGuard or R8, add the following rules to your ProGuard configuration:

```proguard
# Transmission Library
-keep class com.trendyol.transmission.** { *; }
-keepclassmembers class * implements com.trendyol.transmission.Transmission { *; }
```

This ensures that your Transmission interfaces and classes are not obfuscated, which is important for reflection-based operations in the library.