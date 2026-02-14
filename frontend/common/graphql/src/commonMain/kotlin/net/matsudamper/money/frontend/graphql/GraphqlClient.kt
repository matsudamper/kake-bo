package net.matsudamper.money.frontend.graphql

import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.let
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpInterceptor
import net.matsudamper.money.element.ApiTokenId
import net.matsudamper.money.element.FidoId
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.type.ApiTokenId as ApolloApiTokenId
import net.matsudamper.money.frontend.graphql.type.FidoId as ApolloFidoId
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterConditionId as ApolloImportedMailCategoryFilterConditionId
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterId as ApolloImportedMailCategoryFilterId
import net.matsudamper.money.frontend.graphql.type.ImportedMailId as ApolloImportedMailId
import net.matsudamper.money.frontend.graphql.type.ImageId as ApolloImageId
import net.matsudamper.money.frontend.graphql.type.Long as ApolloLong
import net.matsudamper.money.frontend.graphql.type.MailId as ApolloMailId
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoryId as ApolloMoneyUsageCategoryId
import net.matsudamper.money.frontend.graphql.type.MoneyUsageId as ApolloMoneyUsageId
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryId as ApolloMoneyUsageSubCategoryId

public interface GraphqlClient {
    val apolloClient: ApolloClient

    fun updateServerUrl(serverUrl: String)
}

class GraphqlClientImpl(
    private val interceptors: List<ApolloInterceptor>,
    private val httpInterceptors: List<HttpInterceptor> = emptyList(),
    serverUrl: String,
    private val onServerUrlChanged: (String) -> Unit,
) : GraphqlClient {
    private val cacheFactory = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)

    override var apolloClient: ApolloClient = buildClient(serverUrl)
        private set

    override fun updateServerUrl(serverUrl: String) {
        apolloClient = buildClient(serverUrl)
        onServerUrlChanged(serverUrl)
    }

    private fun buildClient(serverUrl: String): ApolloClient = ApolloClient.Builder()
        .serverUrl(serverUrl)
        .httpEngine(DefaultHttpEngine(timeoutMillis = 5000))
        .httpInterceptors(httpInterceptors)
        .interceptors(interceptors)
        .normalizedCache(cacheFactory)
        .addCustomScalarAdapter(
            ApolloLong.type,
            CustomLongAdapter(
                serialize = { it },
                deserialize = { it },
            ),
        )
        .addCustomScalarAdapter(
            ApolloMailId.type,
            CustomStringAdapter(
                serialize = {
                    it.id
                },
                deserialize = {
                    MailId(it)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloMailId.type,
            CustomStringAdapter(
                serialize = {
                    it.id
                },
                deserialize = {
                    MailId(it)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloFidoId.type,
            CustomIntAdapter(
                serialize = {
                    it.value
                },
                deserialize = {
                    FidoId(it)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloApiTokenId.type,
            CustomStringAdapter(
                serialize = {
                    it.value
                },
                deserialize = {
                    ApiTokenId(it)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloImportedMailId.type,
            CustomIntAdapter(
                serialize = {
                    it.id
                },
                deserialize = { value ->
                    ImportedMailId(value)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloImportedMailCategoryFilterConditionId.type,
            CustomIntAdapter(
                serialize = {
                    it.id
                },
                deserialize = { value ->
                    ImportedMailCategoryFilterConditionId(value)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloImportedMailCategoryFilterId.type,
            CustomIntAdapter(
                serialize = {
                    it.id
                },
                deserialize = { value ->
                    ImportedMailCategoryFilterId(value)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloMoneyUsageId.type,
            CustomIntAdapter(
                serialize = {
                    it.id
                },
                deserialize = { value ->
                    MoneyUsageId(value)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloMoneyUsageSubCategoryId.type,
            CustomIntAdapter(
                serialize = {
                    it.id
                },
                deserialize = { value ->
                    MoneyUsageSubCategoryId(value)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloMoneyUsageCategoryId.type,
            CustomIntAdapter(
                serialize = {
                    it.value
                },
                deserialize = { value ->
                    MoneyUsageCategoryId(value)
                },
            ),
        )
        .addCustomScalarAdapter(
            ApolloImageId.type,
            CustomStringAdapter(
                serialize = {
                    it.value
                },
                deserialize = { value ->
                    ImageId(value)
                },
            ),
        )
        .build()
}

private class CustomStringAdapter<T>(
    val deserialize: (String) -> T?,
    val serialize: (T) -> String,
) : Adapter<T?> {
    override fun fromJson(
        reader: JsonReader,
        customScalarAdapters: CustomScalarAdapters,
    ): T? {
        return reader.nextString()?.let { deserialize(it) }
    }

    override fun toJson(
        writer: JsonWriter,
        customScalarAdapters: CustomScalarAdapters,
        value: T?,
    ) {
        if (value != null) {
            writer.value(serialize(value))
        } else {
            writer.nullValue()
        }
    }
}

private class CustomIntAdapter<T>(
    val deserialize: (Int) -> T?,
    val serialize: (T) -> Int,
) : Adapter<T?> {
    override fun fromJson(
        reader: JsonReader,
        customScalarAdapters: CustomScalarAdapters,
    ): T? {
        return deserialize(reader.nextInt())
    }

    override fun toJson(
        writer: JsonWriter,
        customScalarAdapters: CustomScalarAdapters,
        value: T?,
    ) {
        if (value != null) {
            writer.value(serialize(value))
        } else {
            writer.nullValue()
        }
    }
}

private class CustomLongAdapter<T>(
    val deserialize: (Long) -> T?,
    val serialize: (T) -> Long,
) : Adapter<T?> {
    override fun fromJson(
        reader: JsonReader,
        customScalarAdapters: CustomScalarAdapters,
    ): T? {
        return deserialize(reader.nextLong())
    }

    override fun toJson(
        writer: JsonWriter,
        customScalarAdapters: CustomScalarAdapters,
        value: T?,
    ) {
        if (value != null) {
            writer.value(serialize(value))
        } else {
            writer.nullValue()
        }
    }
}
