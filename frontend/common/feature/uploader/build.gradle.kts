plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.androidLibrary")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    androidTarget()
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(libs.kotlin.coroutines.core)
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.workRuntimeKtx)
                implementation(libs.okHttp)
                implementation(libs.kotlin.serialization.json)
                implementation(projects.frontend.common.feature.localstore)
                implementation(projects.frontend.common.graphql)
            }
        }
    }
    explicitApi()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

android {
    namespace = "net.matsudamper.money.frontend.common.feature.uploader"
}
