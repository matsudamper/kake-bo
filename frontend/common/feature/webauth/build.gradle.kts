plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("net.matsudamper.money.buildlogic.compose")
    id("net.matsudamper.money.buildlogic.androidLibrary")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.feature.webauth"
        compileSdk = 36
        minSdk = 34
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                implementation(libs.kotlin.serialization.json)

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

                implementation("io.ktor:ktor-client-logging-js:3.4.0")
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
