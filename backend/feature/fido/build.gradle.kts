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
                implementation(libs.kotestRunnerJunit5)
                implementation("io.mockk:mockk:1.13.17")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
