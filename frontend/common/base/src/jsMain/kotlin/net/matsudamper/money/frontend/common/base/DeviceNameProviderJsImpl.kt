package net.matsudamper.money.frontend.common.base

import kotlinx.browser.window

public class DeviceNameProviderJsImpl : DeviceNameProvider {
    override fun getDeviceName(): String {
        val ua = window.navigator.userAgent
        val browser = when {
            ua.contains("Edg/") -> "Edge"
            ua.contains("Chrome/") -> "Chrome"
            ua.contains("Firefox/") -> "Firefox"
            ua.contains("Safari/") && !ua.contains("Chrome") -> "Safari"
            else -> "Browser"
        }
        val os = when {
            ua.contains("Android") -> "Android"
            ua.contains("iPhone") || ua.contains("iPad") -> "iOS"
            ua.contains("Windows") -> "Windows"
            ua.contains("Mac OS X") -> "Mac"
            ua.contains("Linux") -> "Linux"
            else -> ""
        }
        return if (os.isNotEmpty()) "$browser on $os" else browser
    }
}
