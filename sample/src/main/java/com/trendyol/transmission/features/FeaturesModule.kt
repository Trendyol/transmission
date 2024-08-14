package com.trendyol.transmission.features

import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.input.InputTransformer
import com.trendyol.transmission.features.multioutput.MultiOutputTransformer
import com.trendyol.transmission.features.output.OutputTransformer
import com.trendyol.transmission.router.builder.TransmissionRouterBuilder
import com.trendyol.transmission.transformer.Transformer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

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
		fun provideRouter(
			transformerSet: @JvmSuppressWildcards Set<Transformer>
		): TransmissionRouter {
			return TransmissionRouterBuilder.build {
				addTransformerSet(transformerSet)
			}
		}
	}

}
