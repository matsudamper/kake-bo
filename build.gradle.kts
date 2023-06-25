plugins {
    kotlin("multiplatform") apply false
    alias(libs.plugins.kotlin.serialization) apply false
    id("org.jetbrains.compose") apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
}

dependencies {
}


allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {

    }
}
