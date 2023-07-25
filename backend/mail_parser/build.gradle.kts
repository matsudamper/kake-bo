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
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))

                implementation(project(":shared"))
                implementation(project(":backend:base"))

                implementation(libs.kotlin.serialization.json)
                implementation(libs.jackson.databind)
                implementation(libs.jackson.kotlin)
                implementation(libs.jsoup)
                implementation(libs.log4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}
