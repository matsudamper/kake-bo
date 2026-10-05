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
                implementation(projects.shared)
                implementation(projects.backend.base)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.webauth4jCore)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotestRunnerJunit5)
                implementation("io.mockk:mockk:1.14.11")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
