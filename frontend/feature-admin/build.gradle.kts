plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.feature.admin"
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
                implementation(projects.frontend.common.di)
                implementation(projects.frontend.common.feature.webauth)

                implementation(libs.apolloRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)
                implementation(libs.composeRuntime)
                implementation(libs.composeComponentsResources)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.koinCore)
                implementation(libs.koinCompose)
                implementation(libs.coilRuntime)
                implementation(libs.coilCompose)
            }
        }
    }
    explicitApi()
}
