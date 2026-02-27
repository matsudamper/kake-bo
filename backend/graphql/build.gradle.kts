import com.kobylynskyi.graphql.codegen.model.GeneratedLanguage
import io.github.kobylynskyi.graphql.codegen.gradle.GraphQLCodegenGradleTask
import io.github.kobylynskyi.graphql.codegen.gradle.NullableInputTypeWrapperConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kobylynskyi.graphqlCodegen)
}

val generatedPath: String = layout.buildDirectory.dir("generated/codegen").get().asFile.path
kotlin {
    jvm()
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val jvmMain by getting {
            kotlin.srcDir(generatedPath)
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

val graphqlCodegen = tasks.named<GraphQLCodegenGradleTask>("graphqlCodegen") {
    graphqlSchemaPaths = file("$projectDir/src/commonMain/resources/graphql").listFiles().orEmpty()
        .filter { it.extension == "graphqls" }
        .map { it.toString() }
    generatedLanguage = GeneratedLanguage.KOTLIN
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
    nullableInputTypeWrapperForDirectives = setOf("optional")
    kotlinNullableInputTypeWrapper {
        wrapperClassName = "net.matsudamper.money.graphql.model.GraphQlInputField"
        nullValueExpression = "net.matsudamper.money.graphql.model.GraphQlInputField.Defined(null)"
        undefinedValueExpression = "net.matsudamper.money.graphql.model.GraphQlInputField.Undefined"
        valueExpression = "net.matsudamper.money.graphql.model.GraphQlInputField.Defined(%s)"
    }
    customTypesMapping = mutableMapOf(
        "UserId" to "net.matsudamper.money.element.UserId",
        "ImageId" to "net.matsudamper.money.element.ImageId",
        "MailId" to "net.matsudamper.money.element.MailId",
        "FidoId" to "net.matsudamper.money.element.FidoId",
        "ApiTokenId" to "net.matsudamper.money.element.ApiTokenId",
        "MoneyUsageCategoryId" to "net.matsudamper.money.element.MoneyUsageCategoryId",
        "MoneyUsageSubCategoryId" to "net.matsudamper.money.element.MoneyUsageSubCategoryId",
        "ImportedMailId" to "net.matsudamper.money.element.ImportedMailId",
        "ImportedMailCategoryFilterConditionId" to "net.matsudamper.money.element.ImportedMailCategoryFilterConditionId",
        "ImportedMailCategoryFilterId" to "net.matsudamper.money.element.ImportedMailCategoryFilterId",
        "Long" to "Long",
        "MoneyUsageId" to "net.matsudamper.money.element.MoneyUsageId",
        "MoneyUsagePresetId" to "net.matsudamper.money.element.MoneyUsagePresetId",
        "LocalDateTime" to "java.time.LocalDateTime",
        "OffsetDateTime" to "java.time.OffsetDateTime",
    )
}

tasks.withType<KotlinCompile> {
    dependsOn(graphqlCodegen)
}
