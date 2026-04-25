package net.matsudamper.money.frontend.common.base.nav.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public interface AdminScreenController {
    public fun navigateToLogin()

    public fun navigateToRoot()

    public fun navigateToAddUser()

    public fun navigateToUnlinkedImages()

    public fun popBackStack()
}

@Composable
public fun rememberAdminScreenController(): AdminScreenController {
    return remember {
        AdminScreenControllerImpl()
    }
}

public enum class AdminScreenType : NavKey {
    Login,
    Root,
    AddUser,
    UnlinkedImages,
}

public class AdminScreenControllerImpl : AdminScreenController {
    private val _screen: MutableStateFlow<List<AdminScreenType>> = MutableStateFlow(emptyList())
    public val screen: StateFlow<List<AdminScreenType>> = _screen.asStateFlow()

    override fun navigateToLogin() {
        _screen.update {
            listOf(AdminScreenType.Login)
        }
    }

    override fun navigateToRoot() {
        _screen.update {
            listOf(AdminScreenType.Root)
        }
    }

    override fun navigateToAddUser() {
        _screen.update { current ->
            when (current.lastOrNull()) {
                AdminScreenType.AddUser -> current
                AdminScreenType.Root -> current + AdminScreenType.AddUser
                else -> listOf(AdminScreenType.Root, AdminScreenType.AddUser)
            }
        }
    }

    override fun navigateToUnlinkedImages() {
        _screen.update { current ->
            when (current.lastOrNull()) {
                AdminScreenType.UnlinkedImages -> current
                AdminScreenType.Root -> current + AdminScreenType.UnlinkedImages
                else -> listOf(AdminScreenType.Root, AdminScreenType.UnlinkedImages)
            }
        }
    }

    override fun popBackStack() {
        _screen.update { current ->
            if (current.size <= 1) {
                current
            } else {
                current.dropLast(1)
            }
        }
    }
}
