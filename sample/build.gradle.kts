plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.android.hilt)
	alias(libs.plugins.jetbrains.kotlin.kapt)
}

android {
	namespace = "com.trendyol.transmission"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.trendyol.transmission"
		minSdk = 24
		targetSdk = 34
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		jvmTarget = "17"
	}
	buildFeatures {
		compose = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.7"
	}
	kapt {
		correctErrorTypes = true
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

dependencies {
	implementation(project(":transmission"))
	testImplementation(project(":transmission-test"))

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)


	implementation(libs.androidx.lifecycle.viewmodel)
	implementation(libs.androidx.lifecycle.runtime.compose)

	implementation(libs.hilt.android)
	kapt(libs.hilt.android.compiler)

	implementation(libs.androidx.material3)
	testImplementation(libs.junit)

	testImplementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.junit)
	testImplementation(kotlin("test"))
	testImplementation(libs.turbine)

	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
}
