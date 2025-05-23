import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.apollographql.apollo3.gradle.internal.ApolloDownloadSchemaTask

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
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)

                api(libs.apolloRuntime)
                implementation(libs.kotlin.datetime)
                api(libs.apolloNormalizedCache)
                implementation(libs.apolloAdapters)
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
        mapScalar("LocalDateTime", "kotlinx.datetime.LocalDateTime", "com.apollographql.apollo3.adapter.KotlinxLocalDateTimeAdapter")
        mapScalar("OffsetDateTime", "kotlinx.datetime.Instant", "com.apollographql.apollo3.adapter.KotlinxInstantAdapter")
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
