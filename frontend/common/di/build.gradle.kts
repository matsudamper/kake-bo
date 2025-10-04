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
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.feature.webauth)
                implementation(projects.frontend.common.feature.localstore)
                implementation(projects.frontend.common.graphql)
                implementation(projects.frontend.common.usecase)

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
