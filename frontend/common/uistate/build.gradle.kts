plugins {
    kotlin("multiplatform")
    id("net.matsudamper.money.buildlogic.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(libs.compose.material3)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}
