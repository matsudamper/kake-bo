package net.matsudamper.money.frontend.common.viewmodel

import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener

internal open class RootScreenScaffoldListenerDefaultImpl(
    private val navController: ScreenNavController,
) : RootScreenScaffoldListener {
    override val kakeboScaffoldListener: KakeboScaffoldListener = object : KakeboScaffoldListener {
        override fun onClickTitle() {
            navController.navigateToHome()
        }
    }

    override fun onClickHome() {
        navController.navigate(RootHomeScreenStructure.Home)
    }

    override fun onClickList() {
        navController.navigate(ScreenStructure.Root.Usage.Calendar())
    }

    override fun onClickSettings() {
        navController.navigate(ScreenStructure.Root.Settings.Root)
    }

    override fun onClickAdd() {
        navController.navigate(ScreenStructure.Root.Add.Root)
    }
}
