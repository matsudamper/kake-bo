package net.matsudamper.money.frontend.common.viewmodel

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import net.matsudamper.money.frontend.common.usecase.LoginCheckUseCase

public val LocalGlobalEventHandlerLoginCheckUseCaseDelegate: ProvidableCompositionLocal<GlobalEventHandlerLoginCheckUseCaseDelegate> = compositionLocalOf<GlobalEventHandlerLoginCheckUseCaseDelegate> {
    error("No Provided")
}

public class GlobalEventHandlerLoginCheckUseCaseDelegate(
    useCase: LoginCheckUseCase,
) : LoginCheckUseCase by useCase
