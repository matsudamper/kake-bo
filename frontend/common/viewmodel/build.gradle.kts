plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":frontend:common:base"))
                implementation(project(":frontend:common:uistate"))
                implementation(project(":frontend:common:ui"))
                implementation(project(":frontend:common:schema"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.kotlin.datetime)
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
