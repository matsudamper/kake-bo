package net.matsudamper.money.frontend.graphql

import net.matsudamper.money.frontend.graphql.type.MailId as ApolloMailId
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import net.matsudamper.money.element.MailId

object GraphqlClient {
    private val cacheFactory = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)
    val apolloClient: ApolloClient = ApolloClient.Builder()
        .serverUrl("${serverProtocol}//${serverHost}/query")
        .normalizedCache(cacheFactory)
        .addCustomScalarAdapter(
            ApolloMailId.type,
            object : Adapter<MailId?> {
                override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): MailId? {
                    return reader.nextString()?.let { MailId(it) }
                }

                override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: MailId?) {
                    val id = value?.id
                    if (id != null) {
                        writer.value(id)
                    } else {
                        writer.nullValue()
                    }
                }
            },
        )
        .build()
}
