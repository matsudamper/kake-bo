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
                implementation(projects.backend.app.interfaces)
                implementation(projects.backend.feature.session)

                implementation(libs.ktorServerCore)
                implementation(libs.kotlin.serialization.json)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
