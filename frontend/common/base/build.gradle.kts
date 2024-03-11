plugins {
    kotlin("multiplatform")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(17)
        val commonMain by getting {
            dependencies {
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

                implementation("io.ktor:ktor-client-logging-js:2.3.9")
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.js)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-runner-junit5:5.8.0")
                implementation("io.mockk:mockk:1.13.10")
            }
        }
    }
    explicitApi()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
