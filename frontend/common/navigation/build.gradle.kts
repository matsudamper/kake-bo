plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.base.nav"
    }
    wasmJs {
        browser()
    }
    jvm { }
    jvmToolchain(libs.versions.javaToolchain.get().toInt())
    sourceSets {
        getByName("commonMain") {
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
        getByName("wasmJsMain") {
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
        getByName("jvmMain") {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.androidxLifecycleViewModelCompose)
            }
        }
        getByName("androidMain") {
            dependencies {
                implementation(projects.shared)

                implementation(libs.composeRuntime)
                implementation(libs.composeUi)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.androidxLifecycleViewModelCompose)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotestRunnerJunit5)
                implementation(libs.kotlinRefrect)
            }
        }
        getByName("wasmJsTest") {
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
