package net.matsudamper.money.backend.graphql.resolver.mutation

import java.time.DateTimeException
import java.time.ZoneOffset
import java.util.concurrent.CompletionStage
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.backend.graphql.otelThenApplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.logic.PasswordManager
import net.matsudamper.money.backend.logic.ReplacePasswordUseCase
import net.matsudamper.money.graphql.model.QlChangePasswordErrorType
import net.matsudamper.money.graphql.model.QlChangePasswordResult
import net.matsudamper.money.graphql.model.QlSettingsMutation
import net.matsudamper.money.graphql.model.QlUpdateUserImapConfigInput
import net.matsudamper.money.graphql.model.QlUserImapConfig
import net.matsudamper.money.graphql.model.SettingsMutationResolver

class SettingsMutationResolverResolverImpl : SettingsMutationResolver {
    override fun changePassword(
        settingsMutation: QlSettingsMutation,
        password: String?,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlChangePasswordResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return otelSupplyAsync {
            val result = ReplacePasswordUseCase(
                context.diContainer.createAdminRepository(),
                passwordManager = PasswordManager(),
            ).replacePassword(
                userId = userId,
                password = password,
            )
            when (result) {
                is ReplacePasswordUseCase.Result.Failure -> {
                    val errorType = result.errors.map {
                        when (it) {
                            is ReplacePasswordUseCase.Result.Errors.InternalServerError -> {
                                QlChangePasswordErrorType.Unknown
                            }

                            is ReplacePasswordUseCase.Result.Errors.PasswordLength -> {
                                QlChangePasswordErrorType.PasswordLength
                            }

                            is ReplacePasswordUseCase.Result.Errors.PasswordValidation -> {
                                QlChangePasswordErrorType.PasswordInvalidChar
                            }

                            ReplacePasswordUseCase.Result.Errors.UserNotFound -> {
                                QlChangePasswordErrorType.UserNotFound
                            }
                        }
                    }
                    QlChangePasswordResult(
                        isSuccess = false,
                        errorType = errorType.firstOrNull(),
                    )
                }

                is ReplacePasswordUseCase.Result.Success -> {
                    QlChangePasswordResult(
                        isSuccess = true,
                        errorType = null,
                    )
                }
            }
        }.toDataFetcher()
    }

    override fun updateTimezoneOffset(
        settingsMutation: QlSettingsMutation,
        offsetMinutes: Int,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userConfigRepository = context.diContainer.createUserConfigRepository()
        val userId = context.verifyUserSessionAndGetUserId()
        return otelSupplyAsync {
            val offset = runCatching {
                ZoneOffset.ofTotalSeconds(Math.multiplyExact(offsetMinutes, 60))
            }.getOrElse {
                if (it is DateTimeException || it is ArithmeticException) {
                    return@otelSupplyAsync nullableIntResultBuilder()
                        .error(
                            GraphqlErrorBuilder.newError(env)
                                .message("無効なタイムゾーンオフセットです: $offsetMinutes 分")
                                .build(),
                        )
                        .build()
                }
                throw it
            }
            val isSuccess = userConfigRepository.updateTimezoneOffset(userId, offset)
            if (isSuccess.not()) {
                return@otelSupplyAsync nullableIntResultBuilder()
                    .data(null)
                    .error(
                        GraphqlErrorBuilder.newError(env)
                            .message("タイムゾーンオフセットの更新に失敗しました")
                            .build(),
                    )
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

    private fun nullableIntResultBuilder(): DataFetcherResult.Builder<Int?> {
        @Suppress("UNCHECKED_CAST")
        return DataFetcherResult.newResult<Any>() as DataFetcherResult.Builder<Int?>
    }
}
