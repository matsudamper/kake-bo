package net.matsudamper.money.backend.graphql

import java.util.Locale
import java.util.jar.JarFile
import graphql.GraphQL
import graphql.GraphQLContext
import graphql.Scalars
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.CoercedVariables
import graphql.kickstart.tools.SchemaParser
import graphql.language.Value
import graphql.scalars.ExtendedScalars
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import net.matsudamper.money.backend.graphql.resolver.QueryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserMailAttributesResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserSettingsResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.AdminMutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.MutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.SettingsMutationResolverResolverImpl
import net.matsudamper.money.backend.graphql.resolver.mutation.UserMutationResolverImpl
import net.matsudamper.money.backend.graphql.schema.GraphqlSchemaModule
import net.matsudamper.money.element.MailId


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
                GraphQLScalarType.newScalar()
                    .coercing(
                        @Suppress("UNCHECKED_CAST")
                        ValueClassCoercing(
                            coercing = Scalars.GraphQLString.coercing as Coercing<String, String>,
                            converter = { MailId(it) },
                        ),
                    )
                    .name("MailId")
                    .build(),
                GraphQLScalarType.newScalar(ExtendedScalars.DateTime)
                    .name("DateTime")
                    .build(),
                GraphQLScalarType.newScalar(ExtendedScalars.Date)
                    .name("LocalDate")
                    .build(),
            )
            .resolvers(
                QueryResolverImpl(),
                MutationResolverImpl(),
                AdminMutationResolverImpl(),
                UserMutationResolverImpl(),
                UserResolverImpl(),
                UserSettingsResolverImpl(),
                SettingsMutationResolverResolverImpl(),
                UserMailAttributesResolverImpl(),
            )
            .build()
            .makeExecutableSchema()
    }

    internal val graphql = GraphQL.newGraphQL(schema)
        .queryExecutionStrategy(AsyncExecutionStrategy())
        .build()
}

class ValueClassCoercing<T, R>(
    private val coercing: Coercing<T, T>,
    private val converter: (T) -> R,
) : Coercing<R, R> {
    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): R? {
        return coercing.serialize(dataFetcherResult, graphQLContext, locale)?.let {
            converter(it)
        }
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): R? {
        return coercing.parseValue(input, graphQLContext, locale)?.let {
            converter(it)
        }
    }

    override fun parseLiteral(input: Value<*>, variables: CoercedVariables, graphQLContext: GraphQLContext, locale: Locale): R? {
        return coercing.parseLiteral(input, variables, graphQLContext, locale)?.let {
            converter(it)
        }
    }

    override fun valueToLiteral(input: Any, graphQLContext: GraphQLContext, locale: Locale): Value<*> {
        return coercing.valueToLiteral(input, graphQLContext, locale)
    }
}
