package com.trendyol.transmission.features

import com.trendyol.transmission.DefaultTransmissionRouter
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.input.InputTransformer
import com.trendyol.transmission.features.multioutput.MultiOutputTransformer
import com.trendyol.transmission.features.output.OutputTransformer
import com.trendyol.transmission.transformer.DefaultTransformer
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
	fun bindTransformerSet(): Set<DefaultTransformer>

	@Binds
	@IntoSet
	fun bindInputTransformer(impl: InputTransformer): DefaultTransformer

	@Binds
	@IntoSet
	fun bindOutputTransformer(impl: OutputTransformer): DefaultTransformer

	@Binds
	@IntoSet
	fun bindColorPickerTransformer(impl: ColorPickerTransformer): DefaultTransformer

	@Binds
	@IntoSet
	fun bindMultiOutputTransformer(impl: MultiOutputTransformer): DefaultTransformer

	companion object {
		@Provides
		fun provideRouter(
			transformerSet: @JvmSuppressWildcards Set<DefaultTransformer>
		): DefaultTransmissionRouter {
			return TransmissionRouter(transformerSet)
		}
	}

}
