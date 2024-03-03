plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(17)
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(projects.shared)
                implementation(projects.backend.base)
            }
        }
    }
}
