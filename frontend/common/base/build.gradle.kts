plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.base"
        compileSdk = 36
        minSdk = 34
    }
    js(IR) {
        browser()
    }
    jvm { }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                api(projects.frontend.common.feature.logging)
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)

                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.datetime)

                implementation(libs.ktorClientCore)
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
        val jvmMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)

                implementation(libs.androidxCoreKtx)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotestRunnerJunit5)
                implementation(libs.kotlinRefrect)
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
