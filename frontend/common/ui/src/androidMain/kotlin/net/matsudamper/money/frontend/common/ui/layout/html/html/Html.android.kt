package net.matsudamper.money.frontend.common.ui.layout.html.html

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
public actual fun Html(html: String, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WebView(context).also {
                it.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                it.settings.supportZoom()
            }
        },
        update = {
            it.loadDataWithBaseURL(null, html, "text/html", Charsets.UTF_8.toString(), null)
        },
    )
}
