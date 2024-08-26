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

                implementation(libs.ktorServerCore)
                implementation(libs.ktorServerEngine)
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
                implementation(libs.ktorServerStatusPages)
                implementation(libs.ktorServerCors)
                implementation(libs.ktorServerDefaultHeaders)
                implementation(libs.ktorServerFowardedHeader)
                implementation(libs.ktorSerializationJson)
                implementation(libs.ktorServerContentNegotiation)
                implementation(libs.ktorServerCallLogging)
                implementation(libs.ktorServerCompression)
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

application {
    mainClass.set("net.matsudamper.money.backend.Main")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
