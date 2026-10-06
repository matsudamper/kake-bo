import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.apollo)
}

kotlin {
    wasmJs {
        browser()

    }
    jvm {}
    sourceSets {
        getByName("commonMain") {
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
        mapScalar("ImportedMailCategoryFilterMatcherId", "net.matsudamper.money.element.ImportedMailCategoryFilterMatcherId")
        mapScalar("MoneyUsageId", "net.matsudamper.money.element.MoneyUsageId")
        mapScalar("MoneyUsagePresetId", "net.matsudamper.money.element.MoneyUsagePresetId")
        mapScalar("SessionRecordId", "net.matsudamper.money.element.SessionRecordId")
        mapScalar("LocalDateTime", "kotlinx.datetime.LocalDateTime", "com.apollographql.adapter.datetime.KotlinxLocalDateTimeAdapter")
        mapScalar("OffsetDateTime", "kotlin.time.Instant", "com.apollographql.adapter.core.KotlinInstantAdapter")
        // 適用済みのディレクティブを使うため、イントロスペクションではなくバックエンドのスキーマを直接参照する
        schemaFiles.from(
            rootProject.layout.projectDirectory.dir("backend/graphql/src/commonMain/resources/graphql")
                .asFileTree
                .matching { include("*.graphqls") },
            file("src/commonMain/graphql/extra.graphqls"),
        )
        plugin(project(":frontend:common:graphql:apollo-compiler-plugin"))
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateApolloSources")
}
