plugins {
    id("application")
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

base.archivesName.set("money")
group = "net.matsudamper.money.backend"

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(17)
        val jvmMain by getting {
            dependencies {
                implementation(project(":backend:base"))
                implementation(project(":backend:graphql"))
                implementation(project(":backend:db"))
                implementation(project(":backend:mail"))

                implementation(kotlin("stdlib"))
                implementation("com.graphql-java:graphql-java-extended-scalars:20.2")
                implementation(libs.kotlin.serialization.json)
                implementation(libs.logback.classic)

                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.engine)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.server.statusPages)
                implementation(libs.ktor.server.defaultHeaders)
                implementation(libs.ktor.server.fowardedHeader)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.server.contentNegotiation)
                implementation(libs.ktor.server.callLogging)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-runner-junit5:5.6.2")
                implementation("io.mockk:mockk:1.13.5")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
