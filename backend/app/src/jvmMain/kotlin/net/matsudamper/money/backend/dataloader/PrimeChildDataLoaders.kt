package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.graphql.GraphQlContext

@JvmName("mailFilterPrimeChildDataLoader")
fun CompletableFuture<List<MailFilterRepository.MailFilter>>.primeChildDataLoader(
    env: DataFetchingEnvironment,
): CompletableFuture<List<MailFilterRepository.MailFilter>> {
    val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
    val userId = context.verifyUserSessionAndGetUserId()
    return thenApply { items ->
        items.forEach { item ->
            context.dataLoaders.importedMailCategoryFilterDataLoader.get(env)
                .prime(
                    ImportedMailCategoryFilterDataLoaderDefine.Key(
                        userId = userId,
                        categoryFilterId = item.importedMailCategoryFilterId,
                    ),
                    item,
                )
        }

        items
    }
}

@JvmName("mailFilterConditionPrimeChildDataLoader")
fun CompletableFuture<List<MailFilterRepository.Condition>>.primeChildDataLoader(
    env: DataFetchingEnvironment,
): CompletableFuture<List<MailFilterRepository.Condition>> {
    val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
    val userId = context.verifyUserSessionAndGetUserId()

    return thenApply { items ->
        items.forEach { item ->
            context.dataLoaders.importedMailCategoryFilterConditionDataLoader.get(env)
                .prime(
                    ImportedMailCategoryFilterConditionDataLoaderDefine.Key(
                        userId = userId,
                        conditionId = item.conditionId,
                    ),
                    item,
                )
        }

        items
    }
}
