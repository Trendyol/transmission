plugins {
    id("com.trendyol.transmission.android.library")
    id("com.trendyol.transmission.kotlin.multiplatform")
    id("com.trendyol.transmission.publish")
}

android {
    namespace = "com.trendyol.transmission.core"
}

kotlin {
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
