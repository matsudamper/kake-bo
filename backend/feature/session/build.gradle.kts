plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        getByName("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(projects.shared)
                implementation(projects.backend.base)
                implementation(projects.backend.app.interfaces)
                implementation(projects.backend.di)

                implementation(libs.ktorServerCore)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
