plugins {
    kotlin("multiplatform")
    id("net.matsudamper.money.buildlogic.compose")
    id("com.android.library")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    androidTarget()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.navigation)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.graphql)
                implementation(projects.shared)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(libs.kotlin.datetime)
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
    namespace = "net.matsudamper.money.frontend.common.viewmodel"
    compileSdk = 34
    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
