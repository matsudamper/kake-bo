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
import net.matsudamper.money.backend.di.DiContainer
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry

internal class DataLoaders(
    private val diContainer: DiContainer,
    private val dataLoaderRegistryBuilder: DataLoaderRegistry.Builder = DataLoaderRegistry.Builder(),
    private val userSessionManager: UserSessionManagerImpl,
) {

    val importedMailDataLoader by register {
        ImportedMailDataLoaderDefine(diContainer)
    }

    val moneyUsageSubCategoryDataLoader by register {
        MoneyUsageSubCategoryDataLoaderDefine(diContainer)
    }

    val moneyUsageCategoryDataLoaderDefine by register {
        MoneyUsageCategoryDataLoaderDefine(diContainer)
    }

    val moneyUsageDataLoader by register {
        MoneyUsageDataLoaderDefine(diContainer)
    }

    val moneyUsageAssociateByImportedMailDataLoader by register {
        MoneyUsageAssociateByImportedMailDataLoaderDefine(diContainer)
    }

    val importedMailCategoryFilterDataLoader by register {
        ImportedMailCategoryFilterDataLoaderDefine(diContainer)
    }

    val importedMailCategoryFiltersDataLoader by register {
        ImportedMailCategoryFiltersDataLoaderDefine(diContainer)
    }

    val importedMailCategoryFilterConditionDataLoader by register {
        ImportedMailCategoryFilterConditionDataLoaderDefine(diContainer)
    }
    val importedMailCategoryFilterConditionsDataLoader by register {
        ImportedMailCategoryFilterConditionsDataLoaderDefine(diContainer)
    }

    val moneyUsageAnalyticsBySubCategoryLoader by register {
        MoneyUsageAnalyticsBySubCategoryLoader(diContainer, userSessionManager)
    }

    val userNameDataLoader by register {
        UserNameDataLoaderDefine(diContainer.createUserNameRepository())
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
