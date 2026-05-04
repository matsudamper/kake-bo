plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain {
            dependencies {
                implementation(projects.shared)
                implementation(projects.backend.base)
                implementation(projects.backend.app.interfaces)
                implementation(projects.backend.feature.image)
                implementation(libs.kotlin.coroutines.core)
            }
        }
    }
}
