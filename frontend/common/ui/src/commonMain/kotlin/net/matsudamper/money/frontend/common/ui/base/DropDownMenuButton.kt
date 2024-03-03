package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DropDownMenuButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        contentPadding =
            PaddingValues(
                start = 24.dp,
                end = 18.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        onClick = onClick,
    ) {
        content()
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
}
