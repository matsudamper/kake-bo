package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.UserConfigRepository
import net.matsudamper.money.graphql.model.QlUserImapConfig
import net.matsudamper.money.graphql.model.QlUserSettings
import net.matsudamper.money.graphql.model.UserSettingsResolver

class UserSettingsResolverImpl : UserSettingsResolver {
    override fun imapConfig(userSettings: QlUserSettings, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUserImapConfig?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val result = UserConfigRepository().getImapConfig(userId) ?: return@supplyAsync null

            QlUserImapConfig(
                host = result.host,
                port = result.port,
                password = result.password,
                userName = result.userName,
            )
        }.toDataFetcher()
    }
}
