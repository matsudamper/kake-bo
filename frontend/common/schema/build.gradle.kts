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
                api(project(":shared"))
                api(libs.apollo.runtime)
                implementation(libs.apollo.normalizedCache)
            }
        }
    }
}

apollo {
    service("money") {
        packageName.set("net.matsudamper.money.frontend.graphql")
        mapScalar("MailId", "net.matsudamper.money.element.MailId")
        mapScalar("ImportedMailId", "net.matsudamper.money.element.ImportedMailId")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}
