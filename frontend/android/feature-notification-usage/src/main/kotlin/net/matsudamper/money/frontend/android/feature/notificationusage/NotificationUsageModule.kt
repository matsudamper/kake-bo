package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.Room
import net.matsudamper.money.frontend.common.base.AppSettingsRepository
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.viewmodel.root.add.HomeAddExtensionEntryProvider
import net.matsudamper.money.frontend.graphql.GraphqlClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

public object NotificationUsageModule {
    public val module = module {
        single<NotificationUsageDatabase> {
            Room.databaseBuilder(
                get(),
                NotificationUsageDatabase::class.java,
                "notification_usage.db",
            )
                .addMigrations(
                    NotificationUsageDatabase.Migration1To2,
                    NotificationUsageDatabase.Migration2To3,
                    NotificationUsageDatabase.Migration3To4,
                )
                .build()
        }
        single<NotificationUsageDao> {
            get<NotificationUsageDatabase>().notificationUsageDao()
        }
        single<NotificationUsageParser>(named("com.felicanetworks.mfm.main")) { MobileSuicaNotificationUsageParser() }
        single<NotificationUsageParser>(named("jp.co.saisoncard.android.saisonportal")) { SaisonCardNotificationUsageParser() }
        single<NotificationUsageParser>(named("com.google.android.apps.walletnfcrel")) { GoogleWalletNotificationUsageParser() }
        single<NotificationUsageParser>(named("com.samsung.android.spay")) { SamsungPayNotificationUsageParser() }
        single<NotificationUsageAutoAddApi> {
            NotificationUsageAutoAddGraphqlApi(
                graphqlClient = get<GraphqlClient>(),
            )
        }
        single<NotificationUsageCategoryFilterRepository> {
            NotificationUsageCategoryFilterGraphqlRepository(
                graphqlClient = get<GraphqlClient>(),
            )
        }
        single {
            NotificationUsageAutoAddProcessor(
                dao = get(),
                parsers = getAll(),
                appSettingsRepository = get<AppSettingsRepository>(),
                api = get(),
                categoryFilterRepository = get(),
            )
        }
        single<HomeAddExtensionEntryProvider> { NotificationUsageHomeAddEntryProvider() }
        single<NotificationUsageRepository> {
            NotificationUsageRepositoryAndroidImpl(
                dao = get(),
                parsers = getAll(),
            )
        }
        single<NotificationUsageAccessGateway> {
            NotificationUsageAccessGatewayAndroidImpl(context = get())
        }
    }
}
