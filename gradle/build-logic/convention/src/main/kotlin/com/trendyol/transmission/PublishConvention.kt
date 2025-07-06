package com.trendyol.transmission

import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.vanniktech.maven.publish.MavenPublishBaseExtension

class PublishConvention : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        plugins.apply("com.vanniktech.maven.publish")

        group = rootProject.property("GROUP") as String
        version = rootProject.property("VERSION_NAME") as String

        extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

            pom {
                name.set(project.name.replaceFirstChar { it.uppercase() })
                description.set("Experimental library for asynchronous communication")
                inceptionYear.set("2024")
                url.set("https://github.com/Trendyol/transmission")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("yigitozgumus")
                        name.set("Yiğit Özgümüş")
                        url.set("yigitozgumus1@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/Trendyol/transmission")
                    connection.set("scm:git:https://github.com/Trendyol/transmission.git")
                    developerConnection.set("scm:git:ssh://git@github.com:Trendyol/transmission.git")
                }
            }

            signAllPublications()

        }
    }
}