package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.UserConfigRepository
import net.matsudamper.money.graphql.model.QlSettingsMutation
import net.matsudamper.money.graphql.model.QlUpdateUserImapConfigInput
import net.matsudamper.money.graphql.model.QlUserImapConfig
import net.matsudamper.money.graphql.model.SettingsMutationResolver

class SettingsMutationResolverResolverImpl : SettingsMutationResolver {
    override fun updateImapConfig(
        settingsMutation: QlSettingsMutation,
        config: QlUpdateUserImapConfigInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlUserImapConfig?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        return CompletableFuture.supplyAsync {
            UserConfigRepository().updateImapConfig(
                userId = userId,
                host = config.host,
                port = config.port,
                password = config.password,
                userName = config.userName,
            )
        }.thenApplyAsync { isSuccess ->
            if (isSuccess.not()) {
                return@thenApplyAsync null
            }
            val result = UserConfigRepository().getImapConfig(userId) ?: return@thenApplyAsync null

            QlUserImapConfig(
                host = result.host,
                port = result.port,
                hasPassword = result.password.isNullOrBlank().not(),
                userName = result.userName,
            )
        }.toDataFetcher()
    }
}
