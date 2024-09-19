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
include(":frontend:common:root")
include(":frontend:common:base")
include(":frontend:common:di")
include(":frontend:common:feature:webauth")
include(":frontend:common:viewmodel")
include(":frontend:common:navigation")
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
            from(files("build-logic/libs.versions.toml"))
        }
    }
}
