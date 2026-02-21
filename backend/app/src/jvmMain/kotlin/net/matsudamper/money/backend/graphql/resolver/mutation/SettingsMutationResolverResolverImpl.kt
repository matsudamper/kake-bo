package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.resolver.getOrNull
import net.matsudamper.money.backend.graphql.toDataFetcher
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
        val userConfigRepository = context.diContainer.createUserConfigRepository()
        val userId = context.verifyUserSessionAndGetUserId()
        return CompletableFuture.supplyAsync {
            userConfigRepository.updateImapConfig(
                userId = userId,
                host = config.host.getOrNull(),
                port = config.port.getOrNull(),
                password = config.password.getOrNull(),
                userName = config.userName.getOrNull(),
            )
        }.thenApplyAsync { isSuccess ->
            if (isSuccess.not()) {
                return@thenApplyAsync null
            }
            val result = userConfigRepository.getImapConfig(userId) ?: return@thenApplyAsync null

            QlUserImapConfig(
                host = result.host,
                port = result.port,
                hasPassword = result.password.isNullOrBlank().not(),
                userName = result.userName,
            )
        }.toDataFetcher()
    }
}
