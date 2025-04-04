plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(21)
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))

                implementation(projects.shared)
                implementation(projects.backend.base)
                implementation(libs.angus.mail)
            }
        }
        val jvmTest by getting {
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
