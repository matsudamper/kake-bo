

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.root)
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.viewmodel)
                implementation(projects.frontend.common.graphql)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.html.core)
                implementation(compose.runtime)

                implementation(libs.koinCore)
            }
        }
    }
}

// wasm
compose.experimental {
    web.application {}
}
