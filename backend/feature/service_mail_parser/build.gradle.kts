plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(21)
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))

                implementation(projects.shared)
                implementation(projects.backend.base)

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
