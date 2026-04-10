package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.Room
import net.matsudamper.money.frontend.common.base.AppSettingsRepository
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.viewmodel.root.add.HomeAddExtensionEntryProvider
import net.matsudamper.money.frontend.graphql.GraphqlClient
import org.koin.dsl.module

public object NotificationUsageModule {
    public val module = module {
        single<NotificationUsageDatabase> {
            Room.databaseBuilder(
                get(),
                NotificationUsageDatabase::class.java,
                "notification_usage.db",
            )
                .addMigrations(NotificationUsageDatabase.Migration1To2)
                .build()
        }
        single<NotificationUsageDao> {
            get<NotificationUsageDatabase>().notificationUsageDao()
        }
        single<NotificationUsageParser> { ComExampleNotificationUsageParser() }
        single<NotificationUsageAutoAddApi> {
            NotificationUsageAutoAddGraphqlApi(
                graphqlClient = get<GraphqlClient>(),
            )
        }
        single {
            NotificationUsageAutoAddProcessor(
                dao = get(),
                parsers = getAll(),
                appSettingsRepository = get<AppSettingsRepository>(),
                api = get(),
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
