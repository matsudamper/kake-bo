package net.matsudamper.money.backend.graphql

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import net.matsudamper.money.backend.dataloader.DataLoaderProvider
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderProvider
import net.matsudamper.money.backend.di.RepositoryFactory
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry

class DataLoaders(
    val repositoryFactory: RepositoryFactory,
) {
    val dataLoaderRegistryBuilder = DataLoaderRegistry.Builder()

    val importedMailDataLoader by register {
        ImportedMailDataLoaderProvider(repositoryFactory)
    }

    private fun <K, V> register(initializer: () -> DataLoaderProvider<K, V>): DataLoaderRegister<K, V> {
        val provider = initializer()

        dataLoaderRegistryBuilder.register(
            provider.displayName,
            provider.getDataLoader(),
        )
        return DataLoaderRegister(provider)
    }

    private class DataLoaderRegister<K, V>(
        private val dataLoaderProvider: DataLoaderProvider<K, V>,
    ) : ReadOnlyProperty<Any, DataLoader<K, V>> {
        override fun getValue(thisRef: Any, property: KProperty<*>): DataLoader<K, V> {
            return dataLoaderProvider.getDataLoader()
        }
    }
}