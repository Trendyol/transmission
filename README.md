# Transmission

Abstraction experiment. Kotlin library to create a communication network between different business logic blocks for your Android Projects.

## Motivation

Business logic can accumulate for complex screens in Android quite quickly.

Business logic is abstracted to some controller class, UseCase, or something similar to control and maintain the complexity.

However, the reactive nature of the data we present on the screen often prevents us from genuinely decoupling this logic. For example, in scenarios where one part of the screen controls another, they must be updated together.

This library brings an additional layer to this complexity, intending to decouple the business logic blocks from each other.

You can read more about motivation and how the library works [here](https://medium.com/@yigitozgumus/decoupling-business-logic-in-android-projects-8f1daa209fcb).

## Installation

The library is distributed through Jitpack.

**Add the repository to the root build.gradle**

```kotlin
repositories {
	maven { url("https://jitpack.io") }
}
```

**Add the library to the dependencies block of your app's module build.gradle** (And any module you need to use)

```kotlin
implementation("com.github.Trendyol:transmission:<latest_version>")
```

## How to use it?

The library consists of the following building blocks:
- **Transmission**: Unit of information being transferred. Three subtypes are: _signal_, _effect_, and _data_.
- **Transformer**: Processes _transmission_. Might receive _signal_ or _effect_ and might produce either an _effect_ or _data_.
- **TransmissionRouter**: Processes incoming signals and effects and passes along the produced data. Responsible for distributing the _signals_ and _effects_ between **Transformers**.

`Transmission` interactions are depicted below:

```mermaid
graph TD;
	Signal --> Effect
	Signal --> Data
	Effect --> Data
	Effect --> Effect
```

### How to create a Transformer

Transformers are responsible for handling `signal`s and `effect`s. They have an internal extension method that can be used in any `MutableStateFlow` that holds `Transmission.Data` called `reflectUpdates`.

```kotlin
class InputTransformer @Inject constructor() : Transformer() {  

	private val _inputState = MutableStateFlow(InputUiState())  
	private val inputState = _inputState.reflectUpdates()
  
	override suspend fun onSignal(signal: Transmission.Signal) {  
	    when (signal) {  
		    is InputSignal.InputUpdate -> {  
		       _inputState.update { it.copy(writtenText = signal.value) }  
		       sendEffect(InputEffect.InputUpdate(signal.value))  
		    }  
		}
	}  
	  
	override suspend fun onEffect(effect: Transmission.Effect) {  
	    TODO("Not yet implemented")  
	}
}
```

### How to use TransmissionRouter

Possibly in your ViewModel:

```kotlin
init {  
    viewModelScope.launch {  
       transmissionRouter.initialize(onData = {}, onEffect = {})  
    }  
}
```

The TransmissionRouter takes a set of `Transformer`s as a parameter. Building the Router heavily depends on your app's architecture and dependency injection choices. Here is an approach from the sample app using Hilt:

```kotlin
@InstallIn(ViewModelComponent::class)  
@Module  
interface FeaturesModule {  
  
    @Multibinds  
    fun bindTransformerSet(): Set<Transformer>  
  
    @Binds  
    @IntoSet    
    fun bindInputTransformer(impl: InputTransformer): Transformer  
  
    @Binds    
    @IntoSet    
    fun bindOutputTransformer(impl: OutputTransformer): Transformer  
  
    @Binds    
    @IntoSet    
    fun bindColorPickerTransformer(impl: ColorPickerTransformer): Transformer  
  
    @Binds    
    @IntoSet    
    fun bindMultiOutputTransformer(impl: MultiOutputTransformer): Transformer  
  
    companion object {  
       @Provides  
       fun provideRouter(transformerSet: @JvmSuppressWildcards Set<Transformer>): TransmissionRouter {
          return TransmissionRouter(transformerSet)  
       }  
    }  
  
}
```

## Licence
----------
	MIT License
	
	Copyright (c) 2024 Yiğit Özgümüş
	
	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:
	
	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.
	
	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE
