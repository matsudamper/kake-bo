plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain {
            dependencies {
                implementation(projects.backend.app.interfaces)
                implementation(libs.kotlin.serialization.json)
            }
        }
    }
}
