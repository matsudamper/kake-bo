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
                implementation(project(":shared"))
                implementation(project(":frontend:common:base"))

                implementation(libs.apollo.runtime)
                implementation(libs.kotlin.datetime)
                implementation(libs.apollo.normalizedCache)
                implementation(libs.apollo.adapters)
            }
        }
    }
}

apollo {
    service("money") {
        packageName.set("net.matsudamper.money.frontend.graphql")
        mapScalar("MailId", "net.matsudamper.money.element.MailId")
        mapScalar("ImportedMailId", "net.matsudamper.money.element.ImportedMailId")
        mapScalar("ImportedMailCategoryFilterId", "net.matsudamper.money.element.ImportedMailCategoryFilterId")
        mapScalar("Long", "Long")
        mapScalar("MoneyUsageCategoryId", "net.matsudamper.money.element.MoneyUsageCategoryId")
        mapScalar("MoneyUsageSubCategoryId", "net.matsudamper.money.element.MoneyUsageSubCategoryId")
        mapScalar("ImportedMailCategoryFilterConditionId", "net.matsudamper.money.element.ImportedMailCategoryFilterConditionId")
        mapScalar("MoneyUsageId", "net.matsudamper.money.element.MoneyUsageId")
        mapScalar("LocalDateTime", "kotlinx.datetime.LocalDateTime", "com.apollographql.apollo3.adapter.KotlinxLocalDateTimeAdapter")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}
