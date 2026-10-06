package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.graphql.GraphQlContext

@JvmName("mailFilterPrimeChildDataLoader")
fun CompletableFuture<List<MailFilterRepository.MailFilter>>.primeChildDataLoader(env: DataFetchingEnvironment): CompletableFuture<List<MailFilterRepository.MailFilter>> {
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

@JvmName("mailFilterMatcherPrimeChildDataLoader")
fun CompletableFuture<List<MailFilterRepository.Matcher>>.primeChildDataLoader(env: DataFetchingEnvironment): CompletableFuture<List<MailFilterRepository.Matcher>> {
    val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
    val userId = context.verifyUserSessionAndGetUserId()

    return thenApply { items ->
        items.forEach { item ->
            context.dataLoaders.importedMailCategoryFilterMatcherDataLoader.get(env)
                .prime(
                    ImportedMailCategoryFilterMatcherDataLoaderDefine.Key(
                        userId = userId,
                        matcherId = item.matcherId,
                    ),
                    item,
                )
        }

        items
    }
}
