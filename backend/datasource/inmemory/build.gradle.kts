plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
    }
    jvmToolchain(25)
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.backend.base)
                implementation(projects.backend.app.interfaces)

                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)

                implementation(libs.jedis)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
