plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.viewmodel"
        compileSdk = 36
        minSdk = 34
    }
    js(IR) {
        browser()
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
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
        val androidMain by getting {
            dependencies {
                implementation(projects.frontend.common.feature.localstore)
            }
        }
        val jsMain by getting {
            dependencies {
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
