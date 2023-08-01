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
                implementation(project(":shared"))
                implementation(project(":backend:base"))
                implementation(project(":backend:db:schema"))

                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.jakarta.mail.api)
                implementation(libs.jakarta.mail)
                implementation(libs.angus.mail)
                implementation(libs.jakarta.activation.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
