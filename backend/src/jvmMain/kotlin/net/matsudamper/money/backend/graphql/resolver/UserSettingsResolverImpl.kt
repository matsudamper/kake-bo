package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.UserConfigRepository
import net.matsudamper.money.graphql.model.QlFidoAddInfo
import net.matsudamper.money.graphql.model.QlUser
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
                hasPassword = result.password.isNullOrBlank().not(),
                userName = result.userName,
            )
        }.toDataFetcher()
    }

    override fun fidoAddInfo(userSettings: QlUserSettings, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlFidoAddInfo>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val userNameFuture = context.dataLoaders.userNameDataLoader.get(env)
            .load(userId)
        return CompletableFuture.allOf(userNameFuture).thenApplyAsync {
            QlFidoAddInfo(
                id = userId.value.toString(),
                name = userNameFuture.get(),
                challenge = "test", // TODO challenge
                domain = ServerEnv.domain!!,
            )
        }.toDataFetcher()
    }
}
