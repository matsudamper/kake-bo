package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.backend.graphql.otelThenApplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.QlSettingsMutation
import net.matsudamper.money.graphql.model.QlUpdateUserImapConfigInput
import net.matsudamper.money.graphql.model.QlUserImapConfig
import net.matsudamper.money.graphql.model.SettingsMutationResolver

class SettingsMutationResolverResolverImpl : SettingsMutationResolver {
    override fun updateTimezoneOffset(
        settingsMutation: QlSettingsMutation,
        offsetMinutes: Int,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userConfigRepository = context.diContainer.createUserConfigRepository()
        val userId = context.verifyUserSessionAndGetUserId()
        return otelSupplyAsync {
            val isSuccess = userConfigRepository.updateTimezoneOffset(userId, offsetMinutes)
            if (isSuccess.not()) {
                return@otelSupplyAsync null
            }
            userConfigRepository.getTimezoneOffset(userId)
        }.toDataFetcher()
    }

    override fun updateImapConfig(
        settingsMutation: QlSettingsMutation,
        config: QlUpdateUserImapConfigInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlUserImapConfig?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userConfigRepository = context.diContainer.createUserConfigRepository()
        val userId = context.verifyUserSessionAndGetUserId()
        return otelSupplyAsync {
            userConfigRepository.updateImapConfig(
                userId = userId,
                host = config.host,
                port = config.port,
                password = config.password,
                userName = config.userName,
            )
        }.otelThenApplyAsync { isSuccess ->
            if (isSuccess.not()) {
                return@otelThenApplyAsync null
            }
            val result = userConfigRepository.getImapConfig(userId) ?: return@otelThenApplyAsync null

            QlUserImapConfig(
                host = result.host,
                port = result.port,
                hasPassword = result.password.isNullOrBlank().not(),
                userName = result.userName,
            )
        }.toDataFetcher()
    }
}
