plugins {
    alias(libs.plugins.kotlinMultiplatform)
//    id("maven-publish")
}

kotlin {
    jvm()
}

//java {
//    sourceCompatibility = JavaVersion.VERSION_17
//    targetCompatibility = JavaVersion.VERSION_17
//}
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
//tasks.test { useJUnitPlatform() }

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
