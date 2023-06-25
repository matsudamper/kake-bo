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
                implementation(libs.kotlin.coroutines.core)
            }
        }
        val jsMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.ui)

                implementation("io.ktor:ktor-client-logging-js:2.2.4")
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.js)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}
