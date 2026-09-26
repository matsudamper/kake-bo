plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.base"
    }
    wasmJs {
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
