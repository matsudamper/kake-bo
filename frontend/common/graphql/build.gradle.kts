plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
        }
    }
}

android {
    namespace = "net.matsudamper.money.frontend.graphql"
    compileSdk = 34
    defaultConfig {
        minSdk = 33
        buildConfigField("String", "SERVER_PROTOCOL", "\"${project.rootProject.properties["net.matsudamper.money.android.serverProtocol"]}\"")
        buildConfigField("String", "SERVER_HOST", "\"${project.rootProject.properties["net.matsudamper.money.android.serverHost"]}\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}
