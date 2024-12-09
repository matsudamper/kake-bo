package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import kotlinx.browser.window
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectStore

@Stable
internal class ScreenNavControllerImpl(
    private val initial: IScreenStructure,
    private val currentScreenStructureProvider: () -> IScreenStructure,
) : ScreenNavController {
    private val scopedObjectStoreProvider = mutableMapOf<IScreenStructure, ScopedObjectStore>()

    private var backStackEntries: List<InternalNavStackEntry> by mutableStateOf(
        listOf(
            InternalNavStackEntry(
                structure = initial,
                isHome = true,
            ),
        ),
    )
    override val currentBackstackEntry: ScreenNavController.NavStackEntry
        get() {
            val item = backStackEntries.last()
            return ScreenNavController.NavStackEntry(
                structure = item.structure,
                isHome = item.isHome,
                scopedObjectStore = scopedObjectStoreProvider.getOrPut(item.structure) {
                    ScopedObjectStore()
                },
            )
        }
    override val canGoBack: Boolean = true

    init {
        updateScreenState(
            currentScreenStructureProvider(),
        )

        window.addEventListener(
            "popstate",
            callback = {
                updateScreenState(
                    currentScreenStructureProvider(),
                )
            },
        )
    }

    override fun navigate(navigation: IScreenStructure) {
        val url = navigation.createUrl()
        if (backStackEntries.last().structure.equalScreen(navigation)) {
            window.history.replaceState(
                data = null,
                title = navigation.direction.title,
                url = url,
            )
        } else {
            window.history.pushState(
                data = null,
                title = navigation.direction.title,
                url = url,
            )
        }
        updateScreenState(navigation)
    }

    override fun navigateToHome() {
        while (backStackEntries.isNotEmpty()) {
            if (backStackEntries.last().isHome) {
                break
            }
            back()
        }
        navigate(
            backStackEntries.lastOrNull()?.structure ?: initial,
        )
    }

    override fun back() {
        // TODO: Storeを消すか検討する
        window.history.back()
    }

    private fun updateScreenState(screenStructure: IScreenStructure) {
        backStackEntries = backStackEntries.toMutableStateList().also {
            val last = it.removeLast()
            it.add(
                last.copy(
                    structure = screenStructure,
                    isHome = when (screenStructure) {
                        is ScreenStructure.Root -> true
                        else -> false
                    },
                ),
            )
        }
    }

    private data class InternalNavStackEntry(
        val structure: IScreenStructure,
        val isHome: Boolean,
    )
}
