plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.viewmodel"
    }
    wasmJs {
        browser()
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        getByName("commonMain") {
            dependencies {
                implementation(projects.frontend.common.base)
                api(projects.frontend.common.feature.webauth)
                api(projects.frontend.common.feature.uploader)
                implementation(projects.frontend.common.navigation)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.graphql)
                implementation(projects.frontend.common.usecase)
                implementation(projects.shared)
                implementation(libs.composeRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.apolloRuntime)

                implementation(libs.koinCore)
            }
        }
        getByName("androidMain") {
            dependencies {
                implementation(projects.frontend.common.feature.localstore)
            }
        }
        getByName("wasmJsMain") {
            dependencies {
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
