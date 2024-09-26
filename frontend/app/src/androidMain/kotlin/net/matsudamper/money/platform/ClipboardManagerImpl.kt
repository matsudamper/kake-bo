package net.matsudamper.money.platform

import android.content.ClipData
import androidx.activity.ComponentActivity
import net.matsudamper.money.ui.root.platform.ClipboardManager

internal class ClipboardManagerImpl(
    private val componentActivity: ComponentActivity,
) : ClipboardManager {
    override fun copy(text: String) {
        val clipboardManager = componentActivity.getSystemService(android.content.ClipboardManager::class.java)
        val clip = ClipData.newPlainText("url", text)
        clipboardManager.setPrimaryClip(clip)
    }
}
