package net.matsudamper.money.backend.graphql.resolver.mutation

import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageSubCategoryRepository
import net.matsudamper.money.backend.app.interfaces.UserLoginRepository
import net.matsudamper.money.backend.base.ServerVariables
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.fido.Auth4JModel
import net.matsudamper.money.backend.fido.AuthenticatorConverter
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.converter.toDBElement
import net.matsudamper.money.backend.graphql.converter.toDbElement
import net.matsudamper.money.backend.graphql.exception.GraphqlExceptions
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.graphql.usecase.DeleteMailUseCase
import net.matsudamper.money.backend.graphql.usecase.ImportMailUseCase
import net.matsudamper.money.backend.lib.ChallengeModel
import net.matsudamper.money.backend.logic.ApiTokenEncryptManager
import net.matsudamper.money.backend.logic.IPasswordManager
import net.matsudamper.money.backend.logic.PasswordManager
import net.matsudamper.money.element.ApiTokenId
import net.matsudamper.money.element.FidoId
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.graphql.model.QlAddCategoryInput
import net.matsudamper.money.graphql.model.QlAddCategoryResult
import net.matsudamper.money.graphql.model.QlAddImportedMailCategoryFilterConditionInput
import net.matsudamper.money.graphql.model.QlAddImportedMailCategoryFilterInput
import net.matsudamper.money.graphql.model.QlAddSubCategoryError
import net.matsudamper.money.graphql.model.QlAddSubCategoryInput
import net.matsudamper.money.graphql.model.QlAddSubCategoryResult
import net.matsudamper.money.graphql.model.QlAddUsageQuery
import net.matsudamper.money.graphql.model.QlChangeSessionNameResult
import net.matsudamper.money.graphql.model.QlDeleteApiTokenResult
import net.matsudamper.money.graphql.model.QlDeleteFidoResult
import net.matsudamper.money.graphql.model.QlDeleteMailResult
import net.matsudamper.money.graphql.model.QlDeleteMailResultError
import net.matsudamper.money.graphql.model.QlDeleteSessionResult
import net.matsudamper.money.graphql.model.QlImportMailResult
import net.matsudamper.money.graphql.model.QlImportedMailCategoryCondition
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilter
import net.matsudamper.money.graphql.model.QlMoneyUsage
import net.matsudamper.money.graphql.model.QlMoneyUsageCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import net.matsudamper.money.graphql.model.QlRegisterApiTokenResult
import net.matsudamper.money.graphql.model.QlRegisterFidoInput
import net.matsudamper.money.graphql.model.QlRegisteredFidoInfo
import net.matsudamper.money.graphql.model.QlRegisteredFidoResult
import net.matsudamper.money.graphql.model.QlSession
import net.matsudamper.money.graphql.model.QlSettingsMutation
import net.matsudamper.money.graphql.model.QlUpdateCategoryQuery
import net.matsudamper.money.graphql.model.QlUpdateImportedMailCategoryFilterConditionInput
import net.matsudamper.money.graphql.model.QlUpdateImportedMailCategoryFilterInput
import net.matsudamper.money.graphql.model.QlUpdateSubCategoryQuery
import net.matsudamper.money.graphql.model.QlUpdateUsageQuery
import net.matsudamper.money.graphql.model.QlUserFidoLoginInput
import net.matsudamper.money.graphql.model.QlUserLoginResult
import net.matsudamper.money.graphql.model.QlUserMutation
import net.matsudamper.money.graphql.model.UserMutationResolver

class UserMutationResolverImpl : UserMutationResolver {
    override fun userLogin(
        userMutation: QlUserMutation,
        name: String,
        password: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlUserLoginResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)

        return CompletableFuture.supplyAsync {
            // 連続実行や、ユーザーが存在しているかの検知を防ぐために、最低でも1秒はかかるようにする
            val loginResult = runBlocking {
                minExecutionTime(1000) {
                    val encryptInfo = context.diContainer.userLoginRepository().getLoginEncryptInfo(name)
                        ?: return@minExecutionTime UserLoginRepository.Result.Failure
                    val hashedPassword = PasswordManager().getHashedPassword(
                        password = password,
                        salt = encryptInfo.salt,
                        iterationCount = encryptInfo.iterationCount,
                        keyLength = encryptInfo.keyLength,
                        algorithm = IPasswordManager.Algorithm.entries
                            .firstOrNull { it.algorithmName == encryptInfo.algorithm }
                            ?: throw IllegalArgumentException("algorithm=[${encryptInfo.algorithm}] not found"),
                    )

                    context.diContainer.userLoginRepository()
                        .login(
                            userName = name,
                            hashedPassword = hashedPassword,
                        )
                }
            }
            when (loginResult) {
                is UserLoginRepository.Result.Failure -> {
                    QlUserLoginResult(
                        isSuccess = false,
                    )
                }

                is UserLoginRepository.Result.Success -> {
                    val createSessionResult = context.diContainer.createUserSessionRepository().createSession(loginResult.uerId)
                    context.setUserSessionCookie(
                        createSessionResult.sessionId.id,
                        createSessionResult.latestAccess
                            .plusDays(ServerVariables.USER_SESSION_EXPIRE_DAY),
                    )
                    QlUserLoginResult(
                        isSuccess = true,
                    )
                }
            }
        }.toDataFetcher()
    }

    override fun logout(
        userMutation: QlUserMutation,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)

        return CompletableFuture.supplyAsync {
            context.clearUserSession()
            true
        }.toDataFetcher()
    }

    override fun deleteFido(
        userMutation: QlUserMutation,
        id: FidoId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlDeleteFidoResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val fidoRepository = context.diContainer.createFidoRepository()
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val isSuccess = fidoRepository.deleteFido(userId, id)
            QlDeleteFidoResult(
                isSuccess = isSuccess,
            )
        }.toDataFetcher()
    }

    override fun deleteSession(
        userMutation: QlUserMutation,
        name: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlDeleteSessionResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userSessionRepository = context.diContainer.createUserSessionRepository()
        val sessionInfo = context.verifyUserSessionAndGetSessionInfo()

        return CompletableFuture.supplyAsync {
            val isSuccess = userSessionRepository.deleteSession(
                userId = sessionInfo.userId,
                sessionName = name,
                currentSessionName = sessionInfo.sessionName,
            )
            QlDeleteSessionResult(
                isSuccess = isSuccess,
            )
        }.toDataFetcher()
    }

    override fun changeSessionName(
        userMutation: QlUserMutation,
        name: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlChangeSessionNameResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userSessionRepository = context.diContainer.createUserSessionRepository()
        val sessionInfo = context.verifyUserSessionAndGetSessionInfo()

        return CompletableFuture.supplyAsync {
            val result = userSessionRepository.changeSessionName(
                sessionId = sessionInfo.sessionId,
                name = name,
            )
            QlChangeSessionNameResult(
                isSuccess = result != null,
                session = run session@{
                    result ?: return@session null
                    QlSession(
                        name = result.name,
                        lastAccess = result.latestAccess.atOffset(ZoneOffset.UTC),
                    )
                },
            )
        }.toDataFetcher()
    }

    override fun userFidoLogin(
        userMutation: QlUserMutation,
        userFidoLoginInput: QlUserFidoLoginInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlUserLoginResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val fidoRepository = context.diContainer.createFidoRepository()
        val challengeRepository = context.diContainer.createChallengeRepository()
        return CompletableFuture.supplyAsync {
            runBlocking {
                minExecutionTime(1000) {
                    val requestUserId = String(
                        Base64.getUrlDecoder().decode(userFidoLoginInput.base64UserHandle),
                        Charsets.UTF_8,
                    ).toIntOrNull()
                        ?.let { UserId(it) }
                        ?: return@minExecutionTime QlUserLoginResult(
                            isSuccess = false,
                        )
                    val fidoList = fidoRepository.getFidoList(requestUserId)
                    if (ChallengeModel(challengeRepository).validateChallenge(
                            challenge = userFidoLoginInput.challenge,
                        ).not()
                    ) {
                        throw GraphqlExceptions.BadRequest("challenge is invalid")
                    }

                    for (fido in fidoList) {
                        val authenticator = AuthenticatorConverter.convertFromBase64(
                            base64AttestationStatement = fido.attestedStatement,
                            attestationStatementFormat = fido.attestedStatementFormat,
                            base64AttestedCredentialData = fido.attestedCredentialData,
                            counter = fido.counter,
                        )
                        runCatching {
                            Auth4JModel(
                                challenge = userFidoLoginInput.challenge,
                            ).verify(
                                authenticator = authenticator,
                                credentialId = userFidoLoginInput.credentialId.toByteArray(),
                                base64UserHandle = userFidoLoginInput.base64UserHandle.toByteArray(),
                                base64AuthenticatorData = userFidoLoginInput.base64AuthenticatorData.toByteArray(),
                                base64ClientDataJSON = userFidoLoginInput.base64ClientDataJson.toByteArray(),
                                clientExtensionJSON = null,
                                base64Signature = userFidoLoginInput.base64Signature.toByteArray(),
                            )
                        }.getOrNull() ?: continue

                        fidoRepository.updateCounter(
                            fidoId = fido.fidoId,
                            counter = authenticator.counter,
                            userId = requestUserId,
                        )

                        val createSessionResult = context.diContainer.createUserSessionRepository().createSession(requestUserId)
                        context.setUserSessionCookie(
                            createSessionResult.sessionId.id,
                            createSessionResult.latestAccess
                                .plusDays(ServerVariables.USER_SESSION_EXPIRE_DAY),
                        )
                        return@minExecutionTime QlUserLoginResult(
                            isSuccess = true,
                        )
                    }
                    QlUserLoginResult(
                        isSuccess = false,
                    )
                }
            }
        }.toDataFetcher()
    }

    override fun settingsMutation(
        userMutation: QlUserMutation,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlSettingsMutation?>> {
        return CompletableFuture.supplyAsync {
            QlSettingsMutation()
        }.toDataFetcher()
    }

    override fun importMail(
        userMutation: QlUserMutation,
        mailIds: List<MailId>,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportMailResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return CompletableFuture.supplyAsync {
            val result = ImportMailUseCase(context.diContainer).insertMail(
                userId = userId,
                mailIds = mailIds,
            )

            QlImportMailResult(
                isSuccess = when (result) {
                    is ImportMailUseCase.Result.Success -> true
                    is ImportMailUseCase.Result.Failure,
                    is ImportMailUseCase.Result.ImapConfigNotFound,
                    -> false
                },
            )
        }.toDataFetcher()
    }

    override fun deleteMail(
        userMutation: QlUserMutation,
        mailIds: List<MailId>,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlDeleteMailResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val result = DeleteMailUseCase(
                repositoryFactory = context.diContainer,
            ).delete(userId = userId, mailIds = mailIds)

            val error: QlDeleteMailResultError?
            val isSuccess: Boolean
            when (result) {
                is DeleteMailUseCase.Result.Exception -> {
                    isSuccess = false
                    error = QlDeleteMailResultError.InternalServerError
                }

                is DeleteMailUseCase.Result.Failure -> {
                    isSuccess = false
                    error = QlDeleteMailResultError.InternalServerError
                }

                is DeleteMailUseCase.Result.ImapConfigNotFound -> {
                    isSuccess = false
                    error = QlDeleteMailResultError.MailConfigNotFound
                }

                is DeleteMailUseCase.Result.Success -> {
                    isSuccess = true
                    error = null
                }
            }
            QlDeleteMailResult(
                error = error,
                isSuccess = isSuccess,
            )
        }.toDataFetcher()
    }

    override fun addCategory(
        userMutation: QlUserMutation,
        input: QlAddCategoryInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAddCategoryResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return CompletableFuture.supplyAsync {
            val addResult = context.diContainer.createMoneyUsageCategoryRepository()
                .addCategory(
                    userId = userId,
                    name = input.name,
                )
            when (addResult) {
                is MoneyUsageCategoryRepository.AddCategoryResult.Failed -> {
                    throw addResult.error
                }

                is MoneyUsageCategoryRepository.AddCategoryResult.Success -> {
                    return@supplyAsync QlAddCategoryResult(
                        QlMoneyUsageCategory(
                            id = addResult.result.moneyUsageCategoryId,
                        ),
                    )
                }
            }
        }.toDataFetcher()
    }

    override fun addSubCategory(
        userMutation: QlUserMutation,
        input: QlAddSubCategoryInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAddSubCategoryResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return CompletableFuture.supplyAsync {
            val addResult = context.diContainer.createMoneyUsageSubCategoryRepository()
                .addSubCategory(
                    userId = userId,
                    name = input.name,
                    categoryId = input.categoryId,
                )
            when (addResult) {
                is MoneyUsageSubCategoryRepository.AddSubCategoryResult.Failed.CategoryNotFound -> {
                    QlAddSubCategoryResult(
                        subCategory = null,
                        error = QlAddSubCategoryError.CATEGORY_NOT_FOUND,
                    )
                }

                is MoneyUsageSubCategoryRepository.AddSubCategoryResult.Failed.Error -> {
                    throw addResult.error
                }

                is MoneyUsageSubCategoryRepository.AddSubCategoryResult.Success -> {
                    QlAddSubCategoryResult(
                        subCategory = QlMoneyUsageSubCategory(
                            id = addResult.result.moneyUsageSubCategoryId,
                        ),
                        error = null,
                    )
                }
            }
        }.toDataFetcher()
    }

    override fun updateSubCategory(
        userMutation: QlUserMutation,
        id: MoneyUsageSubCategoryId,
        query: QlUpdateSubCategoryQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return CompletableFuture.supplyAsync {
            val result = context.diContainer.createMoneyUsageSubCategoryRepository()
                .updateSubCategory(
                    userId = userId,
                    subCategoryId = id,
                    name = query.name,
                )
            if (result) {
                QlMoneyUsageSubCategory(
                    id = id,
                )
            } else {
                throw IllegalStateException("update sub category failed")
            }
        }.toDataFetcher()
    }

    override fun deleteSubCategory(
        userMutation: QlUserMutation,
        id: MoneyUsageSubCategoryId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val result = context.diContainer.createMoneyUsageSubCategoryRepository()
                .deleteSubCategory(
                    userId = userId,
                    subCategoryId = id,
                )
            if (result) {
                true
            } else {
                throw IllegalStateException("delete sub category failed")
            }
        }.toDataFetcher()
    }

    override fun addImportedMailCategoryFilter(
        userMutation: QlUserMutation,
        input: QlAddImportedMailCategoryFilterInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilter?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        val dataLoader = context.dataLoaders.importedMailCategoryFilterDataLoader.get(env)

        return CompletableFuture.supplyAsync {
            val result = context.diContainer.createMailFilterRepository()
                .addFilter(
                    userId = userId,
                    title = input.title,
                    orderNum = 0,
                ).onFailure {
                    it.printStackTrace()
                }.getOrNull() ?: return@supplyAsync null

            dataLoader.prime(
                ImportedMailCategoryFilterDataLoaderDefine.Key(
                    userId = userId,
                    categoryFilterId = result.importedMailCategoryFilterId,
                ),
                result,
            )

            QlImportedMailCategoryFilter(
                id = result.importedMailCategoryFilterId,
            )
        }.toDataFetcher()
    }

    override fun updateCategory(
        userMutation: QlUserMutation,
        id: MoneyUsageCategoryId,
        query: QlUpdateCategoryQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageCategory>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return CompletableFuture.supplyAsync {
            val result = context.diContainer.createMoneyUsageCategoryRepository()
                .updateCategory(
                    userId = userId,
                    categoryId = id,
                    name = query.name,
                )
            if (result) {
                QlMoneyUsageCategory(
                    id = id,
                )
            } else {
                throw IllegalStateException("update category failed")
            }
        }.toDataFetcher()
    }

    override fun addUsage(
        userMutation: QlUserMutation,
        usage: QlAddUsageQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsage>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return CompletableFuture.supplyAsync {
            val moneyUsageRepository = context.diContainer.createMoneyUsageRepository()
            val result = moneyUsageRepository
                .addUsage(
                    userId = userId,
                    title = usage.title,
                    description = usage.description,
                    subCategoryId = usage.subCategoryId,
                    amount = usage.amount,
                    date = usage.date,
                )
            when (result) {
                is MoneyUsageRepository.AddResult.Failed -> {
                    throw result.error
                }

                is MoneyUsageRepository.AddResult.Success -> {
                    val mailId = usage.importedMailId
                    if (mailId != null) {
                        val relationResult = moneyUsageRepository.addMailRelation(
                            userId = userId,
                            usageId = result.result.id,
                            importedMailId = mailId,
                        )
                        if (relationResult.not()) {
                            throw IllegalStateException("add mail relation failed")
                        }
                    }

                    return@supplyAsync QlMoneyUsage(
                        id = result.result.id,
                    )
                }
            }
        }.toDataFetcher()
    }

    override fun updateImportedMailCategoryFilter(
        userMutation: QlUserMutation,
        input: QlUpdateImportedMailCategoryFilterInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilter?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.allOf().thenApplyAsync {
            val repository = context.diContainer.createMailFilterRepository()
            val isSuccess = repository.updateFilter(
                filterId = input.id,
                userId = userId,
                title = input.title,
                orderNum = input.orderNumber,
                subCategory = input.subCategoryId,
                operator = input.operator?.toDBElement(),
            )
            if (isSuccess) {
                QlImportedMailCategoryFilter(
                    id = input.id,
                )
            } else {
                null
            }
        }.toDataFetcher()
    }

    override fun addImportedMailCategoryFilterCondition(
        userMutation: QlUserMutation,
        input: QlAddImportedMailCategoryFilterConditionInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilter?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createMailFilterRepository()

        return CompletableFuture.allOf().thenApplyAsync {
            val isSuccess = repository.addCondition(
                userId = userId,
                filterId = input.id,
                condition = input.conditionType?.toDbElement(),
                text = input.text,
                dataSource = input.dataSourceType?.toDbElement(),
            )
            if (isSuccess.not()) {
                return@thenApplyAsync null
            }
            QlImportedMailCategoryFilter(
                id = input.id,
            )
        }.toDataFetcher()
    }

    override fun updateImportedMailCategoryFilterCondition(
        userMutation: QlUserMutation,
        input: QlUpdateImportedMailCategoryFilterConditionInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryCondition?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createMailFilterRepository()

        return CompletableFuture.allOf().thenApplyAsync {
            val isSuccess = repository.updateCondition(
                userId = userId,
                conditionId = input.id,
                conditionType = input.conditionType?.toDbElement(),
                dataSource = input.dataSourceType?.toDbElement(),
                text = input.text,
            )
            if (isSuccess.not()) return@thenApplyAsync null

            QlImportedMailCategoryCondition(
                id = input.id,
            )
        }.toDataFetcher()
    }

    override fun deleteImportedMailCategoryFilter(
        userMutation: QlUserMutation,
        id: ImportedMailCategoryFilterId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createMailFilterRepository()

        return CompletableFuture.allOf().thenApplyAsync {
            val isSuccess = repository.deleteFilter(
                userId = userId,
                filterId = id,
            )
            isSuccess
        }.toDataFetcher()
    }

    override fun deleteImportedMailCategoryFilterCondition(
        userMutation: QlUserMutation,
        id: ImportedMailCategoryFilterConditionId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createMailFilterRepository()

        return CompletableFuture.allOf().thenApplyAsync {
            val isSuccess = repository.deleteCondition(
                userId = userId,
                conditionId = id,
            )
            isSuccess
        }.toDataFetcher()
    }

    override fun registerFido(
        userMutation: QlUserMutation,
        input: QlRegisterFidoInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlRegisteredFidoResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val fidoRepository = context.diContainer.createFidoRepository()
        val userId = context.verifyUserSessionAndGetUserId()
        val challengeRepository = context.diContainer.createChallengeRepository()

        return CompletableFuture.supplyAsync {
            if (ChallengeModel(challengeRepository).validateChallenge(
                    challenge = input.challenge,
                ).not()
            ) {
                throw GraphqlExceptions.BadRequest("challenge is invalid")
            }
            val auth4JModel = Auth4JModel(
                challenge = input.challenge,
            )
            val base64Result = auth4JModel.register(
                base64AttestationObject = input.base64AttestationObject.toByteArray(),
                base64ClientDataJSON = input.base64ClientDataJson.toByteArray(),
                clientExtensionsJSON = null,
            )

            val addedItem = fidoRepository.addFido(
                name = input.displayName,
                userId = userId,
                attestationStatement = base64Result.base64AttestationStatement,
                attestationStatementFormat = base64Result.attestationStatementFormat,
                attestedCredentialData = base64Result.base64AttestedCredentialData,
                counter = base64Result.counter,
                authenticatorExtensions = null,
            )
            QlRegisteredFidoResult(
                fidoInfo = QlRegisteredFidoInfo(
                    id = addedItem.fidoId,
                    name = addedItem.name,
                    base64CredentialId = base64Result.base64CredentialId,
                ),
            )
        }.toDataFetcher()
    }

    override fun deleteImportedMail(
        userMutation: QlUserMutation,
        id: ImportedMailId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createDbMailRepository()

        return CompletableFuture.allOf().thenApplyAsync {
            val isSuccess = repository.deleteMail(
                userId = userId,
                mailId = id,
            )
            isSuccess
        }.toDataFetcher()
    }

    override fun deleteUsage(
        userMutation: QlUserMutation,
        id: MoneyUsageId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createMoneyUsageRepository()

        return CompletableFuture.allOf().thenApplyAsync {
            val isSuccess = repository.deleteUsage(
                userId = userId,
                usageId = id,
            )
            isSuccess
        }.toDataFetcher()
    }

    override fun updateUsage(
        userMutation: QlUserMutation,
        query: QlUpdateUsageQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsage>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createMoneyUsageRepository()

        return CompletableFuture.allOf().thenApplyAsync {
            val isSuccess = repository.updateUsage(
                userId = userId,
                usageId = query.id,
                title = query.title,
                description = query.description,
                amount = query.amount,
                date = query.date,
                subCategoryId = query.subCategoryId,
            )

            if (isSuccess.not()) {
                throw IllegalStateException("update usage failed")
            } else {
                QlMoneyUsage(
                    id = query.id,
                )
            }
        }.toDataFetcher()
    }

    override fun registerApiToken(
        userMutation: QlUserMutation,
        name: String,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlRegisterApiTokenResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createApiTokenRepository()

        val createTokenResult = ApiTokenEncryptManager().createApiToken()
        val encryptApiToken = PasswordManager().create(
            password = createTokenResult.token,
            keyByteLength = createTokenResult.encryptInfo.keyByteLength,
            iterationCount = createTokenResult.encryptInfo.iterationCount,
            salt = createTokenResult.encryptInfo.salt,
            algorithm = IPasswordManager.Algorithm.entries.first { it.algorithmName == createTokenResult.encryptInfo.algorithmName },
        )

        repository.registerToken(
            id = userId,
            hashedToken = encryptApiToken.hashedPassword,
            name = name,
        )

        return CompletableFuture.supplyAsync {
            QlRegisterApiTokenResult(
                isSuccess = true,
                apiToken = createTokenResult.token,
            )
        }.toDataFetcher()
    }

    override fun deleteApiToken(userMutation: QlUserMutation, id: ApiTokenId, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlDeleteApiTokenResult>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val repository = context.diContainer.createApiTokenRepository()
        return CompletableFuture.supplyAsync {
            val isSuccess = repository.deleteToken(userId = userId, apiTokenId = id)
            QlDeleteApiTokenResult(
                isSuccess = isSuccess,
            )
        }.toDataFetcher()
    }
}

@OptIn(ExperimentalContracts::class)
private suspend fun <T> minExecutionTime(
    minMillSecond: Long,
    block: suspend () -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val startTime = System.currentTimeMillis()
    val result = block()
    while (true) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - startTime >= minMillSecond) {
            break
        }
        delay(10)
    }

    return result
}
