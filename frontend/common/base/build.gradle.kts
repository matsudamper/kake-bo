plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.ui)
                api(libs.kotlin.coroutines.core)
            }
        }
        val jsMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.ui)

                api("io.ktor:ktor-client-logging-js:2.2.4")
                api(libs.ktor.client.core)
                api(libs.ktor.client.js)
            }
        }
        val jsTest by getting {
            dependencies {
                api(kotlin("test"))
            }
        }
    }
    explicitApi()
}
