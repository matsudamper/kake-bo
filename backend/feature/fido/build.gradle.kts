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
                implementation(projects.shared)
                implementation(projects.backend.base)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.webauth4jCore)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-runner-junit5:5.9.1")
                implementation("io.mockk:mockk:1.13.11")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
