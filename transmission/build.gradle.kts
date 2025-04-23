import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("com.trendyol.transmission.publish")
}

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        applyDefaultHierarchyTemplate()
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlin.stdlib)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit)

            implementation(kotlin("test"))
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "com.trendyol.transmission.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
