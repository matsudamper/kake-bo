plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.feature.notification"
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
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.navigation)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.viewmodel)
                implementation(projects.frontend.common.graphql)

                implementation(libs.apolloRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)
                implementation(libs.composeRuntime)
                implementation(libs.composeComponentsResources)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.serialization.json)
            }
        }
    }
    explicitApi()
}
