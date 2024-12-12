package net.matsudamper.money.frontend.common.viewmodel

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object PlatformTypeProvider {
    val type: PlatformType
}

internal enum class PlatformType {
    ANDROID,
    JS,
}
