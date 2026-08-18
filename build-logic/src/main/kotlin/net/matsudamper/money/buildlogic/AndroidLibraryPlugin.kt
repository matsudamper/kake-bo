package net.matsudamper.money.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("unused")
class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }
            androidLibrary {
                compileSdk = 36
                defaultConfig {
                    minSdk = 34
                }
                lint {
                    targetSdk = 36
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.toVersion(libs.findVersion("java").get().displayName.toInt())
                    targetCompatibility = JavaVersion.toVersion(libs.findVersion("java").get().displayName.toInt())
                }
            }
            tasks.withType<KotlinCompile>().configureEach {
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(libs.findVersion("java").get().displayName))
                }
            }
        }
    }
}
