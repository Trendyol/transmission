package com.trendyol.transmission.components

import com.trendyol.transmission.components.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.components.input.InputTransformer
import com.trendyol.transmission.components.multioutput.MultiOutputTransformer
import com.trendyol.transmission.components.output.OutputTransformer
import com.trendyol.transmission.router.builder.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val dispatcherModule = module {
    single(named("DefaultDispatcher")) { Dispatchers.Default }
    single(named("IoDispatcher")) { Dispatchers.IO }
    single(named("MainDispatcher")) { Dispatchers.Main }
}

// Register individual Transformer bindings
val featuresModule = module {

    // Transformers (each using CoroutineDispatcher as a dependency)
    single { InputTransformer(get(named("DefaultDispatcher"))) }
    single { OutputTransformer(get(named("DefaultDispatcher"))) }
    single { ColorPickerTransformer(get(named("DefaultDispatcher"))) }
    single { MultiOutputTransformer(get(named("DefaultDispatcher"))) }

    // Collect all Transformers into a list (equivalent to @Multibinds Set<Transformer>)
    single<List<Transformer>> {
        listOf(
            get<InputTransformer>(),
            get<OutputTransformer>(),
            get<ColorPickerTransformer>(),
            get<MultiOutputTransformer>()
        )
    }

    // Provide TransmissionRouter using the transformer list
    single {
        TransmissionRouter {
            addTransformerSet(get<List<Transformer>>().toSet())
            addDispatcher(get(named("DefaultDispatcher")))
        }
    }
}

val viewModelModule = module {
    viewModel { ComponentViewModel(get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration) = startKoin {
    modules(viewModelModule, featuresModule, dispatcherModule)
    appDeclaration()
}

fun initKoin() = initKoin { }