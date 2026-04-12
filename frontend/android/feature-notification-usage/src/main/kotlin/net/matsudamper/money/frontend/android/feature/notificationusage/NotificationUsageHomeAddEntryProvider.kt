package net.matsudamper.money.frontend.android.feature.notificationusage

import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.viewmodel.root.add.HomeAddExtensionEntryProvider

internal class NotificationUsageHomeAddEntryProvider : HomeAddExtensionEntryProvider {
    override fun createEntry(): HomeAddExtensionEntryProvider.Entry {
        return HomeAddExtensionEntryProvider.Entry(
            title = "通知から追加",
            icon = HomeAddExtensionEntryProvider.Entry.Icon.Notification,
            order = 25,
            screenStructure = ScreenStructure.Root.Add.NotificationUsage,
        )
    }
}
