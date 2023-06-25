package net.matsudamper.money.frontend.common.base


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public interface AdminScreenController {
    public fun navigateToLogin()
    public fun navigateToRoot()
    public fun navigateToAddUser()
    public fun popBackStack()
}

@Composable
public fun rememberAdminScreenController(): AdminScreenController {
    return remember {
        AdminScreenControllerImpl()
    }
}

public enum class AdminScreenType {
    Login,
    Root,
    AddUser,
}

public class AdminScreenControllerImpl : AdminScreenController {
    private val _screen: MutableStateFlow<List<AdminScreenType>> = MutableStateFlow(listOf(AdminScreenType.Login))
    public val screen: StateFlow<List<AdminScreenType>> = _screen.asStateFlow()

    override fun navigateToLogin() {
        _screen.update {
            listOf(AdminScreenType.Login)
        }
    }

    override fun navigateToRoot() {
        _screen.update {
            it.plus(AdminScreenType.Root)
        }
    }

    override fun navigateToAddUser() {
        _screen.update {
            it.plus(AdminScreenType.AddUser)
        }
    }

    override fun popBackStack() {
        _screen.update {
            it.dropLast(1)
        }
    }
}
