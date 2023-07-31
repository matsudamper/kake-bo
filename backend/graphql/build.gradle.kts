
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("io.github.kobylynskyi.graphql.codegen") version "5.8.0"
}

val generatedPath = "$buildDir/generated/codegen"
kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(17)
        val jvmMain by getting {
            dependencies {
                api(project(":backend:base"))
                api(project(":shared"))

                api(kotlin("stdlib"))
                api(libs.kotlin.serialization.json)
                api(libs.logback.classic)
                api("com.graphql-java:graphql-java-extended-scalars:2023-01-24T02-11-56-babda5f")

                api("com.graphql-java-kickstart:graphql-java-tools:13.0.2")
            }
        }
        val jvmTest by getting {
            dependencies {
                api(kotlin("test"))
            }
        }
    }
}

sourceSets {
    named("main") {
        kotlin {
            java.setSrcDirs(
                listOf(
                    "src/main/kotlin",
                    generatedPath,
                ).map { File(it) },
            )
        }
    }
}

val graphqlCodegen = tasks.named<io.github.kobylynskyi.graphql.codegen.gradle.GraphQLCodegenGradleTask>("graphqlCodegen") {
    graphqlSchemaPaths = file("$projectDir/src/commonMain/resources/graphql").listFiles().orEmpty()
        .filter { it.extension == "graphqls" }
        .map { it.toString() }
    generatedLanguage = com.kobylynskyi.graphql.codegen.model.GeneratedLanguage.KOTLIN
    outputDir = File(generatedPath)
    packageName = "net.matsudamper.money.graphql.model"
    addGeneratedAnnotation = true
    fieldsWithResolvers = setOf("@lazy")
    generateParameterizedFieldsResolvers = true
    generateBuilder = false
    apiReturnType = "java.util.concurrent.CompletionStage<graphql.execution.DataFetcherResult<{{TYPE}}>>"
    generateDataFetchingEnvironmentArgumentInApis = true
    generateImmutableModels = true
    modelNamePrefix = "Ql"
    generateApisWithThrowsException = false
    parentInterfaces {
        resolver = "graphql.kickstart.tools.GraphQLResolver<{{TYPE}}>"
        mutationResolver = "graphql.kickstart.tools.GraphQLMutationResolver"
        subscriptionResolver = "graphql.kickstart.tools.GraphQLSubscriptionResolver"
        queryResolver = "graphql.kickstart.tools.GraphQLQueryResolver"
    }
    customTypesMapping = mutableMapOf(
        "UserId" to "Int",
        "MailId" to "net.matsudamper.money.element.MailId",
        "MoneyUsageCategoryId" to "net.matsudamper.money.element.MoneyUsageCategoryId",
        "MoneyUsageSubCategoryId" to "net.matsudamper.money.element.MoneyUsageSubCategoryId",
        "ImportedMailId" to "net.matsudamper.money.element.ImportedMailId",
        "MoneyUsageServiceId" to "net.matsudamper.money.element.MoneyUsageServiceId",
        "MoneyUsageId" to "net.matsudamper.money.element.MoneyUsageId",
        "LocalDateTime" to "java.time.LocalDateTime",
        "OffsetDateTime" to "java.time.OffsetDateTime",
    )
}

tasks.withType<KotlinCompile> {
    dependsOn(graphqlCodegen)
}
