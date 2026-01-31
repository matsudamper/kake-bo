plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.navigation)
                implementation(libs.kotlin.datetime)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.uiToolingPreview)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktorClientJs)
                implementation(libs.ktorClientLogging)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(libs.kotlin.datetime)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.androidActivityActivityCompose)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}

android {
    namespace = "net.matsudamper.money.frontend.common.ui"
}
