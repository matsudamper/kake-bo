plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
    }
    sourceSets {
        getByName("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(projects.shared)
                implementation(projects.backend.base)
            }
        }
    }
}
