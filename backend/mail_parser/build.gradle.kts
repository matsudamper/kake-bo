plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(17)
        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib"))
                api(kotlin("reflect"))

                api(project(":shared"))
                api(project(":backend:base"))

                api(libs.kotlin.serialization.json)
                api(libs.jackson.databind)
                api(libs.jackson.kotlin)
                api(libs.jsoup)
                api(libs.log4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                api(kotlin("test"))
            }
        }
    }
    explicitApi()
}
