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
                api(kotlin("stdlib"))

                api(project(":shared"))
                api(project(":frontend:common:base"))
                api(project(":frontend:common:ui"))
                api(project(":frontend:common:viewmodel"))
                api(project(":frontend:common:schema"))
                api(project(":frontend:common:uistate"))

                api(kotlin("stdlib"))
                api(libs.kotlin.serialization.json)
                api(compose.html.core)
            }
        }
    }
}

// wasm
compose.experimental {
    web.application {}
}
