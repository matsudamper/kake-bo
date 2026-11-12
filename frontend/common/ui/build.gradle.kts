import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
    id("net.matsudamper.money.buildlogic.paparazzi")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "net.matsudamper.money.frontend.common.ui.generated.resources"
    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.ui"
        // Android KMP Library Pluginはandroidリソースが既定で無効で、無効だとCompose Resourcesがassetsに含まれない
        androidResources {
            enable = true
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.navigation)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.coilCompose)
                implementation(libs.composeRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)
                implementation(libs.composeUiToolingPreview)
                implementation(libs.composeComponentsResources)
            }
        }
        getByName("wasmJsMain") {
            dependencies {
                implementation(libs.kotlinxBrowser)
                implementation(libs.coilNetworkKtor3)
                implementation(libs.ktorClientJs)
                implementation(libs.ktorClientLogging)
            }
        }
        getByName("androidMain") {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.feature.localstore)
                implementation(projects.frontend.common.graphql)
                implementation(libs.kotlin.datetime)
                implementation(libs.composeRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)

                implementation(libs.androidActivityActivityCompose)
                implementation(libs.coilNetworkOkhttp)
                implementation(libs.zoomable)
            }
        }
        getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}
