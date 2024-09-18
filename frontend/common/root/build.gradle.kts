

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
    id("net.matsudamper.money.buildlogic.compose")
    id("com.android.library")
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
                implementation(projects.frontend.common.graphql)

                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(libs.kotlin.serialization.json)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)
            }
        }
    }
}

android {
    namespace = "net.matsudamper.money.ui.root"
    compileSdk = 34
    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// wasm
compose.experimental {
    web.application {}
}
