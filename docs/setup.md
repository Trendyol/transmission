# Setting Up

This guide provides instructions for adding the Transmission library to your Kotlin Multiplatform project.

## Gradle Setup

### Maven Central (Recommended - Version 1.6.0+)

Starting from version 1.6.0, Transmission is published to Maven Central for better Kotlin Multiplatform support.

Add the dependency to your app's build.gradle file:

```groovy
// In your app's build.gradle file
dependencies {
    implementation 'com.trendyol:transmission:<latest-version>'
    
    // For Android ViewModel integration
    implementation 'com.trendyol:transmission-viewmodel:<latest-version>'
    
    // If you need testing utilities
    testImplementation 'com.trendyol:transmission-test:<latest-version>'
}
```

Or in Kotlin DSL:

```kotlin
// In your app's build.gradle.kts file
dependencies {
    implementation("com.trendyol:transmission:<latest-version>")
    
    // For Android ViewModel integration
    implementation("com.trendyol:transmission-viewmodel:<latest-version>")
    
    // If you need testing utilities
    testImplementation("com.trendyol:transmission-test:<latest-version>")
}
```

### JitPack (For Versions up to 1.5.0)

For versions 1.5.0 and earlier, you can use JitPack:

#### Step 1: Add JitPack Repository

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

#### Step 2: Add the JitPack Dependency

```groovy
// In your app's build.gradle file
dependencies {
    implementation 'com.github.Trendyol:transmission:<version-up-to-1.5.0>'
    
    // If you need testing utilities
    testImplementation 'com.github.Trendyol:transmission:transmission-test:<version-up-to-1.5.0>'
}
```

Or in Kotlin DSL:

```kotlin
// In your app's build.gradle.kts file
dependencies {
    implementation("com.github.Trendyol:transmission:<version-up-to-1.5.0>")
    
    // If you need testing utilities
    testImplementation("com.github.Trendyol:transmission:transmission-test:<version-up-to-1.5.0>")
}
```

Replace `<latest-version>` with the latest version from [GitHub Releases](https://github.com/Trendyol/transmission/releases).

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

## Complete Project Example

Here's a complete example of setting up Transmission in a multiplatform project:

### Module Structure

```
project/
├── shared/
│   ├── src/commonMain/kotlin/
│   │   ├── transmission/
│   │   │   ├── signals/
│   │   │   ├── effects/
│   │   │   ├── data/
│   │   │   └── transformers/
│   │   └── di/
│   └── build.gradle.kts
├── androidApp/
└── iosApp/
```

### shared/build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
}

kotlin {
    android()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.trendyol:transmission:<latest-version>")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.trendyol:transmission-test:<latest-version>")
            }
        }
    }
}
```

## Dependency Injection Setup

### With Koin (Multiplatform)

```kotlin
// shared/src/commonMain/kotlin/di/TransmissionModule.kt
val transmissionModule = module {
    // Define transformers
    single { UserTransformer() }
    single { AuthTransformer() }
    single { DataTransformer() }
    
    // Create transformer set
    single<Set<Transformer>> { 
        setOf(
            get<UserTransformer>(), 
            get<AuthTransformer>(), 
            get<DataTransformer>()
        ) 
    }
    
    // Define router
    single { 
        TransmissionRouter {
            addTransformerSet(get<Set<Transformer>>())
            setCapacity(Capacity.High)
        }
    }
}

// In your app initialization
fun initKoin() {
    startKoin {
        modules(transmissionModule)
    }
}
```

If you're using a dependency injection framework like Hilt (Android) or Koin, you can create a module for your Transmission components:

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
    ): TransmissionRouter = TransmissionRouter {
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
        TransmissionRouter {
            addTransformerSet(get<Set<Transformer>>())
        }
    }
}
```

## RouterViewModel Setup (Multiplatform)

### RouterViewModel Module

For multiplatform projects using ViewModels, add the RouterViewModel dependency:

```kotlin
// Multiplatform module build.gradle.kts
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.trendyol:transmission:<latest-version>")
                implementation("com.trendyol:transmission-viewmodel:<latest-version>")
                
                // Multiplatform ViewModel
                implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
            }
        }
    }
}

// Or for Android-only module
dependencies {
    implementation("com.trendyol:transmission:<latest-version>")
    implementation("com.trendyol:transmission-viewmodel:<latest-version>")
    
    // ViewModel and Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
}
```

### With Dependency Injection

#### Hilt + RouterViewModel

```kotlin
// Module setup
@Module
@InstallIn(SingletonComponent::class)
object TransmissionModule {
    
    @Provides
    @Singleton
    fun provideTransformers(): Set<Transformer> = setOf(
        UserTransformer(),
        AuthTransformer(),
        DataTransformer()
    )
    
    @Provides
    @Singleton
    fun provideTransformerSetLoader(
        transformers: Set<Transformer>
    ): TransformerSetLoader = object : TransformerSetLoader {
        override suspend fun load(): Set<Transformer> = transformers
    }
}

// ViewModel with injection
@HiltViewModel
class MainViewModel @Inject constructor(
    private val transformerSetLoader: TransformerSetLoader
) : RouterViewModel(
    loader = transformerSetLoader,
    config = RouterViewModelConfig(
        capacity = Capacity.High,
        dispatcher = Dispatchers.IO
    )
) {
    val uiState = streamDataAsState<UiState>(UiState.Loading)
    
    fun handleUserAction(action: UserAction) {
        processSignal(UserSignal.Action(action))
    }
}
```

#### Koin + RouterViewModel

```kotlin
// Koin module
val androidTransmissionModule = module {
    // Transformers
    single { UserTransformer() }
    single { AuthTransformer() }
    single { DataTransformer() }
    
    // Transformer set
    single<Set<Transformer>> { 
        setOf(get<UserTransformer>(), get<AuthTransformer>(), get<DataTransformer>()) 
    }
    
    // RouterViewModel configuration
    single { 
        RouterViewModelConfig(
            capacity = Capacity.High,
            dispatcher = get(named("ioDispatcher"))
        )
    }
    
    // ViewModels
    viewModel { 
        UserViewModel(
            transformerSet = get<Set<Transformer>>(),
            config = get<RouterViewModelConfig>()
        )
    }
}

// ViewModel implementation
class UserViewModel(
    transformerSet: Set<Transformer>,
    config: RouterViewModelConfig
) : RouterViewModel(transformerSet, config) {
    
    val userState = streamDataAsState<UserData>(UserData.Empty)
    val loadingState = streamDataAsState<LoadingData>(LoadingData.Idle)
    
    override fun onEffect(effect: Transmission.Effect) {
        when (effect) {
            is NavigationEffect -> handleNavigation(effect)
            is ErrorEffect -> showError(effect.message)
        }
    }
    
    fun login(credentials: Credentials) {
        processSignal(AuthSignal.Login(credentials))
    }
}
```

### Compose Integration

```kotlin
// Compose usage with RouterViewModel
@Composable
fun UserScreen(
    viewModel: UserViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    
    when (loadingState) {
        is LoadingData.Loading -> LoadingIndicator()
        is LoadingData.Error -> ErrorMessage(loadingState.message)
        else -> {
            UserContent(
                userData = userState,
                onLoginClick = { credentials ->
                    viewModel.login(credentials)
                }
            )
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

# RouterViewModel (if using)
-keep class com.trendyol.transmissionviewmodel.** { *; }
```

This ensures that your Transmission interfaces and classes are not obfuscated, which is important for reflection-based operations in the library.