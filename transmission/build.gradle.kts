plugins {
    id("java-library")
    alias(libs.plugins.jetbrainsKotlinJvm)
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.turbine)
}

tasks.test {
    useJUnitPlatform()
}

publishing {

    publications {
        create<MavenPublication>("release") {
            groupId = "com.trendyol"
            artifactId = "transmission"
            version = "1.0.4"
            afterEvaluate {
                from(components["java"])
            }
        }
    }
}
