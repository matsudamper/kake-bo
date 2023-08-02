package net.matsudamper.money.frontend.common.viewmodel.mail

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MailScreenQuery
import net.matsudamper.money.lib.ResultWrapper

public class MailScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {

    public fun get(
        id: ImportedMailId,
    ): Flow<ResultWrapper<ApolloResponse<MailScreenQuery.Data>>> {
        return flow {
            apolloClient
                .query(
                    MailScreenQuery(
                        id = id,
                    )
                )
                .toFlow()
                .catch {
                    emit(ResultWrapper.failure(it))
                }
                .collect {
                    emit(ResultWrapper.success(it))
                }
        }
    }
}
