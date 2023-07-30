package net.matsudamper.money.backend.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.Locale
import java.util.jar.JarFile
import graphql.GraphQL
import graphql.GraphQLContext
import graphql.Scalars
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.CoercedVariables
import graphql.kickstart.tools.PerFieldObjectMapperProvider
import graphql.kickstart.tools.SchemaParser
import graphql.kickstart.tools.SchemaParserOptions
import graphql.language.FieldDefinition
import graphql.language.Value
import graphql.scalars.ExtendedScalars
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import net.matsudamper.money.backend.graphql.resolver.ImportedMailAttributesResolverImpl
import net.matsudamper.money.backend.graphql.resolver.ImportedMailResolverImpl
import net.matsudamper.money.backend.graphql.resolver.MoneyUsageCategoryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.MoneyUsageSubCategoryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.QueryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserMailAttributesResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserSettingsResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.AdminMutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.MutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.SettingsMutationResolverResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.UserMutationResolverImpl
import net.matsudamper.money.backend.graphql.schema.GraphqlSchemaModule
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageServiceId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.MoneyUsageTypeId

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
        val schemaFiles = run {
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
                createStringScalarType(
                    name = "MailId",
                    deserialize = { MailId(it) },
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "MoneyUsageServiceId",
                    serialize = { it.id },
                    deserialize = { MoneyUsageServiceId(it) }
                ),
                createIntScalarType(
                    name = "MoneyUsageTypeId",
                    deserialize = { MoneyUsageTypeId(it) },
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
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "MoneyUsageSubCategoryId",
                    deserialize = { MoneyUsageSubCategoryId(it) },
                    serialize = { it.id },
                ),
                createIntScalarType(
                    name = "MoneyUsageId",
                    deserialize = { MoneyUsageId(it) },
                    serialize = { it.id },
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
                MutationResolverImpl(),
                AdminMutationResolverImpl(),
                UserMutationResolverImpl(),
                MoneyUsageSubCategoryResolverImpl(),
                MoneyUsageCategoryResolverImpl(),
                UserResolverImpl(),
                UserSettingsResolverImpl(),
                SettingsMutationResolverResolverImpl(),
                UserMailAttributesResolverImpl(),
                ImportedMailAttributesResolverImpl(),
                ImportedMailResolverImpl(),
            )
            .options(
                SchemaParserOptions.defaultOptions().copy(
                    objectMapperProvider = object : PerFieldObjectMapperProvider {
                        override fun provide(fieldDefinition: FieldDefinition): ObjectMapper {
                            return jacksonObjectMapper()
                        }
                    }
                )
            )
            .build()
            .makeExecutableSchema()
    }

    internal val graphql = GraphQL.newGraphQL(schema)
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

    object LocalDateTimeCoercing : Coercing<LocalDateTime, String> {
        @Suppress("UNCHECKED_CAST")
        private val coercing = Scalars.GraphQLString.coercing as Coercing<String, String>
        override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String? {
            return coercing.serialize(dataFetcherResult, graphQLContext, locale)?.let {
                it
            }
        }

        override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): LocalDateTime? {
            return coercing.parseValue(input, graphQLContext, locale)?.let {
                LocalDateTime.parse(it)
            }
        }

        override fun parseLiteral(
            input: Value<*>,
            variables: CoercedVariables,
            graphQLContext: GraphQLContext,
            locale: Locale
        ): LocalDateTime? {
            return coercing.parseLiteral(input, variables, graphQLContext, locale)?.let {
                LocalDateTime.parse(it)
            }
        }

        override fun valueToLiteral(input: Any, graphQLContext: GraphQLContext, locale: Locale): Value<*> {
            return coercing.valueToLiteral(input, graphQLContext, locale)
        }
    }
}

class ValueClassCoercing<Inner, Outer>(
    private val serialize: (Inner) -> Outer,
    private val deserialize: (Outer) -> Inner,
) : Coercing<Inner, Outer> {
    @Suppress("UNCHECKED_CAST")
    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): Outer? {
        val inner = dataFetcherResult as? Inner
        return inner?.let(serialize)
    }

    @Suppress("UNCHECKED_CAST")
    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): Inner? {
        val outer = input as? Outer
        return outer?.let(deserialize)
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): Inner? {
        throw NotImplementedError("parseLiteral.input -> $input")
    }

    override fun valueToLiteral(input: Any, graphQLContext: GraphQLContext, locale: Locale): Value<*> {
        throw NotImplementedError("valueToLiteral.input -> $input")
    }
}
