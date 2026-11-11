plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.screenshot"
    }
    sourceSets {
        getByName("androidMain") {
            dependencies {
                api(libs.paparazzi)
                api(libs.composablePreviewScanner)
                implementation(libs.composeRuntime)
                implementation(libs.composeComponentsResources)
            }
        }
    }
    explicitApi()
}
