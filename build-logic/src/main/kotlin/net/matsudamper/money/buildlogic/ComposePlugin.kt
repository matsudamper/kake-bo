package net.matsudamper.money.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }
            afterEvaluate {
                extensions.findByType(KotlinMultiplatformExtension::class.java)?.apply {
                    if (targets.any { it.name == "js" }) {
                        js {
                            binaries.executable()
                        }
                    }
                }
            }
        }
    }
}
