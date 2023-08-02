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
include(":frontend:common:schema")
include(":backend:db:schema")
include(":backend:mail")
include(":backend:mail_parser")

include(":shared")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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
            library("kotlin.coroutines.core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            library("kotlin.datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            library("kotlin.serialization.json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

            plugin("compose", "org.jetbrains.compose").version(extra["compose.version"] as String)
            library("compose.material3", "org.jetbrains.compose.material3:material3:1.4.0")

            library("graphqlJava.extendedScalars", "com.graphql-java:graphql-java-extended-scalars:20.2")
            library("graphqlJavaKickstart.javaTools", "com.graphql-java-kickstart:graphql-java-tools:13.0.2")
            plugin("kobylynskyi.graphqlCodegen", "io.github.kobylynskyi.graphql.codegen").version("5.8.0")

            library("log4j.api", "org.slf4j:slf4j-api:2.0.6")
            library("logback.classic", "ch.qos.logback:logback-classic:1.4.8")

            plugin("ktlint", "org.jlleitschuh.gradle.ktlint").version("11.3.1")

            val ktorVersion = "2.3.2"
            library("ktor.server.core", "io.ktor:ktor-server-core:$ktorVersion")
            library("ktor.server.engine", "io.ktor:ktor-server-cio:$ktorVersion")
            library("ktor.server.statusPages", "io.ktor:ktor-server-status-pages:$ktorVersion")
            library("ktor.server.defaultHeaders", "io.ktor:ktor-server-default-headers:$ktorVersion")
            library("ktor.server.fowardedHeader", "io.ktor:ktor-server-forwarded-header:$ktorVersion")
            library("ktor.server.compression", "io.ktor:ktor-server-compression:$ktorVersion")
            library("ktor.serialization.json", "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            library("ktor.server.contentNegotiation", "io.ktor:ktor-server-content-negotiation:$ktorVersion")
            library("ktor.server.callLogging", "io.ktor:ktor-server-call-logging:$ktorVersion")
            library("ktor.client.core", "io.ktor:ktor-client-core:$ktorVersion")
            library("ktor.client.cio", "io.ktor:ktor-client-cio:$ktorVersion")
            library("ktor.client.js", "io.ktor:ktor-client-js:$ktorVersion")

            library("jakarta.mail.api", "jakarta.mail:jakarta.mail-api:2.1.2")
            library("jakarta.mail", "org.eclipse.angus:jakarta.mail:2.0.2")
            library("angus.mail", "org.eclipse.angus:angus-mail:2.0.2")
            library("jakarta.activation.api", "jakarta.activation:jakarta.activation-api:2.1.2")

            val apolloVersion = "3.8.2"
            plugin("apollo.plugin", "com.apollographql.apollo3").version(apolloVersion)
            library("apollo.runtime", "com.apollographql.apollo3:apollo-runtime:$apolloVersion")
            library("apollo.normalizedCache", "com.apollographql.apollo3:apollo-normalized-cache:$apolloVersion")
            library("apollo.adapters", "com.apollographql.apollo3:apollo-adapters:$apolloVersion")

            val jacksonVersion = "2.15.2"
            library("jackson.databind", "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
            library("jackson.kotlin", "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
            library("jackson.jsr310", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

            library("jsoup", "org.jsoup:jsoup:1.16.1")
        }
    }
}
