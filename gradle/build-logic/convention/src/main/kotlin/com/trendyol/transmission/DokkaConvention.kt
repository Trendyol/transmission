package com.trendyol.transmission

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

/**
 * Convention plugin for configuring Dokka documentation generation.
 * 
 * This plugin applies consistent Dokka configuration across all modules,
 * including source links, external documentation links, and output formatting.
 */
class DokkaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(DokkaPlugin::class.java)
            }
            
            tasks.withType(DokkaTask::class.java).configureEach {
                dokkaSourceSets.configureEach {
                    // Module name and description
                    moduleName.set(project.name)
                    
                    // Source links - link to GitHub source
                    sourceLink {
                        localDirectory.set(project.file("src"))
                        remoteUrl.set(URL("https://github.com/Trendyol/transmission/tree/main/${project.name}/src"))
                        remoteLineSuffix.set("#L")
                    }
                    
                    // External documentation links
                    externalDocumentationLink {
                        url.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/"))
                        packageListUrl.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/package-list"))
                    }
                    
                    externalDocumentationLink {
                        url.set(URL("https://kotlin.github.io/kotlinx.coroutines/"))
                        packageListUrl.set(URL("https://kotlin.github.io/kotlinx.coroutines/package-list"))
                    }
                    
                    // Suppress obvious functions
                    suppressObviousFunctions.set(true)
                    
                    // Include non-public members if they have documentation
                    includeNonPublic.set(false)
                    
                    // Skip empty packages
                    skipEmptyPackages.set(true)
                    
                    // Skip deprecated
                    skipDeprecated.set(false)
                    
                    // Reporting undocumented
                    reportUndocumented.set(true)
                    
                    // Fail on warning
                    failOnWarning.set(false)
                    
                    // Platform documentation
                    displayName.set("Transmission ${project.name}")
                }
            }
        }
    }
} 