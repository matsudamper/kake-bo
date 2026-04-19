plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.androidLibrary")
}

kotlin {
    androidTarget()
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val androidMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(libs.timber)
            }
        }
    }
    explicitApi()
}

android {
    namespace = "net.matsudamper.money.frontend.common.feature.logging"
}
