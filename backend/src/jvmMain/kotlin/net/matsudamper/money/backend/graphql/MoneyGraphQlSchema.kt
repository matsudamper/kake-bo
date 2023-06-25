package net.matsudamper.money.backend.graphql

import graphql.GraphQL
import graphql.execution.AsyncExecutionStrategy
import graphql.kickstart.tools.SchemaParser
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType
import net.matsudamper.money.backend.graphql.resolver.MutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.QueryResolverImpl
import net.matsudamper.money.backend.graphql.resolver.SettingsMutationResolverResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserMutationResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserResolverImpl
import net.matsudamper.money.backend.graphql.resolver.UserSettingsResolverImpl

object MoneyGraphQlSchema {
    private val schemaFiles = ClassLoader.getSystemClassLoader()
        .getResourceAsStream("graphql")!!
        .bufferedReader().lines()
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

    init {
        println("============schema===============")
        println(schemaFiles.joinToString("\n"))
    }

    private val schema = SchemaParser.newParser()
        .schemaString(schemaFiles.joinToString("\n"))
        .scalars(
            GraphQLScalarType.newScalar(ExtendedScalars.GraphQLLong)
                .name("UserId")
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
        )
        .build()
        .makeExecutableSchema()

    internal val graphql = GraphQL.newGraphQL(schema)
        .queryExecutionStrategy(AsyncExecutionStrategy())
        .build()
}
