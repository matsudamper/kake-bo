
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kobylynskyi.graphqlCodegen)
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
                implementation(projects.backend.base)
                implementation(projects.shared)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(libs.logback.classic)
                implementation(libs.graphqlJava.extendedScalars)
                api(libs.graphqlJavaKickstart.javaTools)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
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
        "UserId" to "net.matsudamper.money.element.UserId",
        "MailId" to "net.matsudamper.money.element.MailId",
        "FidoId" to "net.matsudamper.money.element.FidoId",
        "MoneyUsageCategoryId" to "net.matsudamper.money.element.MoneyUsageCategoryId",
        "MoneyUsageSubCategoryId" to "net.matsudamper.money.element.MoneyUsageSubCategoryId",
        "ImportedMailId" to "net.matsudamper.money.element.ImportedMailId",
        "ImportedMailCategoryFilterConditionId" to "net.matsudamper.money.element.ImportedMailCategoryFilterConditionId",
        "ImportedMailCategoryFilterId" to "net.matsudamper.money.element.ImportedMailCategoryFilterId",
        "Long" to "Long",
        "MoneyUsageId" to "net.matsudamper.money.element.MoneyUsageId",
        "LocalDateTime" to "java.time.LocalDateTime",
        "OffsetDateTime" to "java.time.OffsetDateTime",
    )
}

tasks.withType<KotlinCompile> {
    dependsOn(graphqlCodegen)
}
