package net.matsudamper.money.frontend.common.viewmodel.root.add

import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure

public fun interface HomeAddExtensionEntryProvider {
    public fun createEntry(): HomeAddExtensionEntry
}

public data class HomeAddExtensionEntry(
    val title: String,
    val icon: Icon,
    val order: Int,
    val screenStructure: ScreenStructure.Root.Add,
) {
    public enum class Icon {
        Notification,
    }
}
