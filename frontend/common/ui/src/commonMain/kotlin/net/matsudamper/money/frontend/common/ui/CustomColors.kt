package net.matsudamper.money.frontend.common.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
public class CustomColors(
    public val surfaceColor: Color,
    public val menuDividerColor: Color,
) {
    public companion object {
        public val Dark: CustomColors = CustomColors(
            surfaceColor = Color(0xff444654),
            menuDividerColor = Color.Gray,
        )

        public val Light: CustomColors = CustomColors(
            surfaceColor = Color.White,
            menuDividerColor = Color(0xFFDDDDDD),
        )
    }
}

public val LocalCustomColors: ProvidableCompositionLocal<CustomColors> =
    staticCompositionLocalOf { CustomColors.Dark }
