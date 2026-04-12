plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
    id("net.matsudamper.money.buildlogic.compose")
    id("net.matsudamper.money.buildlogic.androidLibrary")
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "net.matsudamper.money.frontend.common.base.nav"
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    jvm { }
    androidTarget()
    jvmToolchain(libs.versions.javaToolchain.get().toInt())
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)
                implementation(libs.composeAnimation)

                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.datetime)

                implementation(libs.ktorClientCore)

                api(libs.jetbrainsNavigation3Ui)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)

                implementation("io.ktor:ktor-client-logging-js:3.4.2")
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientJs)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.androidxLifecycleViewModelCompose)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.androidxLifecycleViewModelCompose)
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
                implementation(libs.kotestFrameworkEngine)
                implementation(libs.kotestAssertionsCore)
            }
        }
    }
    explicitApi()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
