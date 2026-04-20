package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.logic.AddUserUseCase
import net.matsudamper.money.backend.logic.PasswordManager
import net.matsudamper.money.backend.logic.ResetPasswordUseCase
import net.matsudamper.money.graphql.model.AdminMutationResolver
import net.matsudamper.money.graphql.model.QlAdminAddUserErrorType
import net.matsudamper.money.graphql.model.QlAdminAddUserResult
import net.matsudamper.money.graphql.model.QlAdminLoginResult
import net.matsudamper.money.graphql.model.QlAdminMutation
import net.matsudamper.money.graphql.model.QlAdminResetPasswordErrorType
import net.matsudamper.money.graphql.model.QlAdminResetPasswordResult
import net.matsudamper.money.graphql.model.QlAdminUserSearchResult

class AdminMutationResolverImpl : AdminMutationResolver {
    override fun addUser(
        adminMutation: QlAdminMutation,
        name: String,
        password: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAdminAddUserResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyAdminSession()

        return otelSupplyAsync {
            val result = AddUserUseCase(
                context.diContainer.createAdminRepository(),
                passwordManager = PasswordManager(),
            ).addUser(
                userName = name,
                password = password,
            )
            when (result) {
                is AddUserUseCase.Result.Failure -> {
                    QlAdminAddUserResult(
                        errorType = result.errors.map {
                            when (it) {
                                is AddUserUseCase.Result.Errors.InternalServerError -> {
                                    QlAdminAddUserErrorType.Unknown
                                }

                                is AddUserUseCase.Result.Errors.PasswordLength -> {
                                    QlAdminAddUserErrorType.PasswordLength
                                }

                                is AddUserUseCase.Result.Errors.PasswordValidation -> {
                                    QlAdminAddUserErrorType.PasswordInvalidChar
                                }

                                AddUserUseCase.Result.Errors.UserNameLength -> {
                                    QlAdminAddUserErrorType.UserNameLength
                                }

                                AddUserUseCase.Result.Errors.UserNameValidation -> {
                                    QlAdminAddUserErrorType.UserNameInvalidChar
                                }
                            }
                        },
                    )
                }

                is AddUserUseCase.Result.Success -> {
                    QlAdminAddUserResult(
                        errorType = listOf(),
                    )
                }
            }
        }.toDataFetcher()
    }

    override fun adminLogin(
        adminMutation: QlAdminMutation,
        password: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAdminLoginResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        return otelSupplyAsync {
            if (password == ServerEnv.adminPassword) {
                val adminSession = context.diContainer.createAdminUserSessionRepository().createSession()
                context.setAdminSessionCookie(
                    value = adminSession.adminSessionId.id,
                    expires = adminSession.expire,
                )
                QlAdminLoginResult(
                    isSuccess = true,
                )
            } else {
                QlAdminLoginResult(
                    isSuccess = false,
                )
            }
        }.toDataFetcher()
    }

    override fun searchUsers(
        adminMutation: QlAdminMutation,
        query: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAdminUserSearchResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyAdminSession()

        return otelSupplyAsync {
            val users = context.diContainer.createAdminRepository().searchUsers(query)
            QlAdminUserSearchResult(
                users = users,
            )
        }.toDataFetcher()
    }

    override fun resetPassword(
        adminMutation: QlAdminMutation,
        userName: String,
        password: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAdminResetPasswordResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyAdminSession()

        return otelSupplyAsync {
            val result = ResetPasswordUseCase(
                context.diContainer.createAdminRepository(),
                passwordManager = PasswordManager(),
            ).resetPassword(
                userName = userName,
                password = password,
            )
            when (result) {
                is ResetPasswordUseCase.Result.Failure -> {
                    val errorType = result.errors.map {
                        when (it) {
                            is ResetPasswordUseCase.Result.Errors.InternalServerError -> {
                                QlAdminResetPasswordErrorType.Unknown
                            }

                            is ResetPasswordUseCase.Result.Errors.PasswordLength -> {
                                QlAdminResetPasswordErrorType.PasswordLength
                            }

                            is ResetPasswordUseCase.Result.Errors.PasswordValidation -> {
                                QlAdminResetPasswordErrorType.PasswordInvalidChar
                            }

                            ResetPasswordUseCase.Result.Errors.UserNotFound -> {
                                QlAdminResetPasswordErrorType.UserNotFound
                            }
                        }
                    }
                    QlAdminResetPasswordResult(
                        isSuccess = false,
                        errorType = errorType.firstOrNull(),
                    )
                }

                is ResetPasswordUseCase.Result.Success -> {
                    QlAdminResetPasswordResult(
                        isSuccess = true,
                        errorType = null,
                    )
                }
            }
        }.toDataFetcher()
    }
}
