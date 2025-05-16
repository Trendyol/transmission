import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("com.trendyol.transmission.android.application")
    id("com.trendyol.transmission.kotlin.multiplatform")
}

android {
    namespace = "com.trendyol.transmission.counter"
}

kotlin {
    sourceSets {
        applyDefaultHierarchyTemplate()
        val desktopMain by getting
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(compose.runtime)
            implementation(project(":transmission"))
            implementation(compose.foundation)
            implementation(libs.atomicfu)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":transmission-test"))
            implementation(libs.turbine)
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.trendyol.transmission.counter.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Counter Sample"
            packageVersion = "1.0.0"
        }
    }
}

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    binaries.framework {
        baseName = "Counter"
        isStatic = true
    }
}
