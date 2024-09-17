plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
    id("com.android.application")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.root)
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.viewmodel)
                implementation(projects.frontend.common.graphql)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)

                implementation(libs.androidActivityActivityCompose)
                implementation(libs.androidAppCompatAppCompat)
            }
        }
    }
}

android {
    compileSdk = 34
    namespace = "net.matsudamper.money"
    defaultConfig {
        minSdk = 34
        targetSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
