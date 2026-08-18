plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.feature.logging"
        compileSdk = 37
        minSdk = 34
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
