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
include(":backend:feature:ai_mail_parser")
include(":backend:feature:gemini")
include(":backend:feature:fido")
include(":backend:feature:image")
include(":backend:feature:session")
include(":backend:feature:oidc")
include(":backend:feature:object-storage")
include(":backend:feature:image-storage-local")

include(":frontend:app")
include(":frontend:android:app")
include(":frontend:feature-notification")
include(":frontend:feature-admin")
include(":frontend:android:feature-notification-usage")
include(":frontend:common:ui")
include(":frontend:common:root")
include(":frontend:common:base")
include(":frontend:common:di")
include(":frontend:common:feature:webauth")
include(":frontend:common:feature:localstore")
include(":frontend:common:feature:uploader")
include(":frontend:common:feature:logging")
include(":frontend:common:viewmodel")
include(":frontend:common:usecase")
include(":frontend:common:navigation")
include(":frontend:common:graphql:schema")
include(":frontend:common:graphql:apollo-compiler-plugin")
include(":frontend:common:graphql")

include(":shared")
include(":shared:feature-category-filter")
include(":detekt-rules")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://maven.matsudamper.net/releases") {
            content {
                includeGroupAndSubgroups("net.matsudamper")
            }
        }
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
