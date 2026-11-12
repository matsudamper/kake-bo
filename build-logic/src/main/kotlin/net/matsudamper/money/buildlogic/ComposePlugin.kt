package net.matsudamper.money.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }
            // Android Studio の Preview は ComposeViewAdapter を実行時に読み込むため、リリースに含まれない実行時専用で追加する
            pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
                configurations.matching { it.name == "androidRuntimeClasspath" }.configureEach {
                    dependencies.add(project.dependencies.create(libs.library("composeUiTooling")))
                }
            }
            afterEvaluate {
                extensions.findByType(KotlinMultiplatformExtension::class.java)?.apply {
                    if (targets.any { it.name == "wasmJs" }) {
                        @OptIn(ExperimentalWasmDsl::class)
                        wasmJs {
                            binaries.executable()
                        }
                    }
                }
            }
        }
    }
}
