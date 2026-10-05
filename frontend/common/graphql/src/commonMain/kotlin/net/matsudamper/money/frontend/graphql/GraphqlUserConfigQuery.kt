package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import net.matsudamper.money.frontend.graphql.type.UpdateUserImapConfigInput

class GraphqlUserConfigQuery(
    private val graphqlClient: GraphqlClient,
) {
    suspend fun getConfig(): ApolloResponse<GetConfigQuery.Data> {
        return graphqlClient.apolloClient
            .query(GetConfigQuery())
            .execute()
    }

    suspend fun setImapHost(host: String): ApolloResponse<SetImapConfigMutation.Data> {
        return graphqlClient.apolloClient
            .mutation(
                SetImapConfigMutation(
                    UpdateUserImapConfigInput(
                        host = Optional.present(host),
                    ),
                ),
            )
            .execute()
    }

    suspend fun setImapPort(port: Int): ApolloResponse<SetImapConfigMutation.Data> {
        return graphqlClient.apolloClient
            .mutation(
                SetImapConfigMutation(
                    UpdateUserImapConfigInput(
                        port = Optional.present(port),
                    ),
                ),
            )
            .execute()
    }

    suspend fun setImapUserName(userName: String): ApolloResponse<SetImapConfigMutation.Data> {
        return graphqlClient.apolloClient
            .mutation(
                SetImapConfigMutation(
                    UpdateUserImapConfigInput(
                        userName = Optional.present(userName),
                    ),
                ),
            )
            .execute()
    }

    suspend fun setImapPassword(password: String): ApolloResponse<SetImapConfigMutation.Data> {
        return graphqlClient.apolloClient
            .mutation(
                SetImapConfigMutation(
                    UpdateUserImapConfigInput(
                        password = Optional.present(password),
                    ),
                ),
            )
            .execute()
    }
}
