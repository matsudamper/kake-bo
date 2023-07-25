import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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
                implementation(project(":backend:base"))
                implementation(project(":shared"))

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(libs.logback.classic)
                implementation("com.graphql-java:graphql-java-extended-scalars:20.2")

                api("com.graphql-java-kickstart:graphql-java-tools:13.0.2")
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
        "UserId" to "Int",
        "MailId" to "net.matsudamper.money.element.MailId",
        "ImportedMailId" to "net.matsudamper.money.element.ImportedMailId",
        "MoneyUsageServiceId" to "net.matsudamper.money.element.MoneyUsageServiceId",
        "MoneyUsageTypeId" to "net.matsudamper.money.element.MoneyUsageTypeId",
        "DateTime" to "java.time.OffsetDateTime",
        "LocalDate" to "java.time.LocalDateTime",
    )
}

tasks.withType<KotlinCompile> {
    dependsOn(graphqlCodegen)
}
