@file:Suppress("UnstableApiUsage")

rootProject.name = "money"
include(":backend:db")
include(":backend:base")
include(":backend:graphql")
include(":backend")

include(":frontend:jsApp")
include(":frontend:common:ui")
include(":frontend:common:base")
include(":frontend:common:viewmodel")
include(":frontend:common:uistate")
include(":frontend:common:schema")
include(":backend:db:schema")
include(":backend:mail")

include(":shared")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenLocal()
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    @Suppress("UnstableApiUsage")
    versionCatalogs {
        create("libs") {
            val kotlinVersion = extra["kotlin.version"] as String
            plugin("kotlin.multiplatform", "org.jetbrains.kotlin.multiplatform").version(kotlinVersion)
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").version(kotlinVersion)
            library("kotlin.coroutines.core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")

            plugin("compose", "org.jetbrains.compose").version(extra["compose.version"] as String)
            library("compose.material3", "org.jetbrains.compose.material3:material3:1.4.0")


            library("kotlin.serialization.json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

            library("log4j.api", "org.slf4j:slf4j-api:2.0.6")
            library("logback.classic", "ch.qos.logback:logback-classic:1.4.8")

            plugin("ktlint", "org.jlleitschuh.gradle.ktlint").version("11.3.1")

            val ktorVersion = "2.2.4"
            library("ktor.server.core", "io.ktor:ktor-server-core:$ktorVersion")
            library("ktor.server.engine", "io.ktor:ktor-server-cio:$ktorVersion")
            library("ktor.server.statusPages", "io.ktor:ktor-server-status-pages:$ktorVersion")
            library("ktor.server.defaultHeaders", "io.ktor:ktor-server-default-headers:$ktorVersion")
            library("ktor.server.fowardedHeader", "io.ktor:ktor-server-forwarded-header:$ktorVersion")
            library("ktor.serialization.json", "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            library("ktor.server.contentNegotiation", "io.ktor:ktor-server-content-negotiation:$ktorVersion")
            library("ktor.server.callLogging", "io.ktor:ktor-server-call-logging:$ktorVersion")
            library("ktor.client.core", "io.ktor:ktor-client-core:$ktorVersion")
            library("ktor.client.cio", "io.ktor:ktor-client-cio:$ktorVersion")
            library("ktor.client.js", "io.ktor:ktor-client-js:$ktorVersion")

            val apolloVersion = "3.8.2"
            plugin("apollo.plugin", "com.apollographql.apollo3").version(apolloVersion)
            library("apollo.runtime", "com.apollographql.apollo3:apollo-runtime:$apolloVersion")
            library("apollo.normalizedCache", "com.apollographql.apollo3:apollo-normalized-cache:$apolloVersion")

            val jacksonVersion = "2.15.2"
            library("jackson.databind","com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
            library("jackson.kotlin","com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        }
    }
}