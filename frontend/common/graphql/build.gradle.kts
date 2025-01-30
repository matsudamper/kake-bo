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
        }
    }
}

val localProperties = Properties().also { properties ->
    val propertiesFile = File("$rootDir/local.properties")
    if (propertiesFile.exists()) {
        properties.load(propertiesFile.inputStream())
    }
}
android {
    namespace = "net.matsudamper.money.frontend.graphql"
    defaultConfig {
        buildConfigField("String", "SERVER_PROTOCOL", "\"${localProperties["net.matsudamper.money.android.serverProtocol"]}\"")
        buildConfigField("String", "SERVER_HOST", "\"${System.getenv("SERVER_HOST") ?: localProperties["net.matsudamper.money.android.serverHost"]}\"")
    }
    buildFeatures {
        buildConfig = true
    }
}
