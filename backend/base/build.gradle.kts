plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
    }
    sourceSets {
        jvmToolchain(25)
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))

                implementation(projects.shared)

                implementation(libs.kotlin.serialization.json)
                implementation(libs.jackson.databind)
                implementation(libs.jackson.kotlin)
                implementation(libs.log4j.api)
                implementation(libs.opentelemetryApi)
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

// tasks.test {
//    useJUnitPlatform()
// }
