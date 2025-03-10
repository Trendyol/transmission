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
    api(project(":transmission"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.junit)
    implementation(kotlin("test"))
    implementation(libs.turbine)
}

tasks.test {
    useJUnitPlatform()
}

publishing {

    publications {
        create<MavenPublication>("release") {
            groupId = "com.trendyol"
            artifactId = "transmission-test"
            version = "1.4.1"
            afterEvaluate {
                from(components["java"])
            }
        }
    }
}
