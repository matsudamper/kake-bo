plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.root)
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.di)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.viewmodel)
                implementation(projects.frontend.common.graphql)
                implementation(projects.frontend.common.feature.localstore)

                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)
                implementation(libs.composeRuntime)
                implementation(libs.composeComponentsResources)

                implementation(libs.koinCore)
                implementation(libs.coilRuntime)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.frontend.common.navigation)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(libs.composeHtmlCore)
                implementation(libs.androidxComposeSaveable)
            }
        }
    }
}
