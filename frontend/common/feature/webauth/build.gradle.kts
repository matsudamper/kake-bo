plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.compose")
    id("net.matsudamper.money.buildlogic.androidLibrary")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    androidTarget()
    sourceSets {
        jvmToolchain(17)
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(compose.runtime)
                implementation(compose.ui)

                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.datetime)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(compose.runtime)
                implementation(compose.ui)

                implementation("io.ktor:ktor-client-logging-js:2.3.12")
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientJs)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(compose.runtime)
                implementation(compose.ui)

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

android {
    namespace = "net.matsudamper.money.frontend.common.feature.webauth"
    buildFeatures {
        buildConfig = true
    }
}