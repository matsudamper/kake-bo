import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.feature.logging"
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    jvm { }
    sourceSets {
        getByName("androidMain") {
            dependencies {
                implementation(libs.timber)
            }
        }
    }
    explicitApi()
}
