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
}

publishing {

	publications {
		create<MavenPublication>("release") {
			groupId = "com.trendyol"
			artifactId = "transmission"
			version = "0.0.1"
			afterEvaluate {
				from(components["java"])
			}
		}
	}
}
