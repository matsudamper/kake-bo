plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(project(":shared"))
                implementation(project(":frontend:common:ui"))
                implementation(project(":frontend:common:viewmodel"))

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(compose.html.core)
            }
        }
    }
}

// wasm
compose.experimental {
    web.application {}
}
