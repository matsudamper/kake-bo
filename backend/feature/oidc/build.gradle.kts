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
                implementation(libs.ktorServerCore)
                implementation(libs.kotlin.serialization.json)
                api(libs.nimbusJoseJwt)
            }
        }
    }
}
