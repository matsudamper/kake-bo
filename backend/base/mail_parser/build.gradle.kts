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
                implementation(kotlin("reflect"))

                implementation(projects.shared)
                implementation(projects.backend.base)
                implementation(libs.angus.mail)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}

// tasks.test {
//    useJUnitPlatform()
// }
