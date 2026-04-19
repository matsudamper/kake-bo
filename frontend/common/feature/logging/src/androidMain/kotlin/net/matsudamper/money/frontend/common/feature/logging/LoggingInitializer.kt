package net.matsudamper.money.frontend.common.feature.logging

import timber.log.Timber

public fun initializeLogging() {
    Timber.plant(Timber.DebugTree())
}
