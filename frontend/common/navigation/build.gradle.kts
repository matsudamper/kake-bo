plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    jvm { }
    sourceSets {
        jvmToolchain(21)
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(compose.runtime)
                implementation(compose.ui)

                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.datetime)

                implementation(libs.ktorClientCore)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.shared)

                implementation(compose.runtime)
                implementation(compose.ui)

                implementation("io.ktor:ktor-client-logging-js:3.1.2")
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
