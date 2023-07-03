import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.apollo.plugin)
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.apollo.runtime)
                implementation(libs.apollo.normalizedCache)
            }
        }
    }
}

apollo {
    service("money") {
        packageName.set("net.matsudamper.money.frontend.graphql")
        schemaFiles.setFrom(
            file("$rootDir/backend/graphql/src/commonMain/resources/graphql")
                .listFiles()
                .orEmpty()
                .filter { it.endsWith(".graphqls") },
        )
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}
