package net.matsudamper.money.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.accessors.runtime.extensionOf
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }
            extensions.configure<ComposeExtension> {
//                kotlinCompilerPlugin.set("1.5.2-beta01")
//                kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.9.10")
            }
            configure<KotlinMultiplatformExtension> {
                val compose = extensionOf(this, "compose") as ComposePlugin.Dependencies
                sourceSets.getByName("commonMain") {
                    dependencies {
                        implementation(compose.materialIconsExtended)
                    }
                }
            }
        }
    }
}
