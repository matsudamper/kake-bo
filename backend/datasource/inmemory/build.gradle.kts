plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
    }
    jvmToolchain(libs.versions.javaToolchain.get().toInt())
    sourceSets {
        getByName("jvmMain") {
            dependencies {
                implementation(projects.shared)
                implementation(projects.backend.base)
                implementation(projects.backend.app.interfaces)

                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)

                implementation(libs.lettuce)
                implementation(libs.opentelemetryApi)
                implementation(libs.opentelemetryLettuce)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
