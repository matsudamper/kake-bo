plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(projects.shared)
                implementation(projects.backend.base)
                implementation(projects.backend.app.interfaces)
                implementation(projects.backend.di)

                implementation(libs.ktorServerCore)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
