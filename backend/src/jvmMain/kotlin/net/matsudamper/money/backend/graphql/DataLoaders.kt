package net.matsudamper.money.backend.graphql

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.DataLoaderDefine
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderDefine
import net.matsudamper.money.backend.di.RepositoryFactory
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry

class DataLoaders(
    val repositoryFactory: RepositoryFactory,
) {
    val dataLoaderRegistryBuilder = DataLoaderRegistry.Builder()

    val importedMailDataLoader by register {
        ImportedMailDataLoaderDefine(repositoryFactory)
    }

    private fun <K, V> register(
        initializer: () -> DataLoaderDefine<K, V>,
    ): DataLoaderRegister<K, V> {
        val provider = initializer()

        dataLoaderRegistryBuilder.register(
            provider.key,
            provider.getDataLoader(),
        )
        return DataLoaderRegister(provider.key)
    }

    class DataLoaderProvider<K, V>(
        private val dataLoaderName: String,
    ) {
        fun get(env: DataFetchingEnvironment): DataLoader<K, V> {
            return env.dataLoaderRegistry.getDataLoader(dataLoaderName)
        }
    }

    private class DataLoaderRegister<K, V>(
        private val key: String,
    ) : ReadOnlyProperty<Any, DataLoaderProvider<K, V>> {
        override fun getValue(thisRef: Any, property: KProperty<*>): DataLoaderProvider<K, V> {
            return DataLoaderProvider(key)
        }
    }
}