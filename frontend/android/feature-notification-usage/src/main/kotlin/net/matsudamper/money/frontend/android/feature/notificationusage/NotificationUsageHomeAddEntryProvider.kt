package net.matsudamper.money.frontend.android.feature.notificationusage

import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.viewmodel.root.add.HomeAddExtensionEntry
import net.matsudamper.money.frontend.common.viewmodel.root.add.HomeAddExtensionEntryProvider

internal class NotificationUsageHomeAddEntryProvider : HomeAddExtensionEntryProvider {
    override fun createEntry(): HomeAddExtensionEntry {
        return HomeAddExtensionEntry(
            title = "通知から追加",
            icon = HomeAddExtensionEntry.Icon.Notification,
            order = 25,
            screenStructure = ScreenStructure.Root.Add.NotificationUsage,
        )
    }
}
