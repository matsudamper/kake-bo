plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    wasmJs {
        browser()
        nodejs()
    }
    jvm {
    }
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(projects.shared)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotestRunnerJunit5)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
