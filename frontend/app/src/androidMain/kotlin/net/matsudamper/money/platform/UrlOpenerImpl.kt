package net.matsudamper.money.platform

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import net.matsudamper.money.ui.root.platform.UrlOpener

internal class UrlOpenerImpl(
    private val context: Context,
) : UrlOpener {
    override fun open(url: String) {
        // TODO use custom tab
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
