plugins {
    alias(libs.plugins.kotlin.multiplatform)
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

                implementation(projects.backend.base)
                implementation(projects.backend.app.interfaces)
                implementation(projects.backend.datasource.db)
                implementation(projects.backend.datasource.mail)
                implementation(projects.backend.datasource.inmemory)
            }
        }
    }
}
