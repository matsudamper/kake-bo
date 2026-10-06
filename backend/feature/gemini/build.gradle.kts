plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    jvmToolchain(libs.versions.javaToolchain.get().toInt())

    sourceSets {
        jvmMain {
            dependencies {
                implementation(projects.backend.app.interfaces)
                implementation(libs.kotlin.serialization.json)
            }
        }
    }
}
