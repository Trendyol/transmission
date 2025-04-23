plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    google()
}

gradlePlugin {
    plugins {
        create("publishConvention") {
            id = "com.trendyol.transmission.publish"
            implementationClass = "com.trendyol.transmission.PublishConvention"
        }
    }
}

dependencies {
    implementation(libs.gradle.maven.publish.plugin)
}