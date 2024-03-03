package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import net.matsudamper.money.element.FidoId
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.type.FidoId as ApolloFidoId
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterConditionId as ApolloImportedMailCategoryFilterConditionId
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterId as ApolloImportedMailCategoryFilterId
import net.matsudamper.money.frontend.graphql.type.ImportedMailId as ApolloImportedMailId
import net.matsudamper.money.frontend.graphql.type.Long as ApolloLong
import net.matsudamper.money.frontend.graphql.type.MailId as ApolloMailId
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoryId as ApolloMoneyUsageCategoryId
import net.matsudamper.money.frontend.graphql.type.MoneyUsageId as ApolloMoneyUsageId
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryId as ApolloMoneyUsageSubCategoryId

object GraphqlClient {
    private val cacheFactory = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)
    val apolloClient: ApolloClient =
        ApolloClient.Builder()
            .serverUrl("$serverProtocol//$serverHost/query")
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
