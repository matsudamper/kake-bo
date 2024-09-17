plugins {
    kotlin("multiplatform")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    jvm {}
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(libs.kotlin.datetime)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(libs.compose.material3)
                implementation(compose.components.uiToolingPreview)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktorClientJs)
                implementation(libs.ktorClientLogging)
            }
        }
        val jvmMain by getting {
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}
