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
                api(project(":shared"))
                api(project(":backend:base"))
                api(project(":backend:graphql"))
                api(project(":backend:db"))
                api(project(":backend:mail"))
                api(project(":backend:mail_parser"))

                api(kotlin("stdlib"))
                api("com.graphql-java:graphql-java-extended-scalars:20.2")
                api(libs.kotlin.serialization.json)
                api(libs.kotlin.coroutines.core)
                api(libs.logback.classic)

                api(libs.ktor.server.core)
                api(libs.ktor.server.engine)
                api(libs.ktor.client.core)
                api(libs.ktor.client.cio)
                api(libs.ktor.server.statusPages)
                api(libs.ktor.server.defaultHeaders)
                api(libs.ktor.server.fowardedHeader)
                api(libs.ktor.serialization.json)
                api(libs.ktor.server.contentNegotiation)
                api(libs.ktor.server.callLogging)
            }
        }
        val jvmTest by getting {
            dependencies {
                api(kotlin("test"))
                api("io.kotest:kotest-runner-junit5:5.6.2")
                api("io.mockk:mockk:1.13.5")
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
