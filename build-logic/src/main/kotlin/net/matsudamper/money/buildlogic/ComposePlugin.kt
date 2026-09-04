package net.matsudamper.money.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }
        }
    }
}
