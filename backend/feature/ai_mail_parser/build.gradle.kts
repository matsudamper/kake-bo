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
                implementation(libs.kotlin.serialization.json)
                implementation(libs.jsoup)
            }
        }
    }
}
