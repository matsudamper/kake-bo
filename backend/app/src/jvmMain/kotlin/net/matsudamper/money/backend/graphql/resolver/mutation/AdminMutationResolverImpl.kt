package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.logic.AddUserUseCase
import net.matsudamper.money.backend.logic.IPasswordManager
import net.matsudamper.money.backend.logic.PasswordManager
import net.matsudamper.money.graphql.model.AdminMutationResolver
import net.matsudamper.money.graphql.model.QlAdminAddUserErrorType
import net.matsudamper.money.graphql.model.QlAdminAddUserResult
import net.matsudamper.money.graphql.model.QlAdminLoginResult
import net.matsudamper.money.graphql.model.QlAdminMutation

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
            val encryptInfo = context.diContainer.createAdminLoginRepository().getLoginEncryptInfo()
            if (encryptInfo == null) {
                return@otelSupplyAsync QlAdminLoginResult(isSuccess = false)
            }

            val algorithm = IPasswordManager.Algorithm.entries
                .firstOrNull { it.algorithmName == encryptInfo.algorithm }
                ?: return@otelSupplyAsync QlAdminLoginResult(isSuccess = false)

            val passwordManager = PasswordManager()
            val hashedPassword = passwordManager.getHashedPassword(
                password = password,
                salt = encryptInfo.salt,
                iterationCount = encryptInfo.iterationCount,
                keyLength = encryptInfo.keyLength,
                algorithm = algorithm,
            )
            val encodedPassword = java.util.Base64.getEncoder().encodeToString(hashedPassword)

            if (context.diContainer.createAdminLoginRepository().verifyPassword(encodedPassword)) {
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
}
