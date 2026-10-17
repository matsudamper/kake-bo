import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.usecase"
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(projects.frontend.common.base)
                api(projects.frontend.common.feature.webauth)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.graphql)
                implementation(projects.shared)
                implementation(libs.kotlin.datetime)

                implementation(libs.koinCore)
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
