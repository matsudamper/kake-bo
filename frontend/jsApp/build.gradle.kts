

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
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
                implementation(project(":shared"))
                implementation(project(":frontend:common:base"))
                implementation(project(":frontend:common:ui"))
                implementation(project(":frontend:common:viewmodel"))
                implementation(project(":frontend:common:schema"))

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.html.core)
                implementation(compose.runtime)
            }
        }
    }
}

// wasm
compose.experimental {
    web.application {}
}
