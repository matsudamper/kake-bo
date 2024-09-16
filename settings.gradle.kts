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
include(":frontend:androidApp")
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
        val kotlinVersion = extra["kotlin.version"] as String
        val agpVersion = extra["agp.version"] as String
        kotlin("multiplatform").version(kotlinVersion)
        id("org.jetbrains.kotlin.plugin.compose").version(kotlinVersion)
        kotlin("jvm").version(kotlinVersion)
        kotlin("android").version(kotlinVersion)
        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)
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
            val composeVersion = extra["compose.version"] as String
            plugin("kotlin.multiplatform", "org.jetbrains.kotlin.multiplatform").version(kotlinVersion)
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").version(kotlinVersion)
            library("kotlin.coroutines.core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            library("kotlin.datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            library("kotlin.serialization.json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")

            plugin("composeCompiler", "org.jetbrains.kotlin.plugin.compose").version(kotlinVersion)
            plugin("jetbrainsCompose", "org.jetbrains.compose").version(composeVersion)
            library("compose.material3", "org.jetbrains.compose.material3:material3:$composeVersion")

            library("graphqlJava.extendedScalars", "com.graphql-java:graphql-java-extended-scalars:2023-01-24T02-11-56-babda5f")
            library("graphqlJava", "com.graphql-java:graphql-java:22.3")
            library("graphqlJavaKickstart.javaTools", "com.graphql-java-kickstart:graphql-java-tools:13.1.1")
            plugin("kobylynskyi.graphqlCodegen", "io.github.kobylynskyi.graphql.codegen").version("5.10.0")

            library("log4j.api", "org.slf4j:slf4j-api:2.0.16")
            library("logback.classic", "ch.qos.logback:logback-classic:1.5.8")

            library("webauth4jCore", "com.webauthn4j:webauthn4j-core:0.26.0.RELEASE")

            plugin("ktlint", "org.jlleitschuh.gradle.ktlint").version("12.1.1")

            library("jakarta.mail.api", "jakarta.mail:jakarta.mail-api:2.1.3")
            library("jakarta.mail", "org.eclipse.angus:jakarta.mail:2.0.3")
            library("angus.mail", "org.eclipse.angus:angus-mail:2.0.3")
            library("jakarta.activation.api", "jakarta.activation:jakarta.activation-api:2.1.3")

            val jacksonVersion = "2.17.2"
            library("jackson.databind", "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
            library("jackson.kotlin", "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
            library("jackson.jsr310", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

            library("jedis", "redis.clients:jedis:5.1.5")
            library("jsoup", "org.jsoup:jsoup:1.18.1")

            from(files("build-logic/libs.versions.toml"))
        }
    }
}
