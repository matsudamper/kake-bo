plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.feature.webauth"
    }
    wasmJs {
        browser()
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                implementation(libs.kotlin.serialization.json)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)

                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.datetime)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinxBrowser)
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)

                implementation(libs.ktorClientLogging)
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientJs)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)

                implementation(libs.androidxCredentialsPlayServicesAuth)
                implementation(libs.androidxCredentials)
            }
        }
        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
