

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
    id("net.matsudamper.money.buildlogic.compose")
    id("net.matsudamper.money.buildlogic.androidLibrary")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    androidTarget()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.navigation)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.viewmodel)
                implementation(projects.frontend.common.usecase)
                implementation(projects.frontend.common.graphql)

                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(libs.kotlin.serialization.json)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)

                implementation(libs.koinCore)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidxCoreKtx)
                implementation(libs.androidxLifecycleViewModelKtx)
                implementation(libs.androidxLifecycleViewModelCompose)
            }
        }
        explicitApi()
    }
}

android {
    namespace = "net.matsudamper.money.ui.root"
}
dependencies {
    implementation(project(":frontend:common:di"))
}

// wasm
compose.experimental {
    web.application {}
}
