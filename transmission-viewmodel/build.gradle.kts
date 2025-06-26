plugins {
    id("com.trendyol.transmission.android.library")
    id("com.trendyol.transmission.kotlin.multiplatform")
    id("com.trendyol.transmission.publish")
    id("com.trendyol.transmission.dokka")
}

android {
    namespace = "com.trendyol.transmission.viewmodel"
}

kotlin {
    sourceSets {
        applyDefaultHierarchyTemplate()
        commonMain.dependencies {
            api(project(":transmission"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.junit)
            implementation(kotlin("test"))
            implementation(libs.turbine)
        }
    }
}


