import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform") apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
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
            exclude { project.path == ":backend:datasource:db:schema" }
            exclude { project.path == ":backend:graphql" }
            exclude { project.path == ":frontend:common:schema" }
            exclude { it.file.path.contains("generated") }
        }
    }
}
