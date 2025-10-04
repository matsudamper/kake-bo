plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
    }
    sourceSets {
        jvmToolchain(25)
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(projects.shared)
                implementation(projects.backend.base)
            }
        }
    }
}
