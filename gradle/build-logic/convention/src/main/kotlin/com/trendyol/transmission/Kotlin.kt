package com.trendyol.transmission


import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

fun Project.configureKotlin(enableAllWarningsAsErrors: Boolean = false) {
    configureJava()

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            if (enableAllWarningsAsErrors) {
                allWarningsAsErrors.set(true)
            }

            if (this is KotlinJvmCompilerOptions) {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }
}