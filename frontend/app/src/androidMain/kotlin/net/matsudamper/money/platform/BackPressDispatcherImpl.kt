package net.matsudamper.money.platform

import androidx.activity.ComponentActivity
import net.matsudamper.money.frontend.common.base.platform.BackPressDispatcher

internal class BackPressDispatcherImpl(
    private val componentActivity: ComponentActivity,
) : BackPressDispatcher {
    override fun onBackPressed() {
        componentActivity.onBackPressedDispatcher.onBackPressed()
    }
}
