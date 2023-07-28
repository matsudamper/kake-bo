plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        withJava()
    }
    jvmToolchain(17)
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(project(":shared"))
                api(project(":backend:base"))
                api(project(":backend:db:schema"))

                api(kotlin("stdlib"))
                api(kotlin("reflect"))
                api(libs.kotlin.coroutines.core)
                api(libs.kotlin.serialization.json)
                api("jakarta.mail:jakarta.mail-api:2.1.2")
                api("org.eclipse.angus:jakarta.mail:2.0.2")


            }
        }
        val jvmTest by getting {
            dependencies {
                api(kotlin("test"))
            }
        }
    }
}
