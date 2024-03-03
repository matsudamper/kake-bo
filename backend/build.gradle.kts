plugins {
    id("application")
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

base.archivesName.set("money")
group = "net.matsudamper.money.backend"
dependencies {
    implementation(project(":backend:di"))
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
                implementation(projects.backend.graphql)
                implementation(projects.backend.app)

                implementation(kotlin("stdlib"))
                implementation(libs.jackson.jsr310)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.logback.classic)

                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.engine)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.server.statusPages)
                implementation(libs.ktor.server.cors)
                implementation(libs.ktor.server.defaultHeaders)
                implementation(libs.ktor.server.fowardedHeader)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.server.contentNegotiation)
                implementation(libs.ktor.server.callLogging)
                implementation(libs.ktor.server.compression)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-runner-junit5:5.8.0")
                implementation("io.mockk:mockk:1.13.10")
            }
        }
    }
}

application {
    mainClass.set("net.matsudamper.money.backend.Main")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
