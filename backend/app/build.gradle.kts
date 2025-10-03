plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
    }
    sourceSets {
        jvmToolchain(21)
        val jvmMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.backend.di)
                implementation(projects.backend.app.interfaces)
                implementation(projects.backend.base)
                implementation(projects.backend.base.mailParser)
                implementation(projects.backend.graphql)
                implementation(projects.backend.feature.serviceMailParser)
                implementation(projects.backend.feature.fido)

                implementation(kotlin("stdlib"))
                implementation(libs.graphqlJava.extendedScalars)
                implementation(libs.graphqlJava)
                implementation(libs.jackson.jsr310)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.logback.classic)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotestRunnerJunit5)
                implementation("io.mockk:mockk:1.14.2")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
