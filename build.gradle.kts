import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("jvm") apply false
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.ktlint)
}

dependencies {
}

subprojects {
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
