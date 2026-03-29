package net.matsudamper.money.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import net.matsudamper.money.frontend.common.base.platform.ApplicationNotificationManager

class ApplicationNotificationManagerImpl(
    private val componentActivity: ComponentActivity,
    private val onRequestNotificationPermission: () -> Unit,
) : ApplicationNotificationManager {
    override fun notify(message: String) {
        componentActivity.runOnUiThread {
            android.widget.Toast.makeText(componentActivity, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(componentActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                onRequestNotificationPermission()
            }
        }
    }
}
