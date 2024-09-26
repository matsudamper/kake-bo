package net.matsudamper.money.frontend.common.viewmodel

import net.matsudamper.money.frontend.common.usecase.LoginCheckUseCase

public class GlobalEventHandlerLoginCheckUseCaseDelegate(
    useCase: LoginCheckUseCase,
) : LoginCheckUseCase by useCase
