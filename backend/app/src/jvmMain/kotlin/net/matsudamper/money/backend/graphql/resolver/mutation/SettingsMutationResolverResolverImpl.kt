package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletionStage
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.UserConfigRepository.Companion.TIMEZONE_OFFSET_RANGE
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
            if (offsetMinutes !in TIMEZONE_OFFSET_RANGE) {
                return@otelSupplyAsync nullableIntResultBuilder()
                    .error(
                        GraphqlErrorBuilder.newError(env)
                            .message(
                                "offsetMinutes は ${TIMEZONE_OFFSET_RANGE.first} から ${TIMEZONE_OFFSET_RANGE.last} の範囲で指定してください",
                            )
                            .build(),
                    )
                    .build()
            }
            val isSuccess = userConfigRepository.updateTimezoneOffset(userId, offsetMinutes)
            if (isSuccess.not()) {
                return@otelSupplyAsync nullableIntResultBuilder()
                    .data(null)
                    .build()
            }
            nullableIntResultBuilder()
                .data(offsetMinutes)
                .build()
        }
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

    @Suppress("UNCHECKED_CAST")
    private fun nullableIntResultBuilder(): DataFetcherResult.Builder<Int?> {
        return DataFetcherResult.newResult<Any>() as DataFetcherResult.Builder<Int?>
    }
}
