import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.ktlint)
}

dependencies {
}

subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.all {
                languageSettings.optIn("kotlin.time.ExperimentalTime")
            }
        }
    }

    afterEvaluate {
        extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            if (
                targets.any {
                    it.platformType == KotlinPlatformType.js
                }
            ) {
                js(IR) {
                }
            }
        }
    }
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            verbose.set(true)
            exclude { project.path == ":backend:datasource:db:schema" }
            exclude { project.path == ":backend:graphql" }
            exclude { project.path == ":frontend:common:graphql:schema" }
            exclude { it.file.path.contains("generated") }
        }
    }
}
