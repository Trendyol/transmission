import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
//    id("maven-publish")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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

//
//dependencies {
//    implementation(libs.kotlinx.coroutines.core)
//    implementation(libs.kotlin.stdlib)
//    testImplementation(libs.kotlinx.coroutines.test)
//    testImplementation(libs.junit)
//
//    testImplementation(kotlin("test"))
//    testImplementation(libs.turbine)
//}
//

//publishing {
//    publications {
//        create<MavenPublication>("release") {
//            groupId = "com.trendyol"
//            artifactId = "transmission"
//            version = libs.versions.transmission.core.get()
//            afterEvaluate { from(components["java"]) }
//        }
//    }
//}
