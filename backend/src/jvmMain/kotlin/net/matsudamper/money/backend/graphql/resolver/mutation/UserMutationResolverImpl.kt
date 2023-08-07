package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.converter.toDBElement
import net.matsudamper.money.backend.graphql.converter.toDbElement
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.graphql.usecase.DeleteMailUseCase
import net.matsudamper.money.backend.graphql.usecase.ImportMailUseCase
import net.matsudamper.money.backend.repository.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.repository.MoneyUsageRepository
import net.matsudamper.money.backend.repository.MoneyUsageSubCategoryRepository
import net.matsudamper.money.backend.repository.UserLoginRepository
import net.matsudamper.money.backend.repository.UserSessionRepository
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.graphql.model.QlAddCategoryInput
import net.matsudamper.money.graphql.model.QlAddCategoryResult
import net.matsudamper.money.graphql.model.QlAddImportedMailCategoryFilterConditionInput
import net.matsudamper.money.graphql.model.QlAddImportedMailCategoryFilterInput
import net.matsudamper.money.graphql.model.QlAddSubCategoryError
import net.matsudamper.money.graphql.model.QlAddSubCategoryInput
import net.matsudamper.money.graphql.model.QlAddSubCategoryResult
import net.matsudamper.money.graphql.model.QlAddUsageQuery
import net.matsudamper.money.graphql.model.QlDeleteMailResult
import net.matsudamper.money.graphql.model.QlDeleteMailResultError
import net.matsudamper.money.graphql.model.QlImportMailResult
import net.matsudamper.money.graphql.model.QlImportedMailCategoryCondition
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilter
import net.matsudamper.money.graphql.model.QlMoneyUsage
import net.matsudamper.money.graphql.model.QlMoneyUsageCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import net.matsudamper.money.graphql.model.QlSettingsMutation
import net.matsudamper.money.graphql.model.QlUpdateCategoryQuery
import net.matsudamper.money.graphql.model.QlUpdateImportedMailCategoryFilterConditionInput
import net.matsudamper.money.graphql.model.QlUpdateImportedMailCategoryFilterInput
import net.matsudamper.money.graphql.model.QlUpdateSubCategoryQuery
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
                    UserLoginRepository()
                        .login(
                            userName = name,
                            passwords = password,
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
                    val createSessionResult = UserSessionRepository().createSession(loginResult.uerId)
                    context.setUserSessionCookie(
                        createSessionResult.sessionId.id,
                        createSessionResult.expire,
                    )
                    QlUserLoginResult(
                        isSuccess = true,
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
        val userId = context.verifyUserSession()
        return CompletableFuture.supplyAsync {
            val result = ImportMailUseCase(context.repositoryFactory).insertMail(
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
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val result = DeleteMailUseCase(
                repositoryFactory = context.repositoryFactory,
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
        val userId = context.verifyUserSession()
        return CompletableFuture.supplyAsync {
            val addResult = context.repositoryFactory.createMoneyUsageCategoryRepository()
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
        val userId = context.verifyUserSession()
        return CompletableFuture.supplyAsync {
            val addResult = context.repositoryFactory.createMoneyUsageSubCategoryRepository()
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
        val userId = context.verifyUserSession()
        return CompletableFuture.supplyAsync {
            val result = context.repositoryFactory.createMoneyUsageSubCategoryRepository()
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
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val result = context.repositoryFactory.createMoneyUsageSubCategoryRepository()
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
        val userId = context.verifyUserSession()

        val dataLoader = context.dataLoaders.importedMailCategoryFilterDataLoader.get(env)

        return CompletableFuture.supplyAsync {
            val result = context.repositoryFactory.createMailFilterRepository()
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
        val userId = context.verifyUserSession()
        return CompletableFuture.supplyAsync {
            val result = context.repositoryFactory.createMoneyUsageCategoryRepository()
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
        val userId = context.verifyUserSession()
        return CompletableFuture.supplyAsync {
            val result = context.repositoryFactory.createMoneyUsageRepository()
                .addUsage(
                    userId = userId,
                    title = usage.title,
                    description = usage.description,
                    subCategoryId = usage.subCategoryId,
                    amount = usage.amount,
                    date = usage.date,
                    importedMailId = usage.importedMailId
                )
            when (result) {
                is MoneyUsageRepository.AddResult.Failed -> {
                    throw result.error
                }

                is MoneyUsageRepository.AddResult.Success -> {
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
        val userId = context.verifyUserSession()

        return CompletableFuture.allOf().thenApplyAsync {
            val repository = context.repositoryFactory.createMailFilterRepository()
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
        val userId = context.verifyUserSession()
        val repository = context.repositoryFactory.createMailFilterRepository()

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
        val userId = context.verifyUserSession()
        val repository = context.repositoryFactory.createMailFilterRepository()

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
        val userId = context.verifyUserSession()
        val repository = context.repositoryFactory.createMailFilterRepository()

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
        val userId = context.verifyUserSession()
        val repository = context.repositoryFactory.createMailFilterRepository()

        return CompletableFuture.allOf().thenApplyAsync {
            val isSuccess = repository.deleteCondition(
                userId = userId,
                conditionId = id,
            )
            isSuccess
        }.toDataFetcher()
    }
}

private suspend fun <T> minExecutionTime(minMillSecond: Long, block: () -> T): T {
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
