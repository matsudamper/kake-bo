package net.matsudamper.money.platform

import androidx.activity.ComponentActivity
import net.matsudamper.money.ui.root.platform.ApplicationNotificationManager

class ApplicationNotificationManagerImpl(
    private val componentActivity: ComponentActivity,
) : ApplicationNotificationManager {
    override fun notify(message: String) {
        componentActivity.runOnUiThread {
            android.widget.Toast.makeText(componentActivity, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
