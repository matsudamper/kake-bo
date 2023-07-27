package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.graphql.usecase.DeleteMailUseCase
import net.matsudamper.money.backend.graphql.usecase.ImportMailUseCase
import net.matsudamper.money.backend.repository.UserLoginRepository
import net.matsudamper.money.backend.repository.UserSessionRepository
import net.matsudamper.money.element.MailId
import net.matsudamper.money.graphql.model.QlAddCategoryInput
import net.matsudamper.money.graphql.model.QlAddCategoryResult
import net.matsudamper.money.graphql.model.QlAddSubCategoryInput
import net.matsudamper.money.graphql.model.QlAddSubCategoryResult
import net.matsudamper.money.graphql.model.QlAddUsageInput
import net.matsudamper.money.graphql.model.QlDeleteMailResult
import net.matsudamper.money.graphql.model.QlDeleteMailResultError
import net.matsudamper.money.graphql.model.QlImportMailResult
import net.matsudamper.money.graphql.model.QlMoneyUsage
import net.matsudamper.money.graphql.model.QlSettingsMutation
import net.matsudamper.money.graphql.model.QlUserLoginResult
import net.matsudamper.money.graphql.model.QlUserMutation
import net.matsudamper.money.graphql.model.UserMutationResolver

class UserMutationResolverImpl : UserMutationResolver {
    override fun userLogin(userMutation: QlUserMutation, name: String, password: String, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUserLoginResult>> {
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

    override fun settingsMutation(userMutation: QlUserMutation, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlSettingsMutation?>> {
        return CompletableFuture.supplyAsync {
            QlSettingsMutation()
        }.toDataFetcher()
    }

    override fun importMail(userMutation: QlUserMutation, mailIds: List<MailId>, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlImportMailResult>> {
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

    override fun deleteMail(userMutation: QlUserMutation, mailIds: List<MailId>, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlDeleteMailResult>> {
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

    override fun addCategory(userMutation: QlUserMutation, input: QlAddCategoryInput, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlAddCategoryResult>> {
        TODO("Not yet implemented")
    }

    override fun addSubCategory(userMutation: QlUserMutation, input: QlAddSubCategoryInput, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlAddSubCategoryResult>> {
        TODO("Not yet implemented")
    }

    override fun addUsage(userMutation: QlUserMutation, usage: QlAddUsageInput, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlMoneyUsage>> {
        TODO("Not yet implemented")
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
