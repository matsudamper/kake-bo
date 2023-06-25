import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.apollographql.apollo3").version("3.7.5")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.apollographql.apollo3:apollo-runtime:3.7.5")
            }
        }
    }
}

apollo {
    service("money") {
        packageName.set("net.matsudamper.money.frontend.graphql")
        schemaFiles.setFrom(
            file("$rootDir/backend/graphql/src/commonMain/resources/graphql").listFiles(),
        )
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}
