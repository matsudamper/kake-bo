import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.apollographql.apollo.gradle.internal.ApolloDownloadSchemaTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.apollo)
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    jvm {}
    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)

                api(libs.apolloRuntime)
                implementation(libs.kotlin.datetime)
                api(libs.apolloNormalizedCache)
                implementation(libs.apolloAdapters)
                implementation(libs.apolloAdaptersCore)
            }
        }
    }
}

apollo {
    service("money") {
        packageName.set("net.matsudamper.money.frontend.graphql")
        mapScalar("MailId", "net.matsudamper.money.element.MailId")
        mapScalar("FidoId", "net.matsudamper.money.element.FidoId")
        mapScalar("ApiTokenId", "net.matsudamper.money.element.ApiTokenId")
        mapScalar("ImportedMailId", "net.matsudamper.money.element.ImportedMailId")
        mapScalar("ImportedMailCategoryFilterId", "net.matsudamper.money.element.ImportedMailCategoryFilterId")
        mapScalar("Long", "kotlin.Long")
        mapScalar("MoneyUsageCategoryId", "net.matsudamper.money.element.MoneyUsageCategoryId")
        mapScalar("MoneyUsageSubCategoryId", "net.matsudamper.money.element.MoneyUsageSubCategoryId")
        mapScalar("ImportedMailCategoryFilterConditionId", "net.matsudamper.money.element.ImportedMailCategoryFilterConditionId")
        mapScalar("MoneyUsageId", "net.matsudamper.money.element.MoneyUsageId")
        mapScalar("LocalDateTime", "kotlinx.datetime.LocalDateTime", "com.apollographql.adapter.datetime.KotlinxLocalDateTimeAdapter")
        mapScalar("OffsetDateTime", "kotlin.time.Instant", "com.apollographql.adapter.core.KotlinInstantAdapter")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}

tasks.register("downloadSchema", ApolloDownloadSchemaTask::class.java) {
    endpoint.set("http://localhost/query")
    outputFile.set(file("src/commonMain/graphql/schema.graphqls"))
    schema.set("money")
}
