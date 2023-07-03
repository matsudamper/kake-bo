package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.repository.UserLoginRepository
import net.matsudamper.money.backend.repository.UserSessionRepository
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.UserConfigRepository
import net.matsudamper.money.graphql.model.QlSettingsMutation
import net.matsudamper.money.graphql.model.QlUserLoginResult
import net.matsudamper.money.graphql.model.QlUserMutation
import net.matsudamper.money.graphql.model.SettingsMutationResolver
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
