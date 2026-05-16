package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.Base64
import java.util.concurrent.CompletionStage
import kotlinx.coroutines.runBlocking
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.fido.Auth4JModel
import net.matsudamper.money.backend.fido.AuthenticatorConverter
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.exception.GraphqlExceptions
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.lib.ChallengeModel
import net.matsudamper.money.backend.logic.AddUserUseCase
import net.matsudamper.money.backend.logic.IPasswordManager
import net.matsudamper.money.backend.logic.PasswordManager
import net.matsudamper.money.backend.logic.ReplacePasswordUseCase
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.graphql.model.AdminMutationResolver
import net.matsudamper.money.graphql.model.QlAdminAddUserErrorType
import net.matsudamper.money.graphql.model.QlAdminAddUserResult
import net.matsudamper.money.graphql.model.QlAdminFidoLoginInput
import net.matsudamper.money.graphql.model.QlAdminLoginResult
import net.matsudamper.money.graphql.model.QlAdminMutation
import net.matsudamper.money.graphql.model.QlAdminReplacePasswordErrorType
import net.matsudamper.money.graphql.model.QlAdminReplacePasswordResult

class AdminMutationResolverImpl : AdminMutationResolver {
    override fun adminLogout(
        adminMutation: QlAdminMutation,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)

        return otelSupplyAsync {
            context.clearAdminSession()
            true
        }.toDataFetcher()
    }

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

    override fun deleteImages(
        adminMutation: QlAdminMutation,
        imageIds: List<ImageId>,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyAdminSession()

        return otelSupplyAsync {
            context.diContainer.createAdminImageRepository().deleteImages(imageIds)
        }.toDataFetcher()
    }

    override fun adminLogin(
        adminMutation: QlAdminMutation,
        password: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAdminLoginResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        return otelSupplyAsync {
            runBlocking {
                minExecutionTime(LOGIN_MINIMUM_EXECUTION_TIME_MILLIS) {
                    val adminLoginRepository = context.diContainer.createAdminLoginRepository()
                    val encryptInfo = adminLoginRepository.getLoginEncryptInfo()
                        ?: return@minExecutionTime QlAdminLoginResult(isSuccess = false)

                    val algorithm = IPasswordManager.Algorithm.entries
                        .firstOrNull { it.algorithmName == encryptInfo.algorithm }
                        ?: return@minExecutionTime QlAdminLoginResult(isSuccess = false)

                    val passwordManager = PasswordManager()
                    val hashedPassword = passwordManager.getHashedPassword(
                        password = password,
                        salt = encryptInfo.salt,
                        iterationCount = encryptInfo.iterationCount,
                        keyLength = encryptInfo.keyLength,
                        algorithm = algorithm,
                    )
                    val encodedPassword = java.util.Base64.getEncoder().encodeToString(hashedPassword)

                    if (adminLoginRepository.verifyPassword(encodedPassword)) {
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
                }
            }
        }.toDataFetcher()
    }

    override fun adminFidoLogin(
        adminMutation: QlAdminMutation,
        input: QlAdminFidoLoginInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAdminLoginResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        return otelSupplyAsync {
            runBlocking {
                minExecutionTime(LOGIN_MINIMUM_EXECUTION_TIME_MILLIS) {
                    val adminUserId = ServerEnv.adminUserId
                        ?: return@minExecutionTime QlAdminLoginResult(isSuccess = false)

                    val requestUserId = String(
                        Base64.getUrlDecoder().decode(input.base64UserHandle),
                        Charsets.UTF_8,
                    ).toIntOrNull()?.let { UserId(it) }
                        ?: return@minExecutionTime QlAdminLoginResult(isSuccess = false)

                    if (requestUserId.value != adminUserId) {
                        return@minExecutionTime QlAdminLoginResult(isSuccess = false)
                    }

                    val fidoRepository = context.diContainer.createFidoRepository()
                    val challengeRepository = context.diContainer.createChallengeRepository()

                    if (ChallengeModel(challengeRepository).validateChallenge(
                            challenge = input.challenge,
                        ).not()
                    ) {
                        throw GraphqlExceptions.BadRequest("challenge is invalid")
                    }

                    val fidoList = fidoRepository.getFidoList(requestUserId)
                    for (fido in fidoList) {
                        val authenticator = AuthenticatorConverter.convertFromBase64(
                            base64AttestationStatement = fido.attestedStatement,
                            attestationStatementFormat = fido.attestedStatementFormat,
                            base64AttestedCredentialData = fido.attestedCredentialData,
                            counter = fido.counter,
                        )
                        runCatching {
                            Auth4JModel(
                                challenge = input.challenge,
                            ).verify(
                                authenticator = authenticator,
                                credentialId = input.credentialId.toByteArray(),
                                base64UserHandle = input.base64UserHandle.toByteArray(),
                                base64AuthenticatorData = input.base64AuthenticatorData.toByteArray(),
                                base64ClientDataJSON = input.base64ClientDataJson.toByteArray(),
                                clientExtensionJSON = null,
                                base64Signature = input.base64Signature.toByteArray(),
                            )
                        }.getOrNull() ?: continue

                        fidoRepository.updateCounter(
                            fidoId = fido.fidoId,
                            counter = authenticator.counter,
                            userId = requestUserId,
                        )

                        val adminSession = context.diContainer.createAdminUserSessionRepository().createSession()
                        context.setAdminSessionCookie(
                            value = adminSession.adminSessionId.id,
                            expires = adminSession.expire,
                        )
                        return@minExecutionTime QlAdminLoginResult(isSuccess = true)
                    }
                    QlAdminLoginResult(isSuccess = false)
                }
            }
        }.toDataFetcher()
    }

    override fun replacePassword(
        adminMutation: QlAdminMutation,
        userId: UserId,
        password: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAdminReplacePasswordResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyAdminSession()

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
                                QlAdminReplacePasswordErrorType.Unknown
                            }

                            is ReplacePasswordUseCase.Result.Errors.PasswordLength -> {
                                QlAdminReplacePasswordErrorType.PasswordLength
                            }

                            is ReplacePasswordUseCase.Result.Errors.PasswordValidation -> {
                                QlAdminReplacePasswordErrorType.PasswordInvalidChar
                            }

                            ReplacePasswordUseCase.Result.Errors.UserNotFound -> {
                                QlAdminReplacePasswordErrorType.UserNotFound
                            }
                        }
                    }
                    QlAdminReplacePasswordResult(
                        isSuccess = false,
                        errorType = errorType.firstOrNull(),
                    )
                }

                is ReplacePasswordUseCase.Result.Success -> {
                    QlAdminReplacePasswordResult(
                        isSuccess = true,
                        errorType = null,
                    )
                }
            }
        }.toDataFetcher()
    }
}
