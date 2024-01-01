package net.matsudamper.money.backend.graphql

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.DataLoaderDefine
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterConditionDataLoaderDefine
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterConditionsDataLoaderDefine
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFiltersDataLoaderDefine
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderDefine
import net.matsudamper.money.backend.dataloader.MoneyUsageAnalyticsBySubCategoryLoader
import net.matsudamper.money.backend.dataloader.MoneyUsageAssociateByImportedMailDataLoaderDefine
import net.matsudamper.money.backend.dataloader.MoneyUsageCategoryDataLoaderDefine
import net.matsudamper.money.backend.dataloader.MoneyUsageDataLoaderDefine
import net.matsudamper.money.backend.dataloader.MoneyUsageSubCategoryDataLoaderDefine
import net.matsudamper.money.backend.dataloader.UserNameDataLoaderDefine
import net.matsudamper.money.backend.di.RepositoryFactory
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry

internal class DataLoaders(
    val repositoryFactory: RepositoryFactory,
    private val dataLoaderRegistryBuilder: DataLoaderRegistry.Builder = DataLoaderRegistry.Builder(),
    private val userSessionManager: UserSessionManagerImpl,
) {

    val importedMailDataLoader by register {
        ImportedMailDataLoaderDefine(repositoryFactory)
    }

    val moneyUsageSubCategoryDataLoader by register {
        MoneyUsageSubCategoryDataLoaderDefine(repositoryFactory)
    }

    val moneyUsageCategoryDataLoaderDefine by register {
        MoneyUsageCategoryDataLoaderDefine(repositoryFactory)
    }

    val moneyUsageDataLoader by register {
        MoneyUsageDataLoaderDefine(repositoryFactory)
    }

    val moneyUsageAssociateByImportedMailDataLoader by register {
        MoneyUsageAssociateByImportedMailDataLoaderDefine(repositoryFactory)
    }

    val importedMailCategoryFilterDataLoader by register {
        ImportedMailCategoryFilterDataLoaderDefine(repositoryFactory)
    }

    val importedMailCategoryFiltersDataLoader by register {
        ImportedMailCategoryFiltersDataLoaderDefine(repositoryFactory)
    }

    val importedMailCategoryFilterConditionDataLoader by register {
        ImportedMailCategoryFilterConditionDataLoaderDefine(repositoryFactory)
    }
    val importedMailCategoryFilterConditionsDataLoader by register {
        ImportedMailCategoryFilterConditionsDataLoaderDefine(repositoryFactory)
    }

    val moneyUsageAnalyticsBySubCategoryLoader by register {
        MoneyUsageAnalyticsBySubCategoryLoader(repositoryFactory, userSessionManager)
    }

    val userNameDataLoader by register {
        UserNameDataLoaderDefine(repositoryFactory.createUserNameRepository())
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
