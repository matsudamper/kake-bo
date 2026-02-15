package net.matsudamper.money.frontend.common.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
public class CustomColors(
    public val backgroundColor: Color,
    public val surfaceColor: Color,
    public val menuDividerColor: Color,
) {
    public companion object {
        public val Dark: CustomColors = CustomColors(
            backgroundColor = Color(0xff343541),
            surfaceColor = Color(0xff444654),
            menuDividerColor = Color.Gray,
        )

        public val Light: CustomColors = CustomColors(
            backgroundColor = Color(0xFFF5F5F5),
            surfaceColor = Color.White,
            menuDividerColor = Color(0xFFDDDDDD),
        )
    }
}

public val LocalCustomColors: ProvidableCompositionLocal<CustomColors> =
    staticCompositionLocalOf { CustomColors.Dark }
