plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        getByName("jvmMain") {
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
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}
