plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.androidLibrary")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.usecase"
        compileSdk = 36
        minSdk = 34
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                api(projects.frontend.common.feature.webauth)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.graphql)
                implementation(projects.shared)
                implementation(libs.kotlin.datetime)

                implementation(libs.koinCore)
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
