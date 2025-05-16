package com.trendyol.transmission

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
        }

        kotlin {
            applyDefaultHierarchyTemplate()

            applyAndroid(pluginManager)

            applyJvm(pluginManager)

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            configureKotlin()
        }
    }
}

internal fun Project.kotlin(action: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure<KotlinMultiplatformExtension>(action)
}

internal val Project.kotlin: KotlinMultiplatformExtension
    get() = extensions.getByType<KotlinMultiplatformExtension>()

internal fun KotlinMultiplatformExtension.applyAndroid(pluginManager: PluginManager) {
    if (pluginManager.hasPlugin("com.android.library")) {
        androidTarget {
            publishLibraryVariants("release")
        }
    }
    if (pluginManager.hasPlugin("com.android.application")) {
        androidTarget()
    }
}

internal fun KotlinMultiplatformExtension.applyJvm(pluginManager: PluginManager) {
    if (pluginManager.hasPlugin("org.jetbrains.compose")) {
        jvm("desktop")
    } else {
        jvm()
    }
}