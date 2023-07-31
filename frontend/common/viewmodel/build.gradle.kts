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
                api(project(":frontend:common:ui"))
                api(project(":frontend:common:schema"))
                api(compose.runtime)
                api(compose.foundation)
                api(libs.compose.material3)
                api(libs.kotlin.datetime)
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
