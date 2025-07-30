pluginManagement {
	repositories {
		google {
			content {
				includeGroupByRegex("com\\.android.*")
				includeGroupByRegex("com\\.google.*")
				includeGroupByRegex("androidx.*")
			}
		}
		mavenCentral()
		gradlePluginPortal()
	}
}
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
		maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
	}
}

rootProject.name = "transmission"
include(":transmission")
include(":transmission-test")
include(":transmission-viewmodel")
include(":samples:components")
include(":samples:counter")
includeBuild("gradle/build-logic")
include(":transmission-visualizer")
