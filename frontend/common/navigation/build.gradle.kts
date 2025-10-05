plugins {
    alias(libs.plugins.kotlin.multiplatform)
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

                implementation(compose.runtime)
                implementation(compose.ui)

                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.datetime)

                implementation(libs.ktorClientCore)

                api(libs.androidxNavigation3Runtime)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(compose.runtime)
                implementation(compose.ui)

                implementation("io.ktor:ktor-client-logging-js:3.3.0")
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientJs)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.androidxLifecycleViewModelCompose)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.androidxLifecycleViewModelCompose)
                implementation(libs.androidxNavigation3Runtime)
                implementation(libs.androidxNavigation3Ui)
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
