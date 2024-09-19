package net.matsudamper.money.frontend.common.feature.webauth

import android.content.Context
import org.koin.dsl.module

object AndroidModule {
    fun getModule(context: Context) = module {
        factory<Context> { context }
    }
}