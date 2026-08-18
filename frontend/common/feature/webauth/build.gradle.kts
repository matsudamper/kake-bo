plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("com.android.kotlin.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.feature.webauth"
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
                implementation(libs.kotlin.serialization.json)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)

                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.datetime)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)

                implementation("io.ktor:ktor-client-logging-js:3.5.0")
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
        val jsTest by getting {
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
