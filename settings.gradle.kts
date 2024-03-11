@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "money"
include(":backend")
include(":backend:app")
include(":backend:app:interfaces")
include(":backend:base")
include(":backend:di")
include(":backend:base:mail_parser")
include(":backend:graphql")
include(":backend:datasource:db:schema")
include(":backend:datasource:mail")
include(":backend:datasource:inmemory")
include(":backend:feature:service_mail_parser")
include(":backend:feature:fido")

include(":frontend:jsApp")
include(":frontend:common:ui")
include(":frontend:common:base")
include(":frontend:common:viewmodel")
include(":frontend:common:graphql:schema")
include(":frontend:common:graphql")

include(":shared")

pluginManagement {
    includeBuild("build-logic")
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
            library("kotlin.coroutines.core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            library("kotlin.datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            library("kotlin.serialization.json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

            plugin("compose", "org.jetbrains.compose").version(extra["compose.version"] as String)
            library("compose.material3", "org.jetbrains.compose.material3:material3:1.6.10-dev1464")

            library("graphqlJava.extendedScalars", "com.graphql-java:graphql-java-extended-scalars:2023-01-24T02-11-56-babda5f")
            library("graphqlJava", "com.graphql-java:graphql-java:21.3")
            library("graphqlJavaKickstart.javaTools", "com.graphql-java-kickstart:graphql-java-tools:13.1.1")
            plugin("kobylynskyi.graphqlCodegen", "io.github.kobylynskyi.graphql.codegen").version("5.10.0")

            library("log4j.api", "org.slf4j:slf4j-api:2.0.12")
            library("logback.classic", "ch.qos.logback:logback-classic:1.5.3")

            library("webauth4jCore", "com.webauthn4j:webauthn4j-core:0.22.2.RELEASE")

            plugin("ktlint", "org.jlleitschuh.gradle.ktlint").version("12.1.0")

            val ktorVersion = "2.3.8"
            library("ktor.server.core", "io.ktor:ktor-server-core:$ktorVersion")
            library("ktor.server.engine", "io.ktor:ktor-server-cio:$ktorVersion")
            library("ktor.server.statusPages", "io.ktor:ktor-server-status-pages:$ktorVersion")
            library("ktor.server.cors", "io.ktor:ktor-server-cors:$ktorVersion")
            library("ktor.server.defaultHeaders", "io.ktor:ktor-server-default-headers:$ktorVersion")
            library("ktor.server.fowardedHeader", "io.ktor:ktor-server-forwarded-header:$ktorVersion")
            library("ktor.server.compression", "io.ktor:ktor-server-compression:$ktorVersion")
            library("ktor.serialization.json", "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            library("ktor.server.contentNegotiation", "io.ktor:ktor-server-content-negotiation:$ktorVersion")
            library("ktor.server.callLogging", "io.ktor:ktor-server-call-logging:$ktorVersion")
            library("ktor.client.core", "io.ktor:ktor-client-core:$ktorVersion")
            library("ktor.client.cio", "io.ktor:ktor-client-cio:$ktorVersion")
            library("ktor.client.js", "io.ktor:ktor-client-js:$ktorVersion")
            library("ktor.client.logging", "io.ktor:ktor-client-logging:$ktorVersion")

            library("jakarta.mail.api", "jakarta.mail:jakarta.mail-api:2.1.2")
            library("jakarta.mail", "org.eclipse.angus:jakarta.mail:2.0.2")
            library("angus.mail", "org.eclipse.angus:angus-mail:2.0.2")
            library("jakarta.activation.api", "jakarta.activation:jakarta.activation-api:2.1.3")

            val apolloVersion = "3.8.2"
            plugin("apollo.plugin", "com.apollographql.apollo3").version(apolloVersion)
            library("apollo.runtime", "com.apollographql.apollo3:apollo-runtime:$apolloVersion")
            library("apollo.normalizedCache", "com.apollographql.apollo3:apollo-normalized-cache:$apolloVersion")
            library("apollo.adapters", "com.apollographql.apollo3:apollo-adapters:$apolloVersion")

            val jacksonVersion = "2.16.1"
            library("jackson.databind", "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
            library("jackson.kotlin", "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
            library("jackson.jsr310", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

            library("jedis", "redis.clients:jedis:5.1.1")
            library("jsoup", "org.jsoup:jsoup:1.17.2")
        }
    }
}
