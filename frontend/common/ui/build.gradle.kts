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
                api(project(":frontend:common:base"))
                api(project(":frontend:common:uistate"))
                api(libs.kotlin.datetime)
                api(compose.runtime)
                api(compose.foundation)
                api(libs.compose.material3)
            }
        }
        val commonTest by getting {
            dependencies {
                api(kotlin("test"))
            }
        }
    }
    explicitApi()
}
