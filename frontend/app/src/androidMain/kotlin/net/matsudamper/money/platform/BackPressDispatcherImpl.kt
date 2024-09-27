package net.matsudamper.money.platform

import androidx.activity.ComponentActivity
import net.matsudamper.money.ui.root.platform.BackPressDispatcher

internal class BackPressDispatcherImpl(
    private val componentActivity: ComponentActivity,
) : BackPressDispatcher {
    override fun onBackPressed() {
        componentActivity.onBackPressedDispatcher.onBackPressed()
    }
}
