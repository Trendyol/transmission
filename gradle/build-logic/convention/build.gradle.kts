plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

gradlePlugin {
    plugins {
        register("publishConvention") {
            id = "com.trendyol.transmission.publish"
            implementationClass = "com.trendyol.transmission.PublishConvention"
        }
        register("androidLibrary") {
            id = "com.trendyol.transmission.android.library"
            implementationClass = "com.trendyol.transmission.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "com.trendyol.transmission.android.application"
            implementationClass = "com.trendyol.transmission.AndroidApplicationConventionPlugin"
        }
        register("kotlinMultiplatform") {
            id =  "com.trendyol.transmission.kotlin.multiplatform"
            implementationClass = "com.trendyol.transmission.KotlinMultiplatformConventionPlugin"
        }
        register("dokka") {
            id = "com.trendyol.transmission.dokka"
            implementationClass = "com.trendyol.transmission.DokkaConventionPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.maven.publish.plugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.dokka.gradlePlugin)
}