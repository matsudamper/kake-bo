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
        jvmToolchain(17)
        val commonMain by getting {
            dependencies {
                implementation(projects.frontend.common.feature.webauth)

                implementation(libs.koinCore)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

android {
    namespace = "net.matsudamper.money.frontend.common.di"
}