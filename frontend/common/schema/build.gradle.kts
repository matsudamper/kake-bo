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
                implementation(project(":frontend:common:base"))

                api(libs.apollo.runtime)
                implementation(libs.apollo.normalizedCache)
                implementation(libs.apollo.adapters)
                implementation(libs.kotlin.datetime)
            }
        }
    }
}

apollo {
    service("money") {
        packageName.set("net.matsudamper.money.frontend.graphql")
        mapScalar("MailId", "net.matsudamper.money.element.MailId")
        mapScalar("ImportedMailId", "net.matsudamper.money.element.ImportedMailId")
        mapScalar("MoneyUsageServiceId", "net.matsudamper.money.element.MoneyUsageServiceId")
        mapScalar("MoneyUsageTypeId", "net.matsudamper.money.element.MoneyUsageTypeId")
        mapScalar("LocalDateTime", "kotlinx.datetime.LocalDateTime", "com.apollographql.apollo3.adapter.KotlinxLocalDateTimeAdapter")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}
