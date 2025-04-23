package com.trendyol.transmission

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

fun Project.configureJava() {
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

private fun Project.java(action: JavaPluginExtension.() -> Unit) {
    extensions.configure<JavaPluginExtension>(action)
}