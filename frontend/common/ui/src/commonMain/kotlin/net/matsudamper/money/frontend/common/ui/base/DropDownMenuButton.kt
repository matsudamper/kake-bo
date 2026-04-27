package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_drop_down
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun DropDownMenuButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 18.dp,
            top = 8.dp,
            bottom = 8.dp,
        ),
        onClick = onClick,
    ) {
        content()
        Icon(painter = painterResource(Res.drawable.ic_arrow_drop_down), contentDescription = null)
    }
}
