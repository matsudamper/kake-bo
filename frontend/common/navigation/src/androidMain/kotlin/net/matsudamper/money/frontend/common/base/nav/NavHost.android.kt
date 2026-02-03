package net.matsudamper.money.frontend.common.base.nav

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public actual fun NavHost(
    navController: ScreenNavController,
    entryProvider: (IScreenStructure) -> NavEntry<IScreenStructure>,
) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current
    val backStack by rememberUpdatedState(navController.backstackEntries)

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider,
        onBack = {
            if (navController.canGoBack) {
                navController.back()
            } else {
                dispatcher?.onBackPressedDispatcher?.onBackPressed()
            }
        },
        predictivePopTransitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(
                    animationSpec = spring(
                        dampingRatio = 1.0f,
                        stiffness = 1600.0f,
                    ),
                ),
                initialContentExit = fadeOut(
                    animationSpec = spring(
                        dampingRatio = 1.0f,
                        stiffness = 1600.0f,
                    ),
                ),
            )
        },
    )
}

@Composable
public actual fun rememberScopedObjectStoreOwner(key: String): ScopedObjectStoreOwner {
    val owner = viewModel(
        key = key,
        initializer = {
            InMemoryScopedObjectStoreOwnerImplViewModel()
        },
    )
    return owner
}

private class InMemoryScopedObjectStoreOwnerImplViewModel :
    ViewModel(),
    ScopedObjectStoreOwner by InMemoryScopedObjectStoreOwnerImpl()
