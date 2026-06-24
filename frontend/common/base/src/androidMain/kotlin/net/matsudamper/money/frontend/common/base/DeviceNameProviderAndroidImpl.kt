package net.matsudamper.money.frontend.common.base

import android.os.Build

public class DeviceNameProviderAndroidImpl : DeviceNameProvider {
    override fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
}
