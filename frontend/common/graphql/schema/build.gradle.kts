import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.apollo)
}

kotlin {
    js {
        browser()

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
                implementation(libs.apolloAdaptersCore)
            }
        }
    }
}

apollo {
    service("money") {
        packageName.set("net.matsudamper.money.frontend.graphql")
        mapScalar("UserId", "net.matsudamper.money.element.UserId")
        mapScalar("MailId", "net.matsudamper.money.element.MailId")
        mapScalar("ImageId", "net.matsudamper.money.element.ImageId")
        mapScalar("FidoId", "net.matsudamper.money.element.FidoId")
        mapScalar("ApiTokenId", "net.matsudamper.money.element.ApiTokenId")
        mapScalar("ImportedMailId", "net.matsudamper.money.element.ImportedMailId")
        mapScalar("ImportedMailCategoryFilterId", "net.matsudamper.money.element.ImportedMailCategoryFilterId")
        mapScalar("Long", "kotlin.Long")
        mapScalar("MoneyUsageCategoryId", "net.matsudamper.money.element.MoneyUsageCategoryId")
        mapScalar("MoneyUsageSubCategoryId", "net.matsudamper.money.element.MoneyUsageSubCategoryId")
        mapScalar("ImportedMailCategoryFilterConditionId", "net.matsudamper.money.element.ImportedMailCategoryFilterConditionId")
        mapScalar("MoneyUsageId", "net.matsudamper.money.element.MoneyUsageId")
        mapScalar("MoneyUsagePresetId", "net.matsudamper.money.element.MoneyUsagePresetId")
        mapScalar("SessionRecordId", "net.matsudamper.money.element.SessionRecordId")
        mapScalar("LocalDateTime", "kotlinx.datetime.LocalDateTime", "com.apollographql.adapter.datetime.KotlinxLocalDateTimeAdapter")
        mapScalar("OffsetDateTime", "kotlin.time.Instant", "com.apollographql.adapter.core.KotlinInstantAdapter")
        introspection {
            endpointUrl.set("http://localhost/query")
            schemaFile.set(file("src/commonMain/graphql/schema.graphqls"))
        }
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}
