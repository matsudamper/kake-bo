package net.matsudamper.money.backend.graphql

import java.time.LocalDateTime
import java.util.Locale
import java.util.jar.JarFile
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import graphql.GraphQL
import graphql.GraphQLContext
import graphql.Scalars
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.CoercedVariables
import graphql.kickstart.tools.PerFieldConfiguringObjectMapperProvider
import graphql.kickstart.tools.SchemaParser
import graphql.kickstart.tools.SchemaParserOptions
import graphql.language.Value
import graphql.scalars.ExtendedScalars
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.graphql.resolver.MoneyUsageCategoryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.MoneyUsageResolverImpl
import net.matsudamper.money.backend.graphql.resolver.MoneyUsageSubCategoryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.MoneyUsageSuggestResolverImpl
import net.matsudamper.money.backend.graphql.resolver.QueryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.SessionAttributesResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserMailAttributesResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserSettingsResolverImpl
import net.matsudamper.money.backend.graphql.resolver.analytics.MoneyUsageAnalyticsByCategoryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.analytics.MoneyUsageAnalyticsResolverImpl
import net.matsudamper.money.backend.graphql.resolver.importedmail.ImportedMailAttributesResolverImpl
import net.matsudamper.money.backend.graphql.resolver.importedmail.ImportedMailCategoryConditionResolverImpl
import net.matsudamper.money.backend.graphql.resolver.importedmail.ImportedMailCategoryFilterResolverImpl
import net.matsudamper.money.backend.graphql.resolver.importedmail.ImportedMailResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.AdminMutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.MutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.SettingsMutationResolverResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.UserMutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.setting.ApiTokenAttributesResolverImpl
import net.matsudamper.money.backend.graphql.resolver.user.UserResolverImpl
import net.matsudamper.money.backend.graphql.schema.GraphqlSchemaModule
import net.matsudamper.money.element.FidoId
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId

object MoneyGraphQlSchema {
    private fun getDebugSchemaFiles(): List<String> {
        return ClassLoader.getSystemClassLoader()
            .getResourceAsStream("graphql")!!
            .bufferedReader().lines()
            .filter { it.endsWith(".graphqls") }
            .toList()
            .onEach {
                println("lines -> $it")
            }
            .map {
                ClassLoader.getSystemClassLoader()
                    .getResourceAsStream("graphql/$it")!!
                    .bufferedReader()
                    .readText()
            }
    }

    private fun getSchemaFiles(): List<String> {
        return runCatching {
            val currentJarUri = GraphqlSchemaModule::class.java.protectionDomain.codeSource.location.file
            val jarFIle = JarFile(currentJarUri)
            JarFile(currentJarUri)
                .entries()
                .toList()
                .filter { it.isDirectory.not() }
                .filter { it.name.startsWith("graphql/") }
                .filter { it.name.endsWith(".graphqls") }
                .map {
                    jarFIle.getInputStream(it).bufferedReader().readText()
                }
        }.getOrNull().orEmpty()
    }

    private val schema by lazy {
        val schemaFiles =
            run {
                getSchemaFiles().takeIf { it.isNotEmpty() }
                    ?: getDebugSchemaFiles()
            }
        println("==========schema==========")
        schemaFiles.forEach {
            println(it)
        }
        SchemaParser.newParser()
            .schemaString(schemaFiles.joinToString("\n"))
            .scalars(
                GraphQLScalarType.newScalar(ExtendedScalars.GraphQLLong)
                    .name("UserId")
                    .build(),
                GraphQLScalarType.newScalar(ExtendedScalars.GraphQLLong)
                    .name("Long")
                    .build(),
                createStringScalarType(
                    name = "MailId",
                    deserialize = { MailId(it) },
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "ImportedMailCategoryFilterId",
                    deserialize = { ImportedMailCategoryFilterId(it) },
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "ImportedMailId",
                    deserialize = { ImportedMailId(it) },
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "MoneyUsageCategoryId",
                    deserialize = { MoneyUsageCategoryId(it) },
                    serialize = { it.value },
                ),
                createIntScalarType(
                    name = "MoneyUsageSubCategoryId",
                    deserialize = { MoneyUsageSubCategoryId(it) },
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "ImportedMailCategoryFilterConditionId",
                    deserialize = { ImportedMailCategoryFilterConditionId(it) },
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "MoneyUsageId",
                    deserialize = { MoneyUsageId(it) },
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "FidoId",
                    deserialize = { FidoId(it) },
                    serialize = { it.value },
                ),
                GraphQLScalarType.newScalar(ExtendedScalars.DateTime)
                    .name("OffsetDateTime")
                    .build(),
                GraphQLScalarType.newScalar()
                    .coercing(LocalDateTimeCoercing)
                    .name("LocalDateTime")
                    .build(),
            )
            .resolvers(
                QueryResolverImpl(),
                ImportedMailCategoryConditionResolverImpl(),
                MutationResolverImpl(),
                AdminMutationResolverImpl(),
                UserMutationResolverImpl(),
                MoneyUsageSubCategoryResolverImpl(),
                MoneyUsageCategoryResolverImpl(),
                MoneyUsageResolverImpl(),
                UserResolverImpl(),
                MoneyUsageAnalyticsResolverImpl(),
                MoneyUsageAnalyticsByCategoryResolverImpl(),
                ImportedMailCategoryFilterResolverImpl(),
                UserSettingsResolverImpl(),
                SettingsMutationResolverResolverImpl(),
                UserMailAttributesResolverImpl(),
                ImportedMailAttributesResolverImpl(),
                ImportedMailResolverImpl(),
                MoneyUsageSuggestResolverImpl(),
                SessionAttributesResolverImpl(),
                ApiTokenAttributesResolverImpl(),
            )
            .options(
                @Suppress("OPT_IN_USAGE")
                SchemaParserOptions.defaultOptions().copy(
                    objectMapperProvider =
                    PerFieldConfiguringObjectMapperProvider { mapper, _ ->
                        mapper.registerModule(
                            JavaTimeModule(),
                        )
                    },
                    fieldVisibility =
                    if (ServerEnv.isDebug) {
                        null
                    } else {
                        NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY
                    },
                ),
            )
            .build()
            .makeExecutableSchema()
    }

    public val graphql =
        GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(AsyncExecutionStrategy())
            .build()

    private fun <T> createStringScalarType(
        name: String,
        serialize: (T) -> String,
        deserialize: (String) -> T,
    ): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .coercing(
                ValueClassCoercing(
                    serialize = serialize,
                    deserialize = deserialize,
                ),
            )
            .name(name)
            .build()
    }

    private fun <T> createIntScalarType(
        name: String,
        serialize: (T) -> Int,
        deserialize: (Int) -> T,
    ): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .coercing(
                ValueClassCoercing(
                    serialize = serialize,
                    deserialize = deserialize,
                ),
            )
            .name(name)
            .build()
    }

    private fun <T> createLongScalarType(
        name: String,
        serialize: (T) -> Long,
        deserialize: (Long) -> T,
    ): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .coercing(
                ValueClassCoercing(
                    serialize = serialize,
                    deserialize = deserialize,
                ),
            )
            .name(name)
            .build()
    }

    object LocalDateTimeCoercing : Coercing<LocalDateTime, String> {
        @Suppress("UNCHECKED_CAST")
        private val coercing = Scalars.GraphQLString.coercing as Coercing<String, String>

        override fun serialize(
            dataFetcherResult: Any,
            graphQLContext: GraphQLContext,
            locale: Locale,
        ): String? {
            return coercing.serialize(dataFetcherResult, graphQLContext, locale)?.let {
                it
            }
        }

        override fun parseValue(
            input: Any,
            graphQLContext: GraphQLContext,
            locale: Locale,
        ): LocalDateTime? {
            return coercing.parseValue(input, graphQLContext, locale)?.let {
                LocalDateTime.parse(it)
            }
        }

        override fun parseLiteral(
            input: Value<*>,
            variables: CoercedVariables,
            graphQLContext: GraphQLContext,
            locale: Locale,
        ): LocalDateTime? {
            return coercing.parseLiteral(input, variables, graphQLContext, locale)?.let {
                LocalDateTime.parse(it)
            }
        }

        override fun valueToLiteral(
            input: Any,
            graphQLContext: GraphQLContext,
            locale: Locale,
        ): Value<*> {
            return coercing.valueToLiteral(input, graphQLContext, locale)
        }
    }
}

class ValueClassCoercing<Inner, Outer>(
    private val serialize: (Inner) -> Outer,
    private val deserialize: (Outer) -> Inner,
) : Coercing<Inner, Outer> {
    @Suppress("UNCHECKED_CAST")
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): Outer? {
        val inner = dataFetcherResult as? Inner
        return inner?.let(serialize)
    }

    @Suppress("UNCHECKED_CAST")
    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): Inner? {
        val outer = input as? Outer
        return outer?.let(deserialize)
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): Inner? {
        throw NotImplementedError("parseLiteral.input -> $input")
    }

    override fun valueToLiteral(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): Value<*> {
        throw NotImplementedError("valueToLiteral.input -> $input")
    }
}
