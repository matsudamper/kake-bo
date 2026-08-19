plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.feature.logging"
    }
    js(IR) {
        browser()
    }
    jvm { }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val androidMain by getting {
            dependencies {
                implementation(libs.timber)
            }
        }
    }
    explicitApi()
}
