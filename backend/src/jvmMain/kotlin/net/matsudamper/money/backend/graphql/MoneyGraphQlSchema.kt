package net.matsudamper.money.backend.graphql

import java.util.jar.JarFile
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
import net.matsudamper.money.backend.graphql.schema.GraphqlSchemaModule


object MoneyGraphQlSchema {
    private fun getDebugSchemaFiles(): List<String> {
        return ClassLoader.getSystemClassLoader()
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
    }

    internal val graphql = GraphQL.newGraphQL(schema)
        .queryExecutionStrategy(AsyncExecutionStrategy())
        .build()
}
