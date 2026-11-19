@file:Suppress("UnstableApiUsage")

import com.github.burrunan.s3cache.AwsS3BuildCache

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
include(":frontend:common:screenshot")
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
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.github.burrunan.s3-build-cache") version "1.9.11"
}

val remoteBuildCacheAccessKeyId: String? = System.getenv("S3_BUILD_CACHE_ACCESS_KEY_ID")
buildCache {
    remote<AwsS3BuildCache> {
        isEnabled = !remoteBuildCacheAccessKeyId.isNullOrBlank()
        isPush = System.getenv("CI") == "true"
        endpoint = System.getenv("S3_BUILD_CACHE_ENDPOINT").orEmpty().ifBlank { "s3.matsudamper.net" }
        forcePathStyle = true
        region = "us-east-1"
        bucket = "gradle-remote-cache"
        prefix = "matsudamper/kake-bo/"
        // 自前の S3 互換サーバーなので AWS 固有のストレージクラスを指定しない
        isReducedRedundancy = false
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}
