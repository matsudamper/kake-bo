package net.matsudamper.money.frontend.common.di

import android.content.Context
import org.koin.dsl.module

object AndroidModule {
    fun getModule(context: Context) = module {
        factory<Context> { context }
    }
}
