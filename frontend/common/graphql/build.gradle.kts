import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.androidLibrary")
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
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                api(projects.frontend.common.graphql.schema)

                api(libs.apolloRuntime)
                implementation(libs.kotlin.datetime)
                api(libs.apolloNormalizedCache)
                implementation(libs.apolloAdapters)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.frontend.common.feature.localstore)
                implementation(libs.koinCore)
            }
        }
    }
}

android {
    namespace = "net.matsudamper.money.frontend.graphql"
}
