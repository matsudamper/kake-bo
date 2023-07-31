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
                api(project(":frontend:common:base"))

                api(libs.apollo.runtime)
                api(libs.kotlin.datetime)
                api(libs.apollo.normalizedCache)
                api(libs.apollo.adapters)
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
        mapScalar("MoneyUsageCategoryId", "net.matsudamper.money.element.MoneyUsageCategoryId")
        mapScalar("MoneyUsageSubCategoryId", "net.matsudamper.money.element.MoneyUsageSubCategoryId")
        mapScalar("MoneyUsageId", "net.matsudamper.money.element.MoneyUsageId")
        mapScalar("LocalDateTime", "kotlinx.datetime.LocalDateTime", "com.apollographql.apollo3.adapter.KotlinxLocalDateTimeAdapter")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}
